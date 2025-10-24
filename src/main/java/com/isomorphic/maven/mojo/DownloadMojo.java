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

import com.isomorphic.maven.packaging.Module;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Downloads and unpacks a given distribution, but does not actually do anything with the resulting Maven artifacts. 
 */
@Mojo(name="download", requiresProject=false, requiresDirectInvocation=true)
public final class DownloadMojo extends AbstractPackagerMojo {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadMojo.class);

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
    public void setIncludeAnI(Boolean includeAI) {
        this.incAI = includeAI;
    }

    @Override
    public void doExecute(Set<Module> artifacts) throws MojoExecutionException, MojoFailureException {
    }

}
