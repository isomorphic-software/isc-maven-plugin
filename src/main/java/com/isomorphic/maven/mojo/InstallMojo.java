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
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;

import com.isomorphic.maven.packaging.Module;

/**
 * Installs a collection of {@link Module}s to the user's local repository. 
 * Functionally, pretty much just like the Install Plugin's install-file goal, except this one works on a collection.
 * <p> 
 * Refer to http://maven.apache.org/plugins/maven-install-plugin/install-file-mojo.html
 */
@Mojo(name="install", requiresProject=false)
public final class InstallMojo extends AbstractPackagerMojo {
	
	/**
	 * Install each of the provided {@link Module}s, along with their SubArtifacts (POMs, JavaDoc bundle, etc.), to a local repository.
	 */
	@Override 
	public void doExecute(Set<Module> artifacts) throws MojoExecutionException, MojoFailureException {
	
		for (Module artifact : artifacts) {
	     
			InstallRequest installRequest = new InstallRequest();
	        installRequest.addArtifact(artifact);
	        
	        for (Artifact subArtifact : artifact.getAttachments()) {
	        	installRequest.addArtifact(subArtifact);
	        }

	        try {
				repositorySystem.install(repositorySystemSession, installRequest );
			} catch (InstallationException e) {
				throw new MojoFailureException("Installation failed: ", e);
			}			
		}
		
	}
	
}