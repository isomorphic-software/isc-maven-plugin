package com.isomorphic.maven.mojo.reify;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 *  A utility class to run the {@link ImportMojo}'s 
 *  <a href="https://www.smartclient.com/smartgwtee-latest/server/javadoc/com/isomorphic/tools/ReifyDataSourceValidator.html">validation step</a>
 *  independently of the import process.
 *  <p>
 *  Invocation will require values for smartclientRuntimeDir, dataSourcesDir, and optionally 
 *  mockDataSourcesDir parameters. 
 *  
 *  @see ImportMojo
 */
@Mojo(name="reify-validate", requiresProject=false)
public class ValidateMojo extends ImportMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.validate();
    }
}	
