package com.isomorphic.maven.mojo.reify;

import com.isomorphic.maven.util.AntProjectLogger;
import org.apache.tools.ant.BuildException;

import java.io.File;

/**
 * An Ant task allowing the Reify {@link ValidateMojo} to be run from Ant builds.  Note that
 * default values have been changed to accomodate a typical Ant project structure, otherwise
 * functionality is unchanged.
 * 
 * @see ValidateMojo
 */
public class ValidateTask extends ImportTask {

    @Override
    public void execute() throws BuildException {
        ValidateMojo mojo = new ValidateMojo();

        mojo.setLog(new AntProjectLogger(getProject()));

        mojo.setWebappDir(webappDir != null ? new File(webappDir) : new File(getProject().getBaseDir(), webappDir));
        mojo.setSmartclientRuntimeDir(smartclientRuntimeDir != null ? new File(smartclientRuntimeDir) : new File(getProject().getBaseDir(), "war/isomorphic"));
        mojo.setDataSourcesDir(dataSourcesDir);
        mojo.setMockDataSourcesDir(mockDataSourcesDir);
        mojo.setValidationFailureThreshold(validationFailureThreshold);

        try {
            mojo.execute();
        } catch (Exception e) {
            throw new BuildException(e);
        }

    }

}
