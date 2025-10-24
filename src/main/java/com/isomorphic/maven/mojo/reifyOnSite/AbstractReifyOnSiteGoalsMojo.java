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

import com.google.common.base.Splitter;
import com.isomorphic.maven.mojo.AbstractPackagerMojo;
import com.isomorphic.maven.packaging.License;
import com.isomorphic.maven.packaging.Module;
import com.isomorphic.maven.packaging.Product;
import com.isomorphic.maven.util.AntPathMatcherFilter;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is the shared Reify OnSite Mojo that the goals install-reify-onsite and
 * upgrade-reify-onsite inherit from.  It was introduced with those goals in order to provide
 * a common ancestor for documenting parameters, because many of the parameters that were
 * previously annotated and documented in AbstractBaseMojo are not applicable to these new
 * Reify OnSite goals, but we do want those goals to extend AbstractBaseMojo because they
 * make use of substantial shared facilities, like obtaining the latest release number,
 * downloading and unpacking.
 *
 * So we define the common annotations and docs for the Reify OnSite functions here, so they
 * are inherited by the Reify OnSite goals but not the existing "core" goals like "install"
 */
public class AbstractReifyOnSiteGoalsMojo extends AbstractPackagerMojo {

    protected Logger log = LoggerFactory.getLogger(AbstractReifyOnSiteGoalsMojo.class);


    /**
     * Technically the product should be "REIFY_ONSITE", but there is no need to specify it
     * (and indeed it will be ignored if you do).  REIFY_ONSITE is implied for the Reify
     * OnSite goals, so we force it to that setting
     *
     * @since 1.0.0
     */
    @Parameter(property = "product", defaultValue = "REIFY_ONSITE")
    public void setProduct(Product product) {
        this.product = product;
    }

    /**
     * Technically the license should be "ENTERPRISE", but there is no need to specify it
     * (and indeed it will be ignored if you do).  ENTERPRISE is implied for the Reify OnSite
     * goals, so we force it to that setting
     *
     * @since 1.0.0
     */
    @Parameter(property = "license", defaultValue = "ENTERPRISE")
    public void setLicense(License license) {
        this.license = license;
    }


    @Component
    protected MojoExecution mojoExecution;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (product != Product.REIFY_ONSITE) {
            log.info("For goal '" + mojoExecution.getGoal() + "', product MUST be REIFY_ONSITE.  Overriding and continuing");
            product = Product.REIFY_ONSITE;
        }
        if (license != License.ENTERPRISE) {
            log.info("For goal '" + mojoExecution.getGoal() + "', license MUST be ENTERPRISE.  Overriding and continuing");
            license = License.ENTERPRISE;
        }
        super.execute();
    }

    @Override
    public void doExecute(Set<Module> artifacts) throws MojoExecutionException, MojoFailureException {
        throw new MojoExecutionException("The doExecute() override that accepts a Set of Modules is not supported for " +
                "goal " + mojoExecution.getGoal() + "; this goal is about deploying " +
                "Reify OnSite, not installing Maven-managed assets");
    }

    @Override
    protected void skin(File basedir, Map<String, String> skinResources) throws MojoExecutionException, IOException {

        if (skins == null && mojoExecution.getGoal().equals("install-reify-onsite")) {
            // Install all skins if no "skins" property is provided
            return;
        }

        // Otherwise, we install the list of skins provided (plus some standard ones).  If we
        // are upgrading, we only upgrade the installed skins, plus any that the user specified
        // in the skins param
        log.info("Pruning unwanted skins...");

        // Reify OnSite goals do not need to extract or unJar anything, so the "skinResources"
        // is kinda redundant.  We just use it to store the path to the skins/ directory
        // against a known key

        List<String> requested;
        if (skins != null) {
            requested = new ArrayList<>(Splitter.on(",").trimResults().splitToList(skins.toLowerCase()));
        } else {
            requested = new ArrayList<>();
        }

        String subdir = "reifyOnSite";
        String skinDirName = subdir + "/" + skinResources.get("REIFY_ONSITE");
        File skinDir = new File(basedir, skinDirName);

        // The Install goal adds the same set of standard skins that a standard framework
        // goal adds; the Upgrade goal also adds any skins that are currently installed
        List<String> additionalSkins = getAdditionalSkins(skinResources.get("REIFY_ONSITE"));
        requested.addAll(additionalSkins);

        String[] skinNames = skinDir.list();
        if (skinNames != null && skinNames.length > 0) {
            for (String skinName : skinNames) {
                if (requested.contains(skinName.toLowerCase())) continue;
                File dirToRemove = new File(skinDir, skinName);
                log.info("Deleting '{}' skin resources at '{}'", skinName, dirToRemove.getAbsolutePath());
                if (FileSystemUtils.deleteRecursively(dirToRemove)) {
                    log.info("Deleting '{}' skin resources at '{}' completed successfully", skinName, dirToRemove.getAbsolutePath());
                } else {
                    log.info("Deleting '{}' skin resources at '{}' encountered problems, some skin resources may remain", skinName, dirToRemove.getAbsolutePath());
                }
            }
        } else {
            log.info("No 'skins' directory found in download, skipping");
        }
    }

    protected List<String> getAdditionalSkins(String skinDirName) {
        List<String> additional = new ArrayList<String>();
        // preserve for simplicity
        additional.add("enterprise");
        additional.add("fonts");
        additional.add("toolskin");
        additional.add("toolskinnative");

        return additional;
    }


    protected File sourceRoot, targetRoot;
    protected int fileCount;

    protected void copyRecursively(File file, AntPathMatcherFilter filter)
            throws MojoExecutionException, MojoFailureException
    {
        String relPath = sourceRoot.toPath().relativize(file.toPath()).toString()
                .replace(File.separatorChar, '/'); // normalize to forward slashes in case we're running on Windows

        if (file.isDirectory()) {
            // Skip excluded directories entirely
            if (!relPath.isEmpty() && !filter.accept(relPath + "/")) {
                return;
            }
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    copyRecursively(child, filter);
                }
            }
        } else {
            if (filter.accept(relPath)) {
                fileCount++;
                try {
                    copyFile(file, new File(targetRoot, relPath));
                } catch(IOException ioe) {
                    throw new MojoFailureException("Caught excpetion trying to copy files", ioe);
                }
            }
        }
    }

    protected void copyFile(File src, File dst) throws IOException {
        if (!dst.getParentFile().exists()) {
            dst.getParentFile().mkdirs();
        }
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }
    }


}
