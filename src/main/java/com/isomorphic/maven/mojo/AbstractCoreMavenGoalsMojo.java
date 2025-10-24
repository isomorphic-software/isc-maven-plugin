package com.isomorphic.maven.mojo;

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

import com.isomorphic.maven.packaging.License;
import com.isomorphic.maven.packaging.Module;
import com.isomorphic.maven.packaging.Product;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Set;

/**
 * This is the shared "core" Mojo that the primary goals - install, deploy, download - inherit
 * from.  It was introduced when we refactored at version 1.5.0 because many of the parameters
 * that were previously annotated and documented in AbstractBaseMojo are not applicable to the
 * new Reify OnSite goals that we introduced with that release, but we do want those goals to
 * extend AbstractBaseMojo because they make use of substantial shared facilities, like
 * obtaining the latest release number, downloading and unpacking.
 *
 * So we define the annotations and docs for the core functions here, so they are inherited
 * by the core goals but not the Reify OnSite ones.
 */
public class AbstractCoreMavenGoalsMojo extends AbstractPackagerMojo {


    /**
     * One of SMARTGWT or SMARTCLIENT.
     *
     * @since 1.0.0
     */
    @Parameter(property = "product", defaultValue = "SMARTGWT")
    public void setProduct(Product product) {
        this.product = product;
    }

    /**
     * If true, makes a copy of the given distribution in a 'latest' subdirectory.
     * Can be useful for bookmarking documentation, etc. but adds additional install time
     * and storage requirements.
     *
     * @since 1.4.0
     */
    @Parameter(property = "copyToLatestFolder", defaultValue = "false")
    public void setCopyToLatestFolder(Boolean copyToLatest) {
        this.copyToLatest = copyToLatest;
    }

    /**
     * One of: LGPL, EVAL, PRO, POWER, ENTERPRISE.  As of version 1.4.7, it is no longer valid
     * to specify optional modules like ANALYTICS_MODULE for this property.  To include
     * optional modules ANALYTICS_MODULE, MESSAGING_MODULE or AI_MODULE, use the
     * {@link #setIncludeAnalytics} / {@link #setIncludeMessaging} / (@link #setIncludeAI} properties,
     * respectively, to cause the optional modules to be included with the base
     * installation / deployment.
     *
     * @since 1.0.0
     */
    @Parameter(property = "license", required = true)
    public void setLicense(License license) {
        this.license = license;
    }

    /**
     * If true, no attempt it made to extract the contents of any distribution.
     * Only useful in the case where some manual intervention is required
     * between download and another step. For example, it would be possible to
     * first run the download goal, manipulate the version number of some
     * dependency in some POM, and then run the install goal with
     * skipExtraction=false to prevent the modified POM from being overwritten.
     * <p>
     * This is the kind of thing that should generally be avoided, however.
     */
    @Parameter(property = "skipExtraction", defaultValue = "false")
    public void setSkipExtraction(Boolean skipExtract) {
        this.skipExtract = skipExtract;
    }


    /**
     * Limits the skins installed with the runtime to the names in this comma-separated list.
     * E.g., <code>-Dskins=Tahoe,Stratus</code> will remove all skins except Tahoe and Stratus.
     * Note that these deleted resources cannot be recovered except by reinstalling the artifact(s) to
     * your repository.
     *
     * @since 1.4.6
     */
    @Parameter(property = "skins")
    public void setSkins(String skins) {
        this.skins = skins;
    }


    /**
     * If true, artifacts should be <a href=
     * "http://books.sonatype.com/mvnref-book/reference/pom-relationships-sect-pom-syntax.html#pom-reationships-sect-versions"
     * >versioned</a> with the 'SNAPSHOT' qualifier, in the case of development
     * builds only. The setting has no effect on patch builds.
     * <p>
     * If false, each artifact's POM file is modified to remove the unwanted
     * qualifier. This can be useful if you need to deploy a development build
     * to a production environment.
     *
     * @since 1.0.0
     */
    @Parameter(property = "snapshots", defaultValue = "true")
    public void setSnapshots(Boolean snapshots) {
        this.snapshots = snapshots;
    }

    /**
     * If true, the optional messaging module (bundled and distributed
     * separately) has been licensed and should be downloaded with the
     * distribution specified by {@link #license}.
     *
     * @since 1.0.0
     */
    @Parameter(property = "includeMessaging", defaultValue = "false")
    public void setIncludeMessaging(Boolean includeMessaging) {
        this.incMessaging = includeMessaging;
    }

    /**
     * If true, the optional analytics module (bundled and distributed
     * separately) has been licensed and should be downloaded with the
     * distribution specified by {@link #license}.
     *
     * @since 1.0.0
     */
    @Parameter(property = "includeAnalytics", defaultValue = "false")
    public void setIncludeAnalytics(Boolean includeAnalytics) {
        this.incAnalytics = includeAnalytics;
    }

    /**
     * If true, the optional AI module (bundled and distributed
     * separately) has been licensed and should be downloaded with the
     * distribution specified by {@link #license}.
     *
     * @since 1.4.7
     */
    @Parameter(property = "includeAI", defaultValue = "false")
    public void setIncludeAI(Boolean includeAI) {
        this.incAI = includeAI;
    }

    @Override
    public void doExecute(Set<Module> artifacts) throws MojoExecutionException, MojoFailureException {
    }

}
