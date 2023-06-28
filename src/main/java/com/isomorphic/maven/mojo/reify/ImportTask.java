package com.isomorphic.maven.mojo.reify;

import com.isomorphic.maven.util.AntProjectLogger;
import com.isomorphic.util.ErrorMessage.Severity;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.maven.settings.Proxy;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.File;

/**
 * An Ant task allowing the Reify {@link ImportMojo} to be run from Ant builds.  Note that
 * default values have been changed to accomodate a typical Ant project structure, otherwise
 * functionality is unchanged.
 * <p>
 * To use this Task, just add a taskdef to the desired build target, and make sure you have the
 * required classpath entries set up correctly.  If you've built your project using one of the 
 * Maven archetypes for either  
 * <a href="https://www.smartclient.com/smartgwt-latest/javadoc/com/smartgwt/client/docs/MavenSupport.html">SmartGWT</a>
 * or
 * <a href="https://www.smartclient.com/smartclient-latest/isomorphic/system/reference/?id=group..mavenSupport">SmartClient</a>
 * (and subsequently issued the ant 'unmaven' target), this should all be done for you.  
 * Otherwise, you'll want to add something like the following to your build:
 * <pre>
 * &lt;property name="reify.lib" value="${basedir}/build/ivy/reify/lib" /&gt;
 * 
 * &lt;path id="reify.classpath"&gt;
 *     &lt;fileset dir="${reify.lib}" erroronmissingdir="false"&gt;
 *       &lt;include name="**&#47;*.jar" /&gt;
 *     &lt;/fileset&gt;
 * &lt;/path&gt;
 * 
 * &lt;target name="reify-tasklibs"&gt;
 *     &lt;mkdir dir="${reify.lib}" /&gt;
 *     &lt;ivy:resolve conf="reify" /&gt;
 *     &lt;ivy:retrieve conf="reify" pattern="${reify.lib}/[artifact]-[revision](-[classifier]).[ext]"/&gt;
 * &lt;/target&gt;
 * 
 * &lt;target name="reify-import" depends="reify-tasklibs"&gt;
 * 
 *     &lt;taskdef name="reify-import" classname="com.isomorphic.maven.mojo.reify.ImportTask" classpathref="reify.classpath"/&gt;
 *     &lt;reify-import projectName="MyProject" username="${username}" password="${password}" 
 *                   datasourcesDir="WEB-INF/ds/classic-models" 
 *                   smartclientRuntimeDir="${basedir}/war/isomorphic" /&gt;
 * &lt;/target&gt;
 * </pre>
 * Use of ivy is of course optional,  you just need somehow to get the isomorphic_m2pluginextras module and its 
 * <a href="https://www.smartclient.com/smartclient-latest/isomorphic/system/reference/?id=group..javaModuleDependencies">dependencies</a>
 * on your classpath - here they are copied to the directory defined in the <code>reify.lib</code> property)
 * <p>
 * Note that users working behind a proxy may need to provide server information to Ant 
 * by way of the ANT_OPTS environment variable:
 * <pre>
 * export ANT_OPTS="-Dhttp.proxyHost=myproxyhost -Dhttp.proxyPort=8080 -Dhttp.proxyUser=myproxyusername -Dhttp.proxyPassword=myproxypassword"
 * </pre>
 */
public class ImportTask extends Task {

    protected String serverUrl = "https://create.reify.com";
    protected String workdir;
    protected String webappDir = "war";
    protected String smartclientRuntimeDir;
    protected boolean includeTestJsp;
    protected String testJspPathname;
    protected boolean includeTestHtml;
    protected String testHtmlPathname;
    protected String dataSourcesDir = "WEB-INF/ds";
    protected String mockDataSourcesDir = "WEB-INF/ds/mock";
    protected boolean skipValidationOnImport;
    protected Severity validationFailureThreshold = Severity.ERROR;
    protected String uiDir = "WEB-INF/ui";
    protected String projectFileDir = "WEB-INF/ui";
    protected boolean modifyWelcomeFiles;
    protected boolean drawOnWelcomeFiles;
    protected String projectName;
    protected String projectFileName;
    protected String zipFileName;

