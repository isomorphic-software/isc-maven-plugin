package com.isomorphic.maven.mojo.reifyOnSite;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.isomorphic.maven.mojo.AbstractPackagerMojo;
import com.isomorphic.maven.packaging.Module;
import com.isomorphic.maven.packaging.Product;
import com.isomorphic.maven.util.AntPathMatcherFilter;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.isomorphic.maven.packaging.Product.REIFY_ONSITE;

/**
 * Downloads and unpacks a Reify OnSite distribution and uses it to upgrade the existing
 * Reify OnSite deployment at {@link #targetDir} without affecting your settings or data.
 * Note that the current version of this goal only supports the SmartClient version of
 * Reify OnSite.  Isomorphic does also provide a version of Reify OnSite packaged as
 * a SmartGWT application, but that is outside the scope of this plugin.
 * <p>
 * Note that Reify OnSite is NOT a Maven project,  This goal upgrades the actual product in
 * the target directory you specify; it does not install files to any Maven cache or
 * repository.
 */
@Mojo(name="upgrade-reify-onsite", requiresProject=false, requiresDirectInvocation=true)
public final class UpgradeReifyOnSiteMojo extends AbstractReifyOnSiteGoalsMojo {

    private static final Logger log = LoggerFactory.getLogger(UpgradeReifyOnSiteMojo.class);

    /**
     * Path to an existing installation of Reify OnSite.  If the directory does not exist, or
     * does not contain an installation of Reify OnSite, this goal will fail.
     *
     * @since 1.5.0
     */
    @Parameter(property="targetDir", required=true)
    private String targetDir;


    /**
     * By default, this goal upgrades just the skins it finds installed in the existing
     * installation it is upgrading.  If you would like the upgrade to also install additional
     * skins from the downloaded package, name them in this parameter.  So, if the existing
     * installation contains skins Enterprise, Stratus and ToolSkin, and you set this parameter
     * like <code>-Dskins=Tahoe,Obsidian</code>, the Reify Online installation will have five upgraded
     * skins after the goal completes: Enterprise, Stratus, ToolSkin, Tahoe and Obsidian.
     *
     * @since 1.5.0
     */
    @Parameter(property = "skins")
    public void setSkins(String skins) {
        this.skins = skins;
    }


    private Pattern pattern = Pattern.compile("^(.*)-\\d+(?:\\.\\d+){1,2}(?:[-\\.][A-Za-z0-9]+)*\\.jar$");


