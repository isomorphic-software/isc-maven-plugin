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

import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.isomorphic.maven.packaging.Module;

/**
 * Deploys a collection of {@link Module}s to the Maven repository location indicated by the given {@link #repositoryUrl} property. 
 * Functionally, pretty much just like the Deploy Plugin's deploy-file goal, except this one works on a collection.
 * 
 * @see http://maven.apache.org/plugins/maven-deploy-plugin/deploy-file-mojo.html
 */
@Mojo(name="deploy", requiresProject=false)
public final class DeployMojo extends AbstractPackagerMojo {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DeployMojo.class);
	
	/**
	 * The identifier of a server entry from which Maven should read the authentication credentials
	 * to be used during deployment to {@link #repositoryUrl}. 
	 * 
	 * @see http://maven.apache.org/ref/3.1.1/maven-settings/settings.html#class_server
	 * @since 1.0.0
	 */
	@Parameter(property="repositoryId")
	private String repositoryId;
	
	/**
	 * The URL of the location to which the artifacts should be deployed.  e.g.,
	 * <p/>
	 * http://nexus.corp.int/nexus/content/repositories/thirdparty/
	 * 
	 * @since 1.0.0
	 */
	@Parameter(property="repositoryUrl", required=true)
	private String repositoryUrl;
	
	/**
	 * The repositoryType, as required by the Builder(String, String, String) constructor.
	 * 
	 * @see <a href="http://http://download.eclipse.org/aether/aether-core/0.9.0.M2/apidocs/org/eclipse/aether/repository/RemoteRepository.Builder.html#RemoteRepository.Builder(java.lang.String,%20java.lang.String,%20java.lang.String)">
	 * @since 1.0.0
	 */
	@Parameter(property="repositoryType", defaultValue="default")
	private String repositoryType;
	
	@Component 
    private SettingsDecrypter settingsDecrypter; 
	
	/**
	 * Deploy each of the provided {@link Module}s, along with their SubArtifacts (POMs, JavaDoc bundle, etc.), to the repository location
	 * indicated by {@link #repositoryUrl}.
	 */
	@Override 
	public void doExecute(Set<Module> artifacts) throws MojoExecutionException, MojoFailureException {

    	RemoteRepository.Builder builder = new RemoteRepository.Builder(repositoryId, repositoryType, repositoryUrl);
		Authentication authentication = getAuthentication(repositoryId);
    	builder.setAuthentication(authentication);

    	LOGGER.info("Deploying to repositoryId '{}' with credentials: {}", repositoryId, authentication == null ? null : authentication.toString());
    	
    	RemoteRepository repository = builder.build();
		
		for (Module artifact : artifacts) {
	     
			DeployRequest deployRequest = new DeployRequest();
			deployRequest.addArtifact(artifact); 
        	deployRequest.setRepository(repository);
        	
			for (Artifact subArtifact : artifact.getAttachments()) {
	        	deployRequest.addArtifact(subArtifact);
	        }
	        
	        try {
				repositorySystem.deploy(repositorySystemSession, deployRequest);
			} catch (DeploymentException e) {
				throw new MojoFailureException("Deployment failed: ", e);
			}	

		}		
	}
	
	/**
	 * Returns user credentials for the server with the given id, as kept in Maven
	 * settings.  
	 * 
	 * @param serverId the id of the server containing the authentication credentials
	 * @return the Authentication credentials for the given server with the given id
	 * 
	 * @see http://maven.apache.org/settings.html#Servers
	 */
	private Authentication getAuthentication(String serverId) {
		
		Authentication authentication = null;
		Server server = getDecryptedServer(serverId);
		
		if (server != null) {
	    	authentication = new AuthenticationBuilder()
				.addUsername(server.getUsername())
				.addPassword(server.getPassword())
				.addPrivateKey(server.getPrivateKey(), server.getPassphrase())
				.build();
		}
		return authentication;
	}

	/**
	 * Decrypt settings and return the server element with the given id.  Useful for e.g., reading encrypted 
	 * user credentials.
	 * 
	 * @param id the id of the server to be decrypted
	 * @return a Server with its protected elements decrypted, if one is found with the given id.  Null otherwise.
	 * 
	 * @see http://maven.apache.org/guides/mini/guide-encryption.html
	 */
    private Server getDecryptedServer(String id) { 
        final SettingsDecryptionRequest settingsDecryptionRequest = new DefaultSettingsDecryptionRequest(); 
        settingsDecryptionRequest.setServers(settings.getServers()); 
        final SettingsDecryptionResult decrypt = settingsDecrypter.decrypt(settingsDecryptionRequest); 
        List<Server> servers = decrypt.getServers();
        
        for (Server server : servers) {
        	if (server.getId().equals(id)) {
        		return server;
        	}
        }
        return null;
    } 
}