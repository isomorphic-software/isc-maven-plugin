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
import java.util.List;
import java.util.Set;

import static com.isomorphic.maven.packaging.Product.REIFY_ONSITE;

/**
 * Downloads and unpacks a Reify OnSite distribution and deploys it to {@link #targetDir}.
 * Note that the current version of this goal only supports installing the SmartClient version
 * of Reify OnSite.  Isomorphic does also provide a version of Reify OnSite packaged as a
 * SmartGWT application, but that is outside the scope of this plugin.
 * <p>
 * Note that Reify OnSite is NOT a Maven project,  This goal installs the actual product in
 * the target directory you specify; it does not install files to any Maven cache or
 * repository.
 */
@Mojo(name="install-reify-onsite", requiresProject=false, requiresDirectInvocation=true)
public final class InstallReifyOnSiteMojo extends AbstractReifyOnSiteGoalsMojo {

    /**
     * Path to the directory where Reify OnSite should be installed.  By default, this
     * directory will be created if necessary - see {@link #createDir}.  If the target
     * directory already exists and is not empty, by default this goal will fail - see
     * {@link #overwriteTarget}
     * <p>
     * As described in the {@link #standalone} property docs, Reify OnSite can run standalone
     * or as a regular webapp.  If you are running standalone (the default), you can deploy to
     * any target directory you like.  If you are deploying as a regular webapp in an existing
     * server, the targetDir MUST be the directory configured as the context root in your
     * servlet container - for example, in Tomcat it should be
     * "tomcat/webapps/ROOT", in other servlet engines you often configure it with a
     * <code>&lt;context-root&gt;</code> tag or similar.  This is because Reify OnSite is literally the exact
     * same code running at <code>https://create.reify.com</code>, including the automatic redirecting we do
     * for authentication, so it expects to be running in the context root and will fail if you
     * try to run it elsewhere.
     *
     * @since 1.5.0
     */
    @Parameter(property="targetDir", required=true)
    private String targetDir;

    /**
     * If the {@link #targetDir} already exists and is not empty, should we proceed anyway?
     * By default, the goal will fail
     *
     * @since 1.5.0
     */
    @Parameter(property="overwriteTarget", defaultValue="false")
    private Boolean overwriteTarget;

    /**
     * If the {@link #targetDir} does not exist, should we create it automatically, including
     * any parent directories necessary?  Defaults to true
     *
     * @since 1.5.0
     */
    @Parameter(property="createDir", defaultValue="true")
    private Boolean createDir;

    /**
     * Reify OnSite bundles an embedded version of Tomcat which enables it to run standalone
     * with a simple CLI command.  It can also be installed as a regular webapp under an
     * existing installation of Tomcat (or whatever other servlet engine you wish to use).
     * By default we install for standalone use; if you do not intend to run standalone, set
     * standalone=false and the plugin will not deploy the standalone elements (but also read
     * the important proviso about restrictions on {@link #targetDir} when not running in
     * standalone mode)
     *
     * @since 1.5.0
     */
    @Parameter(property="standalone", defaultValue="true")
    private Boolean standalone;

    /**
     * The HTTP port that Reify OnSite should listen on, in {@link #standalone} mode.
     * Defaults to "8080".  Ignored if we are not installing in standalone mode
     *
     * @since 1.5.0
     */
    @Parameter(property="port", defaultValue="8080")
    private String port;

    /**
     * Limits the skins installed with Reify OnSite to the names in this comma-separated list.
     * E.g., <code>-Dskins=Tahoe,Stratus</code> will remove all skins except Tahoe and Stratus
     * (plus a couple that are always installed because various parts of the system expect
     * them to be present)
     *
     * @since 1.5.0
     */
    @Parameter(property = "skins")
    public void setSkins(String skins) {
        this.skins = skins;
    }

    // We originally tried to hide the optional module flags here, but this approach does not
    // work - ChatGPT completely hallucinated this "hidden" property, which it insisted was
    // present only in Maven plugin annotations >= 3.7.0, and had me looking for all kinds of
    // unlikely reason why it wasn't working.  Eventually, inspecting the source showed that
    // there has never been a "hidden" property in any release.  So instead we refactored the
    // parameters down to just those classes to which they are applicable; this was a better
    // approach anyway, because some parameters are applicable to both Core and Reify OnSite
    // Goals, but the documentation needs to be different
/*
    @Parameter(property = "includeAI", hidden = true)
    private Boolean includeAI;

    @Parameter(property = "includeAnalytics", hidden = true)
    private Boolean includeAnalytics;

    @Parameter(property = "includeMessaging", hidden = true)
    private Boolean includeMessaging;
*/


    public void doExecute(File basedir) throws MojoExecutionException, MojoFailureException {
        sourceRoot = new File(basedir, "reifyOnSite");
        if (!sourceRoot.isDirectory()) {
            throw new MojoFailureException("The reifyOnSite directory does not exist in " + basedir.toString());
        }
        targetRoot = new File(targetDir);
        if (targetRoot.exists()) {
            if (targetRoot.isDirectory()) {
                File[] contents = targetRoot.listFiles();
                if (contents != null && contents.length > 0) {
                    if (!overwriteTarget) {
                        throw new MojoExecutionException("targetDir " + targetDir + " already " +
                                "exists and is not empty. Stopping right now for safety " +
                                "reasons.  To overwrite the existing directory, specify " +
                                "overwriteTarget=true");
                    }
                }
            } else  {
                throw new MojoExecutionException("targetDir " + targetDir + " is not a directory");
            }
        } else {
            if (!createDir) {
                throw new MojoExecutionException("targetDir " + targetDir + " does not exist.  " +
                            "Specify createDir=true to create the targetDir automatically");
            } else {
                targetRoot.mkdirs();
            }
        }

        log.info("Installing Reify OnSite to target directory " + targetRoot.getAbsolutePath());

        // This is the install Mojo - we can just copy everything, excluding the embedded
        // Tomcat stuff if that is not required
        AntPathMatcherFilter filter = new AntPathMatcherFilter();
        filter.include("**");
        if (!standalone) {
            log.info("Not in standalone mode, excluding embedded Tomcat");
            filter.exclude("start_embedded_server.*,WEB-INF/bin/**,WEB-INF/embeddedTomcat/**");
        }

        fileCount = 0;
        copyRecursively(sourceRoot, filter);

        log.info("Copied " + fileCount + " files to " + targetRoot.getAbsolutePath());

        if (standalone) {
            // Unix-derived OS's require scripts to be marked executable
            String[] fileNames = {"start_embedded_server.sh", "start_embedded_server.command",
                    "WEB-INF/bin/embeddedTomcat.sh", "WEB-INF/bin/embeddedTomcat.command"};
            for (int i = 0; i < fileNames.length; i++) {
                File f = new File(targetRoot, fileNames[i]);
                // This call is a harmless no-op on Windows
                f.setExecutable(true, false);
            }
            log.info("Marked standalone scripts as executable");

            if (!"8080".equals(port)) {
                try {
                    File f = new File(targetRoot, "start_embedded_server.sh");
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            // Simple literal replacement
                            line = line.replace("port 8080", "port " + port);
                            sb.append(line).append(System.lineSeparator());
                        }
                    }

                    // Write the modified text back to the same file
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(f))) {
                        writer.write(sb.toString());
                    }

                    log.info("Changed listening port to " + port);
                } catch(IOException ioe) {
                    throw new MojoFailureException("Caught IO Exception trying to modify " +
                            "embedded Tomcat to listen on port " + port, ioe);
                }
            }
        }
    }

}
