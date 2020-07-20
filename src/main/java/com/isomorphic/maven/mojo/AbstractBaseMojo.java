package com.isomorphic.maven.mojo;

import java.util.List;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBaseMojo extends AbstractMojo {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBaseMojo.class);

    @Parameter(readonly = true, defaultValue = "${repositorySystemSession}")
    protected RepositorySystemSession repositorySystemSession;

    @Component
    protected ModelBuilder modelBuilder;

    @Component
    protected MavenProject project;

    @Component
    protected RepositorySystem repositorySystem;
    
    @Component
    protected ArtifactResolver artifactResolver;

    @Component
    protected RemoteRepositoryManager remoteRepositoryManager;
    
    @Component
    protected Settings settings;

    @Component
    private SettingsDecrypter settingsDecrypter; 

    @Override
    public abstract void execute() throws MojoExecutionException, MojoFailureException;

    public UsernamePasswordCredentials getCredentials(String serverId) {
        Server server = getDecryptedServer(serverId);
        String username = null;
        String password = null;
        if (server != null) {
            username = server.getUsername();
            password = server.getPassword();
            return new UsernamePasswordCredentials(username, password);
        }
        return null;
    }

    /**
     * Returns user credentials for the server with the given id, as kept in Maven
     * settings.
     * <p>
     * Refer to http://maven.apache.org/settings.html#Servers
     *
     * @param serverId the id of the server containing the authentication credentials
     * @return the Authentication credentials for the given server with the given id
     *
     */
    protected Authentication getAuthentication(String serverId) {

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
     * <p>
     * Refer to http://maven.apache.org/guides/mini/guide-encryption.html
     *
     * @param id the id of the server to be decrypted
     * @return a Server with its protected elements decrypted, if one is found with the given id.  Null otherwise.
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
