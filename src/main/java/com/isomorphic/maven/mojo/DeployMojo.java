package com.isomorphic.maven.mojo;

import com.isomorphic.maven.packaging.Module;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Deploys a collection of {@link Module}s to the Maven repository location indicated by the given {@link #repositoryUrl} property. 
 * Functionally, pretty much just like the Deploy Plugin's deploy-file goal, except this one works on a collection.
 * <p>
 * Refer to <a href="http://maven.apache.org/plugins/maven-deploy-plugin/deploy-file-mojo.html"></a>
 */
@Mojo(name="deploy", requiresProject=false)
public final class DeployMojo extends AbstractPackagerMojo {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeployMojo.class);

    /**
     * The identifier of a server entry from which Maven should read the authentication credentials
     * to be used during deployment to {@link #repositoryUrl}.
     * <p>
     * Refer to <a href="http://maven.apache.org/ref/3.1.1/maven-settings/settings.html#class_server"></a>
     * @since 1.0.0
     */
    @Parameter(property="repositoryId")
    private String repositoryId;

    /**
     * The URL of the location to which the artifacts should be deployed.  e.g.,
     * <p/>
     * <a href="http://nexus.corp.int/nexus/content/repositories/thirdparty/"></a>
     *
     * @since 1.0.0
     */
    @Parameter(property="repositoryUrl", required=true)
    private String repositoryUrl;

    /**
     * The repositoryType, as required by the Builder(String, String, String) constructor.
     * <p>
     * Refer to <a href="http://http://download.eclipse.org/aether/aether-core/0.9.0.M2/apidocs/org/eclipse/aether/repository/RemoteRepository.Builder.html#RemoteRepository.Builder(java.lang.String,%20java.lang.String,%20java.lang.String)">
     * @since 1.0.0
     */
    @Parameter(property="repositoryType", defaultValue="default")
    private String repositoryType;

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

}