    protected String username;
    protected String password;


    @Override
    public void execute() throws BuildException {

        ImportMojo mojo = new ImportMojo();
        mojo.setLog(new AntProjectLogger(getProject()));

        if (username == null || password == null) {
            throw new BuildException("Username and password parameters required.");
        }

        if (projectName == null) {
            projectName = getProject().getName();
        }

        mojo.setHost(String.valueOf(serverUrl));
        mojo.setCredentials(new UsernamePasswordCredentials(username, password));

        mojo.setWorkdir(workdir != null ? new File(workdir) : new File(getProject().getBaseDir(), "build/reify"));
        mojo.setWebappDir(webappDir != null ? new File(webappDir) : new File(getProject().getBaseDir(), webappDir));
        mojo.setSmartclientRuntimeDir(smartclientRuntimeDir != null ? new File(smartclientRuntimeDir) : new File(getProject().getBaseDir(), "war/isomorphic"));
        mojo.setIncludeTestJsp(includeTestJsp);
        mojo.setTestJspPathname(testJspPathname = testJspPathname != null ? testJspPathname : projectName + ".run.jsp");
        mojo.setIncludeTestHtml(includeTestHtml);
        mojo.setTestHtmlPathname(testHtmlPathname = testHtmlPathname != null ? testHtmlPathname : projectName + ".run.html");
        mojo.setDataSourcesDir(dataSourcesDir);
        mojo.setMockDataSourcesDir(mockDataSourcesDir);
        mojo.setSkipValidationOnImport(skipValidationOnImport);
        mojo.setValidationFailureThreshold(validationFailureThreshold);
        mojo.setUiDir(uiDir);
        mojo.setProjectFileDir(projectFileDir);
        mojo.setModifyWelcomeFiles(modifyWelcomeFiles);
        mojo.setDrawOnWelcomeFiles(drawOnWelcomeFiles);
        mojo.setProjectName(projectName);
        mojo.setProjectFileName(projectFileName != null ? projectFileName : projectName + "proj.xml");
        mojo.setZipFileName(zipFileName != null ? zipFileName : projectName + "proj.zip");


        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPort = System.getProperty("http.proxyPort");
        String proxyUser = System.getProperty("http.proxyUser");
        String proxyPassword = System.getProperty("http.proxyPassword");

        //undocumented by Ant, allowed / expected by Maven so allow it here
        String proxyBypass = System.getProperty("http.nonProxyHosts");

        if (proxyHost != null || proxyPort != null) {
            Proxy p = new Proxy();
            p.setHost(proxyHost != null ? proxyHost : "");
            p.setPort(Integer.valueOf(proxyPort != null ? proxyPort : "8080"));
            p.setUsername(proxyUser != null ? proxyUser : "");
            p.setPassword(proxyPassword != null ? proxyPassword : "");
            p.setNonProxyHosts(proxyBypass != null ? proxyBypass : "localhost");
            p.setActive(true);

            mojo.setProxy(p);
        }

        try {
            mojo.execute();
        } catch (Exception e) {
            throw new BuildException(e);
        }

    }

    /**
     * Change the default value of <strong><a href="https://create.reify.com"></a></strong>.
     * @param serverUrl the new value
     * @see  ImportMojo#serverUrl
     */
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    /**
     * Change the default value of <strong>${basedir}/build/reify</strong>.
     * @param workdir the new value
     * @see  ImportMojo#workdir
     */
    public void setWorkdir(String workdir) {
        this.workdir = workdir;
    }


    /**
     * Change the default value of <strong>${basedir}/war</strong>.
     * @param webappDir the new value
     * @see ImportMojo#webappDir
     */
    public void setWebappDir(String webappDir) {
        this.webappDir = webappDir;
    }

    /**
     * Change the default value of <strong>${basedir}/war/isomorphic</strong>.
     * @param smartclientRuntimeDir the new value
     * @see ImportMojo#smartclientRuntimeDir
     */
    public void setSmartclientRuntimeDir(String smartclientRuntimeDir) {
        this.smartclientRuntimeDir = smartclientRuntimeDir;
    }