    public void doExecute(File basedir) throws MojoExecutionException, MojoFailureException {
        sourceRoot = new File(basedir, "reifyOnSite");
        if (!sourceRoot.isDirectory()) {
            throw new MojoFailureException("The reifyOnSite directory does not exist in " + basedir.toString());
        }
        targetRoot = new File(targetDir);
        if (targetRoot.exists()) {
            if (targetRoot.isDirectory()) {
                boolean isReify = false;
                String[] contents = targetRoot.list();
                if (contents != null && contents.length > 0) {
                    boolean assembled = false, isomorphic = false, tools = false;
                    for (int i = 0; i < contents.length; i++) {
                        // No Reify installation can work without the assembled/, isomorphic/
                        // and tools/ directories, so check for the presence of those
                        File f = new File(targetRoot, contents[i]);
                        if (f.isDirectory() && "assembled".equals(contents[i])) {
                            assembled = true;
                        } else if (f.isDirectory() && "isomorphic".equals(contents[i])) {
                            isomorphic = true;
                        } else if (f.isDirectory() && "tools".equals(contents[i])) {
                            tools = true;
                        }
                    }
                    isReify = assembled && isomorphic && tools;
                }
                if (!isReify) {
                    throw new MojoExecutionException("targetDir " + targetDir + " is not an " +
                                "existing Reify OnSite installation");
                }

            } else  {
                throw new MojoExecutionException("targetDir " + targetDir + " is not a directory");
            }
        } else {
            throw new MojoExecutionException("targetDir " + targetDir + " does not exist");
        }

        log.info("Upgrading Reify OnSite at target directory " + targetRoot.getAbsolutePath() +
                    " to version " + buildNumber + ", buildDate " + buildDate);

        File embedded = new File(targetRoot, "WEB-INF/embeddedTomcat");
        boolean standalone = embedded.exists();

        AntPathMatcherFilter filter = new AntPathMatcherFilter();
        filter.include("**");

        // Exclude Java libs from blind copy - we need to deal with those carefully one-by-one,
        // in case he existing installation includes custom customer JARs that are not part of
        // Reify OnSite
        filter.exclude("WEB-INF/lib/**");

        // Exclude settings and database
        filter.exclude("WEB-INF/classes/**,WEB-INF/db/**");

        // Whether or not we are in standalone mode, exclude the bootstrap scripts from
        // copying - if we are not in standalone mode they are not needed, and if we are in
        // standalone mode, the customer may have changed them to listen on a custom port
        filter.exclude("start_embedded_server.*");

        // And embedded Tomcat if not standalone
        if (!standalone) {
            log.info("Not in standalone mode, excluding embedded Tomcat");
            filter.exclude("WEB-INF/bin/**,WEB-INF/embeddedTomcat/**");
        }

        fileCount = 0;
        copyRecursively(sourceRoot, filter);

        log.info("Copied " + fileCount + " files to " + targetRoot.getAbsolutePath());

        // Iterate over the JAR files and replace one by one, matching by base name only (ie,
        // recognise that commons-cli-1.4 should replace commons-cli-1.2.77, for example)

        File srcLibDir = new File(sourceRoot, "WEB-INF/lib");
        File tgtLibDir = new File(targetRoot, "WEB-INF/lib");
        File[] libs = srcLibDir.listFiles();
        if (libs == null) libs = new File[0];
        Arrays.sort(libs, (a, b) -> a.getName().toLowerCase().compareTo(b.getName().toLowerCase()));
        for (int i = 0; i < libs.length; i++) {
            File lib = libs[i];
            String strippedName = lib.getName().substring(0, lib.getName().lastIndexOf('.'));
            final String baseName = getBaseName(lib.getName());
            boolean versioned = !(baseName.equals(strippedName));
            //log.info("Chacking matches for downloaded file " + lib.getName() + "(base name is " + baseName + ", stripped name is " + strippedName + ")");
            File[] targetLibs = tgtLibDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return getBaseName(file.getName()).equals(baseName);
                }
            });

            try {
                String relPath = sourceRoot.toPath().relativize(lib.toPath()).toString()
                        .replace(File.separatorChar, '/');

                if (targetLibs == null || targetLibs.length == 0) {
                    // There is no matching file in the target directory - full steam ahead
                    copyFile(lib, new File(targetRoot, relPath));
                } else if (targetLibs.length == 1) {
                    // The most common mainstream case - exactly one existing file.  We will
                    // go ahead and replace it anyway, since that's what we do with, eg, JS
                    // files, but if the filename is in any way different, or the file was
                    // not versioaned, also log that we replaced it
                    if (lib.getName().equals(targetLibs[0].getName()) && versioned) {
                        copyFile(lib, new File(targetRoot, relPath));
                    } else if (!(lib.getName().equals(targetLibs[0].getName()))) {
                        log.info("Replacing existing versioned library " + targetLibs[0].getName() +
                                    " with " + lib.getName());
                        targetLibs[0].delete();
                        copyFile(lib, new File(targetRoot, relPath));
                    } else {
                        log.info("Replacing unversioned library " + targetLibs[0].getName());
                        copyFile(lib, new File(targetRoot, relPath));
                    }
                } else if (targetLibs.length > 1) {
                    // If there is more than one then we currently have an "overlapping" situation
                    // and presumably things are working OK regardless.  So in this case, copy the
                    // downloaded version over and log that this has happened
                    boolean willReplace = lib.getName().equals(targetLibs[0].getName());
                    String existing = targetLibs[0].getName();
                    for (int j = 1; j < targetLibs.length; j++) {
                        existing += ", " + targetLibs[j].getName();
                        willReplace = willReplace || lib.getName().equals(targetLibs[j].getName());
                    }
                    log.info("Found multiple libraries in the existing installation with the " +
                                "same base name: " + existing + ". Library " + lib.getName() +
                                " will be copied from the downloaded build" +
                                (willReplace ? ", replacing the existing file of that name." : ".") +
                                "  Other existing files with the same base name will be left " +
                                "undisturbed");
                    copyFile(lib, new File(targetRoot, relPath));
                }
            } catch(IOException ioe) {
                throw new MojoFailureException("Caught Exception trying to copy JARs", ioe);
            }
        }


        if (standalone) {
            // Unix-derived OS's require scripts to be marked executable
            String[] fileNames = {"WEB-INF/bin/embeddedTomcat.sh", "WEB-INF/bin/embeddedTomcat.command"};
            for (int i = 0; i < fileNames.length; i++) {
                File f = new File(targetRoot, fileNames[i]);
                // This call is a harmless no-op on Windows
                f.setExecutable(true, false);
            }
            log.info("Marked standalone scripts as executable");
        }
    }

    private String getBaseName(String fullName) {
        String baseName;
        Matcher m = pattern.matcher(fullName);
        if (m.matches()) {
            baseName = m.group(1);
        } else {
            baseName = fullName.substring(0, fullName.lastIndexOf('.'));
        }
        return baseName;
    }

    @Override
    protected List<String> getAdditionalSkins(String skinDirName) {

        // Let's add the standard skins, in case they have been deleted from the local
        // installation, thereby breaking it, and the Ugrade goal is being run in the hope
        // that it will fix things
        List<String> additional = super.getAdditionalSkins(skinDirName);

        File skinsDir = new File(targetDir, skinDirName);
        String[] skinNames = skinsDir.list();
        if (skinNames == null || skinNames.length == 0) {
            log.info("No skins found in " + skinDirName + ".  Skipping skin pruning, all skins will be installed/upgraded");
            return new ArrayList<>();
        }
        for (String name : skinNames) {
            File skinDir = new File(skinsDir, name);
            if (skinDir.isDirectory()) {
                additional.add(name.toLowerCase());
            }
        }

        return additional;
    }


}
