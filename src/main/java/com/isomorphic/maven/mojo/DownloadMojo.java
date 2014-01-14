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

import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.isomorphic.maven.packaging.Module;

/**
 * Downloads and unpacks a given distribution, but does not actually do anything with the resulting Maven artifacts. 
 */
@Mojo(name="download", requiresProject=false, requiresDirectInvocation=true)
public final class DownloadMojo extends AbstractPackagerMojo {

	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadMojo.class);
		
	@Override
	public void doExecute(Set<Module> artifacts)	throws MojoExecutionException, MojoFailureException {
	}

}