    /**
     * Change the default value of <strong>false</strong>.
     * @param includeTestJsp the new value
     * @see ImportMojo#includeTestJsp
     */
    public void setIncludeTestJsp(boolean includeTestJsp) {
        this.includeTestJsp = includeTestJsp;
    }

    /**
     * Change the default value of <strong>{@link #projectName}.run.jsp</strong>.
     * @param testJspPathname the new value
     * @see ImportMojo#testJspPathname
     */
    public void setTestJspPathname(String testJspPathname) {
        this.testJspPathname = testJspPathname;
    }

    /**
     * Change the default value of <strong>false</strong>.
     * @param includeTestHtml the new value
     * @see ImportMojo#includeTestHtml
     */
    public void setIncludeTestHtml(boolean includeTestHtml) {
        this.includeTestHtml = includeTestHtml;
    }

    /**
     * Change the default value of <strong>{@link #projectName}.run.html</strong>.
     * @param  testHtmlPathname the new value
     * @see ImportMojo#testHtmlPathname
     */
    public void setTestHtmlPathname(String testHtmlPathname) {
        this.testHtmlPathname = testHtmlPathname;
    }

    /**
     * Change the default value of WEB-INF/ds.
     * @param dataSourcesDir the new value
     * @see ImportMojo#dataSourcesDir
     */
    public void setDataSourcesDir(String dataSourcesDir) {
        this.dataSourcesDir = dataSourcesDir;
    }

    /**
     * Change the default value of <strong>{@link #dataSourcesDir}/mock</strong>.
     * @param mockDataSourcesDir the new value
     * @see ImportMojo#mockDataSourcesDir
     */
    public void setMockDataSourcesDir(String mockDataSourcesDir) {
        this.mockDataSourcesDir = mockDataSourcesDir;
    }

    /**
     * Change the default value of <strong>false</strong>.
     * @param skipValidationOnImport the new value
     * @see ImportMojo#skipValidationOnImport
     */
    public void setSkipValidationOnImport(boolean skipValidationOnImport) {
        this.skipValidationOnImport = skipValidationOnImport;
    }

    /**
     * Change the default value of <strong>ERROR</strong>.
     * Other legal values include INFO and WARN.
     * @param validationFailureThreshold the new value
     * @see ImportMojo#validationFailureThreshold
     */
    public void setValidationFailureThreshold(Severity validationFailureThreshold) {
        this.validationFailureThreshold = validationFailureThreshold;
    }

    /**
     * Change the default value of <strong>WEB-INF/ui</strong>.
     * @param uiDir the new value
     * @see ImportMojo#uiDir
     */
    public void setUiDir(String uiDir) {
        this.uiDir = uiDir;
    }

    /**
     * Change the default value of <strong>WEB-INF/ui</strong>.
     * @param projectFileDir the new value
     * @see ImportMojo#projectFileDir
     */
    public void setProjectFileDir(String projectFileDir) {
        this.projectFileDir = projectFileDir;
    }

    /**
     * Change the default value of <strong>false</strong>.
     * @param modifyWelcomeFiles the new value
     * @see ImportMojo#modifyWelcomeFiles
     */
    public void setModifyWelcomeFiles(boolean modifyWelcomeFiles) {
        this.modifyWelcomeFiles = modifyWelcomeFiles;
    }

    /**
     * Change the default value of <strong>false</strong>.
     * @param drawOnWelcomeFiles the new value
     * @see ImportMojo#drawOnWelcomeFiles
     */
    public void setDrawOnWelcomeFiles(boolean drawOnWelcomeFiles) {
        this.drawOnWelcomeFiles = drawOnWelcomeFiles;
    }

    /**
     * Change the default value of <strong>${ant.project.name}</strong>.
     * @param projectName the new value
     * @see ImportMojo#projectName
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * Set the username needed to authenticate to <a href="https://create.reify.com/"></a>
     * @param username the new value
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Set the password needed to authenticate to <a href="https://create.reify.com/"></a>
     * @param password the new value
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
