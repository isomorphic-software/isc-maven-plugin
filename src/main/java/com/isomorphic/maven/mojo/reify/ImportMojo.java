package com.isomorphic.maven.mojo.reify;

import static com.isomorphic.util.ErrorMessage.Severity.ERROR;
import static com.isomorphic.util.ErrorMessage.Severity.INFO;
import static com.isomorphic.util.ErrorMessage.Severity.WARN;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Proxy;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.XPath;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.isomorphic.maven.mojo.AbstractBaseMojo;
import com.isomorphic.maven.util.ArchiveUtils;
import com.isomorphic.maven.util.HttpRequestManager;
import com.isomorphic.maven.util.LoggingCountingOutputStream;
import com.isomorphic.tools.ReifyDataSourceValidator;
import com.isomorphic.util.ErrorMessage;
import com.isomorphic.util.ErrorMessage.Severity;
import com.isomorphic.util.ErrorReport;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;

/**
 * Provides for single-step download and extraction of assets hosted on the Reify platform.
 * While there is nothing to prevent a user from modifying the imported resources, changes 
 * should almost always be made using Reify and then re-imported. If necessary, use the API 
 * (e.g., RPCManager.createScreen, Canvas.getByLocalId) to modify imported screen definitions 
 * at runtime.  Imported MockDataSources can simply be replaced using e.g., SQLDataSources 
 * having the same ID in another directory - there is no reason to modify the contents of the 
 * 'mock' subdirectory.
 * <p> 
 * To encourage recommended usage, the reify-import goal takes steps to detect local changes 
 * and fail when any are found.  Refer to the {@link #skipOverwriteProtection}
 * parameter for details.  Further, a validation step (optional, see {@link ImportMojo#skipValidationOnImport}) 
 * attempts to detect <a href="https://www.smartclient.com/smartgwtee-latest/server/javadoc/com/isomorphic/tools/ReifyDataSourceValidator.html">commonly found discrepancies</a> 
 * between your MockDataSources and working DataSources.
 * <p>
 * The <a href="https://www.smartclient.com/smartclient-latest/isomorphic/system/reference/?id=group..reifyForDevelopers">Reify for Developers</a> 
 * documentation topic contains further discussion around best practices during the design / development cycle.
 * <p>
 * If you've built your project using one of the Maven archetypes for either  
 * <a href="https://www.smartclient.com/smartgwt-latest/javadoc/com/smartgwt/client/docs/MavenSupport.html">SmartGWT</a>
 * or
 * <a href="https://www.smartclient.com/smartclient-latest/isomorphic/system/reference/?id=group..mavenSupport">SmartClient</a>, 
 * you should have a skeleton configuration in you POM already.  if not, add something like the following:
 *
 * <pre>
 * &lt;pluginManagement&gt;
 *     &lt;plugins&gt;
 *         &lt;plugin&gt;
 *             &lt;groupId&gt;com.isomorphic&lt;/groupId&gt;
 *             &lt;artifactId&gt;isc-maven-plugin&lt;/artifactId&gt;
 *             &lt;version&gt;1.4.0-SNAPSHOT&lt;/version&gt;
 *             &lt;!-- the m2pluginextras dependency will be required when skipValidationOnImport = false --&gt;
 *             &lt;dependencies&gt;
 *                 &lt;dependency&gt;
 *                     &lt;groupId&gt;com.isomorphic.extras&lt;/groupId&gt;
 *                     &lt;artifactId&gt;isomorphic-m2pluginextras&lt;/artifactId&gt;
 *                    &lt;version&gt;${smartgwt.version}&lt;/version&gt;
 *                 &lt;/dependency&gt;       
 *             &lt;/dependencies&gt;
 *         &lt;/plugin&gt;
 *     &lt;/plugins&gt;
 * &lt;/pluginManagement&gt;                
 * 
 * &lt;build&gt;
 *     &lt;plugins&gt;
 *       &lt;plugin&gt;
 *           &lt;groupId&gt;com.isomorphic&lt;/groupId&gt;
 *           &lt;artifactId&gt;isc-maven-plugin&lt;/artifactId&gt;
 *           &lt;configuration&gt;
 *             &lt;smartclientRuntimeDir&gt;${project.parent.build.directory}/gwt/launcherDir/myapplication/sc&lt;/smartclientRuntimeDir&gt;
 *             &lt;dataSourcesDir&gt;WEB-INF/ds/classic-models&lt;/dataSourcesDir&gt;
 *           &lt;/configuration&gt;
 *       &lt;/plugin&gt;
 *     &lt;/plugins&gt;
 * &lt;/build&gt;
 * </pre> 
 * and check that the SmartClient runtime has been extracted to the configured location 
 * (via e.g., mvn war:exploded, mvn jetty:run).
 */
// Set requiresProject: false so that we can run the whole thing from an Ant project w/ no POM
@Mojo(name="reify-import", requiresProject=false)
public class ImportMojo extends AbstractBaseMojo {

    /**
     * The full URL of the Reify site hosting your project/s.  Useful when running your own
     * <a href="https://www.smartclient.com/smartclient-latest/isomorphic/system/reference/?id=group..reifyOnSite">Reify OnSite</a>
     * instance, at e.g., 'http://localhost:8080/create/'.
     *
     * @since 1.4.2
     */
    @Parameter(property = "serverUrl", defaultValue = "https://create.reify.com")
    protected String serverUrl;

    /**
     * The id of a <a href="http://maven.apache.org/settings.html#Servers">server
     * configuration</a> containing authentication credentials for the
     * Reify site specified by {@link ImportMojo#serverUrl}, used to download exported projects.
     *
     * @since 1.4.0
     */
    @Parameter(property = "serverId", defaultValue = "smartclient-developer")
    protected String serverId;

    /**
     * The directory to which the archive is downloaded and unpacked.  Note that this is
     * not the final destination of exported assets.  For that, see {@link ImportMojo#webappDir}.
     *
     * @since 1.4.0
     */
    @Parameter(property = "workdir", defaultValue = "${project.build.directory}/reify")
    protected File workdir;

    /**
     * The directory containing web application sources.
     *
     * @since 1.4.0
     */
    @Parameter(property = "webappDir", defaultValue = "${project.basedir}/src/main/webapp")
    protected File webappDir;

    /**
     * The directory containing the Isomorphic runtime.  The default value is appropriate for
     * the path created by a typical mvn war:exploded invocation.  Alternatively, set this to
     * the path created by mvn jetty:run, or any other path where an appropriate SmartClient
     * runtime may be found.
     *
     * Note that SmartGWT users may need to change the value to something like:
     * <p>
     * <code>
     * ${project.build.directory}/${project.build.finalName}/${gwtModuleName}/sc
     * </code>
     *
     * @since 1.4.0
     */
    @Parameter(property = "smartclientRuntimeDir", defaultValue="${project.build.directory}/${project.build.finalName}/isomorphic")
    protected File smartclientRuntimeDir;

    /**
     * If true, the import process will create a JSP launcher that loads the given project/s.
     * The file's name and location can be configured via {@link #testJspPathname}.
     *
     * @since 1.4.0
     */
    @Parameter(property = "includeTestJsp", defaultValue = "false")
    protected boolean includeTestJsp;

    /**
     * The name (and optional path, relative to {@link #webappDir}) of the JSP launcher to be
     * created when {@link #includeTestJsp} is true.
     *
     * @since 1.4.0
     */
    @Parameter(property = "testJspPathname", defaultValue = "${projectName}.run.jsp")
    protected String testJspPathname;

    /**
     * If true, the import process will create an HTML launcher that loads the given project/s.
     * The file's name and location can be configured via {@link #testHtmlPathname}.
     *
     * @since 1.4.0
     */
    @Parameter(property = "includeTestHtml", defaultValue = "false")
    protected boolean includeTestHtml;

    /**
     * The name (and optional path, relative to {@link #webappDir}) of the HTML launcher to be
     * created when {@link #includeTestHtml} is true.
     *
     * @since 1.4.0
     */
    @Parameter(property = "testHtmlPathname", defaultValue = "${projectName}.run.html")
    protected String testHtmlPathname;

    /**
     * The directory, relative to {@link #webappDir}), in which your project's working
     * datasources (i.e., other than mocks) reside.
     * <p>
     * Note that this will need to conform to the webapp's server.properties
     * <code>project.datasources</code> configuration.
     *
     * @since 1.4.0
     */
    @Parameter(property = "dataSourcesDir", defaultValue = "WEB-INF/ds")
    protected String dataSourcesDir;

    /**
     * The directory, relative to {@link #webappDir}), in which exported MockDataSources
     * should ultimately reside.
     * <p>
     * Note that this will need to conform to the webapp's server.properties
     * <code>project.datasources</code> configuration.
     *
     * @since 1.4.0
     */
    @Parameter(property = "mockDataSourcesDir", defaultValue = "WEB-INF/ds/mock")
    protected String mockDataSourcesDir;

    /**
     * When true, DataSource validation will be skipped following import.  The
     * {@link ValidateMojo} can still be executed independently at any time.
     *
     * @since 1.4.0
     */
    @Parameter(property = "skipValidationOnImport", defaultValue = "false")
    protected boolean skipValidationOnImport;

    /**
     * One of INFO, ERROR, WARN that will determine the level of severity tolerated in any
     * validation error.  Any error at or above this threshold will result in "BUILD FAILURE".
     *
     * @since 1.4.0
     */
    @Parameter(property = "validationFailureThreshold", defaultValue = "ERROR")
    protected Severity validationFailureThreshold;

    /**
     * The directory in which exported screens should ultimately reside, relative to
     * {@link #webappDir}.  Note that this will need to conform to the webapp's
     * server.properties <code>project.ui</code> configuration.
     *
     * @since 1.4.0
     */
    @Parameter(property = "uiDir", defaultValue = "WEB-INF/ui")
    protected String uiDir;

    /**
     * The directory to which the exported project file should ultimately reside, relative to
     * {@link #webappDir}.  Note that this will need to conform to the webapp's
     * server.properties <code>project.project</code> configuration.
     *
     * @since 1.4.0
     */
    @Parameter(property="projectFileDir", defaultValue = "WEB-INF/ui")
    protected String projectFileDir;

    /**
     * If true, the project's welcome files will be modified to include a script block that
     * loads the imported project.  Note that the loaded project is not drawn - for that,
     * refer to {@link #drawOnWelcomeFiles}.  Welcome files are determined by scanning the
     * project's web.xml file for the standard welcome-files declaration.  If none is found,
     * the webroot is searched for files named index.jsp and index.html.
     * <p>
     * Subsequent imports of other projects append the newly imported project name to the
     * existing one/s.
     *
     * @since 1.4.0
     */
    @Parameter(property="modifyWelcomeFiles", defaultValue = "false")
    protected boolean modifyWelcomeFiles;

    /**
     * Like {@link #modifyWelcomeFiles}, except this variant will draw the project's first
     * screen when all of its screens have been loaded.
     *
     * @since 1.4.0
     */
    @Parameter(property="drawOnWelcomeFiles", defaultValue = "false")
    protected boolean drawOnWelcomeFiles;

    /**
     * The name of the project as it's known by the reify.com environment.
     *
     * @since 1.4.0
     */
    @Parameter(property="projectName", defaultValue = "${project.artifactId}", required=true)
    protected String projectName;
    private String projectFileName;
    private String zipFileName;

    /**
     * When a project is imported, its screen and datasource assets are marked with a checksum
     * representing the contents of each file.  Subsequent imports are preceded by a check to
     * make sure each of these checksums still match the file's content.  In the event that
     * any do not, the import fails by default.
     * <p>
     * To override this behavior, you may set this parameter value to <code>true</code>, in
     * which case the file will simply be overwritten.  Use this feature at your own risk.
     *
     * @since 1.4.0
     */
    @Parameter(property="skipOverwriteProtection", defaultValue = "false")
    private boolean skipOverwriteProtection;

    private UsernamePasswordCredentials credentials;
    private Configuration freemarkerConfig;
    private Proxy proxy;

    private HttpHost host;
    private HttpRequestManager httpWorker;

    public ImportMojo() {

        // we'll use a freemarker template to construct the request for project metadata
        ClassTemplateLoader ctl = new ClassTemplateLoader(getClass(), "");
        freemarkerConfig = new Configuration(Configuration.VERSION_2_3_28);
        freemarkerConfig.setTemplateLoader(ctl);
        freemarkerConfig.setDefaultEncoding("UTF-8");
        freemarkerConfig.setLogTemplateExceptions(false);
        freemarkerConfig.setWrapUncheckedExceptions(true);
        freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    /**
     * After ensuring there are no local modifications (unless otherwise configured), downloads
     * and installs Reify project contents to the local project environment.
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        try {

            setHost(serverUrl);

            // derive filenames from project name
            zipFileName = projectName + ".proj.zip";
            projectFileName = projectName + ".proj.xml";

            // check to make sure there are no local changes, unless configured otherwise
            if (! skipOverwriteProtection) {
                checkForLocalModifications();
            }

            // allow explicit configuration from ImportTask (for Ant builds)
            if (credentials == null) {
                credentials = getCredentials(serverId);
            }
            if (credentials == null) {
                throw new MojoExecutionException("Reify credentials have not been configured.  Check Maven settings for the '" + serverId + "' server.");
            }

            // allow for manual construction of the Proxy (again, for Ant builds)
            httpWorker = new HttpRequestManager(host, credentials, settings != null ? settings.getActiveProxy() : proxy);
            httpWorker.login();

            // get fresh project metadata and archive from the server
            Document project = downloadProjectDocument();
            File archive = downloadProjectArchive(project);

            getLog().info(String.format("Importing Reify project assets from download at '%s'...", archive.getCanonicalPath()));
            File unpacked = new File(workdir, projectName);
            ArchiveUtils.unzip(archive, unpacked);

            // update the new project file with checksums of file content for overwrite protection on future imports
            writeProjectFile(project, unpacked);

            // and copy the result to application source
            FileUtils.copyDirectory(unpacked, webappDir);

            // finally, if the user has for some reason decided they want us to mangle their source, do so
            if (modifyWelcomeFiles || drawOnWelcomeFiles) {
                modifyWelcomeFiles();
            }

            // and validate whatever live datasources exist against the newly imported mocks
            if (! skipValidationOnImport) {
                getLog().info("Validating MockDataSources against any working DataSources...");
                validate();
            }

        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Import Error", e);
        }  finally {
            try {
                httpWorker.logout();
            } catch (Exception ignore) {}
        }
    }

    /**
     * Compares mock DataSources to live, logging findings and throwing an Exception if and
     * when the {@link #validationFailureThreshold} is exceeded.
     */
    protected void validate() {

        try {

            ReifyDataSourceValidator validator = new ReifyDataSourceValidator(smartclientRuntimeDir.getCanonicalPath(), webappDir + "/" + dataSourcesDir);
            validator.setMockDataSourcesBasePath(webappDir + "/" + mockDataSourcesDir);

            List<ErrorReport> result = validator.verify();
            boolean fail = false;
            if (! result.isEmpty()) {
                for (ErrorReport report : result) {
                    for (String key : report.keySet()) {
                        List<ErrorMessage> errors = report.getErrors(key);

                        for (ErrorMessage msg : errors) {

                            String output = null;
                            if (key.equals(report.getDataSourceId())) {
                                output = String.format("%s: %s", report.getDataSourceId(), msg.getErrorString());
                            } else {
                                output = String.format("%s.%s: %s", report.getDataSourceId(), key, msg.getErrorString());
                            }

                            Severity severity = msg.getSeverity();
                            if (severity.compareTo(validationFailureThreshold) >= 0) {
                                fail = true;
                            }

                            if (severity == INFO) {
                                getLog().info(output);
                            }
                            if (msg.getSeverity() == WARN) {
                                getLog().warn(output);
                            }
                            if (msg.getSeverity() == ERROR) {
                                getLog().error(output);
                            }
                        }
                    }
                }
            } else {
                getLog().info("No validation errors were found.");
            }
            if (fail) {
                throw new MojoExecutionException("Mock DataSource validation failure.  Please refer to ReifyDataSourceValidator log output for detailed report.");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks for local modifications, as documented at {@link #skipOverwriteProtection}},
     * and fails when any local changes are detected.
     * <p>
     * Note that files are expected in the configured locations - screens in {@link #uiDir}
     * and datasources in {@link #mockDataSourcesDir} / {@link #dataSourcesDir}
     * subdirectories).
     *
     * @throws MojoExecutionException when any local changes are detected
     * @throws Exception on any other error
     */
    private void checkForLocalModifications() throws MojoExecutionException, Exception {

        File projectFile = new File(webappDir, projectFileDir + "/" + projectFileName);
        if (! projectFile.exists()) {
            //LOGGER.info("Project file does not exist at '{}', but will be created during import.", projectFile.getCanonicalPath());
            getLog().info(String.format("Project file does not exist at '%s', but will be created during import.", projectFile.getCanonicalPath()));
            return;
        }

        SAXReader reader = new SAXReader();
        Document document = reader.read(projectFile);

        String msg = "File '%s' appears to have been modified since the last import.  Aborting to provide an opprtunity to investigate.  \n" +
                "You can either revert your changes, delete the file, or remove its checksum from the project file to continue. \n" +
                "You can also disable the check for this and all other files using the skipOverwriteProtection parameter.";
        
        List<Node> screens = getScreenNodes(document);
        for (Node screen : screens) {
            String expected = screen.valueOf("checksum");
            if (expected.isEmpty()) {
                continue;
            }
            File file = getScreen(screen, webappDir);
            if (! file.exists()) {
                continue;
            }

            String actual = Files.asByteSource(file).hash(Hashing.sha256()).toString();
            if (!actual.equals(expected)) {
                getLog().debug(String.format("Checksum mismatch - Expected: '%s' Actual: '%s'", expected, actual));
                throw new MojoExecutionException(String.format(msg, file.getCanonicalPath()));
            }
        }
        List<Node> datasources = getDataSourceNodes(document);
        for (Node ds : datasources) {
            String expected = ds.valueOf("checksum");
            if (expected.isEmpty()) {
                continue;
            }
            File file = getDataSource(ds, webappDir);
            if (! file.exists()) {
                continue;
            }

            String actual = Files.asByteSource(file).hash(Hashing.sha256()).toString();
            if (!actual.equals(expected)) {
                getLog().debug(String.format("Checksum mismatch - Expected: '%s' Actual: '%s'", expected, actual));
                throw new MojoExecutionException(String.format(msg, file.getCanonicalPath()));
            }
        }
    }

    /**
     * Requests the named project's metadata from the reify server.
     *
     * @return Document containing the result.
     * @throws Exception when any error occurs
     */
    private Document downloadProjectDocument() throws Exception {

        getLog().info("Contacting server for Reify project metadata...");
        
        HttpGet request = new HttpGet("/isomorphic/RESTHandler/hostedProjects?fileName=" + URLEncoder.encode(projectName) + "&isc_dataFormat=xml");
        HttpResponse response = httpWorker.execute(request);

        String body = null;
        try {

            body = EntityUtils.toString(response.getEntity());
            Document metadata =  DocumentHelper.parseText(body);

            Node fileContents = metadata.selectSingleNode("//fileContents");
            String project = fileContents.getText();

            return DocumentHelper.parseText(project);

        } catch (Exception e) {

            getLog().error("Response from server:" + System.getProperty("line.separator") + body);
            throw new Exception(String.format("Unexpected response to request for project file.  Check that user '%s' is able to access the project named '%s'",
                    credentials.getUserName(), projectName));
        }
    }

    private boolean isSmartGWTBuild() {
        // SmartGWT builds have their modules directory directly under $smartclientRuntimeDir,
        // as opposed to SmartClient, which expects it at $smartclientRuntimeDir/system/modules
        return FileUtils.getFile(smartclientRuntimeDir, "modules").exists();
    }

    private String getIsomorphicDirPath(String relativeToPath) {

        String[] parentDirectories = relativeToPath.split("/");
        int parentDirectoryCount = parentDirectories == null ? 0 : parentDirectories.length;
        String prefix = "";
        for (int i=1; i < parentDirectoryCount; i++) {
            prefix = "../" + prefix;
        }

        String name = null;
        // e.g., myapplication/sc vs isomorphic
        if (isSmartGWTBuild()) {
            name = smartclientRuntimeDir.getParentFile().getName() + "/" + smartclientRuntimeDir.getName();
        } else {
            name = smartclientRuntimeDir.getName();
        }
        return prefix + name;
    }

    /**
     * Requests the named project's export archive, including all screens and all datasources,
     * downloads it, and saves it to {@link #workdir}.
     *
     * @param project metadata used to provide the names of requested assets
     * @return zip file containing the export result
     * @throws Exception when any error occurs
     */
    private File downloadProjectArchive(Document project) throws Exception {
        
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("projectName", projectName);
        context.put("datasourcesDir", dataSourcesDir);
        context.put("mockDatasourcesDir", mockDataSourcesDir);
        context.put("uiDir", uiDir);
        context.put("projectFileDir", projectFileDir);
        context.put("projectFileName", projectFileName);
        context.put("zipFileName", zipFileName);

        context.put("screens", getScreenNames(project));
        context.put("datasources", getDataSourceNames(project));

        context.put("includeTestJsp",  includeTestJsp);
        context.put("testJspPathname", testJspPathname);
        StringWriter jspWriter  = new StringWriter();
        freemarkerConfig.getTemplate("TestJspFileContent.ftl").process(context, jspWriter);
        context.put("jspFileContent", StringEscapeUtils.escapeHtml4(jspWriter.toString()));

        context.put("includeTestHtml",  includeTestHtml);
        context.put("testHtmlPathname", testHtmlPathname);
        StringWriter htmlWriter  = new StringWriter();
        context.put("isomorphicDir", getIsomorphicDirPath(testHtmlPathname));
        context.put("modulesDir", isSmartGWTBuild() ? "modules" : "system/modules");
        freemarkerConfig.getTemplate("TestHtmlFileContent.ftl").process(context, htmlWriter);
        context.put("htmlFileContent", StringEscapeUtils.escapeHtml4(htmlWriter.toString()));

        StringWriter messageWriter = new StringWriter();
        freemarkerConfig.getTemplate("ProjectExportRequestParameter.ftl").process(context, messageWriter);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("_transaction", messageWriter.toString()));

        HttpPost request = new HttpPost("/isomorphic/IDACall?isc_rpc=1");
        request.setEntity(new UrlEncodedFormEntity(params));

        getLog().info(String.format("Downloading '%s' project export from '%s'...", projectName, host.getHostName()));

        HttpResponse response = httpWorker.execute(request);
        HttpEntity entity = response.getEntity();

        if (! "application/zip".equals(entity.getContentType().getValue())) {
            throw new MojoExecutionException("Response does not contain zip file contents:\n" + EntityUtils.toString(entity));
        }

        FileUtils.forceMkdir(workdir);
        FileUtils.cleanDirectory(workdir);
        File zip = new File(workdir + "/" + zipFileName);

        OutputStream outputStream = null;
        try {
            outputStream = new LoggingCountingOutputStream(new FileOutputStream(zip), entity.getContentLength());
            entity.writeTo(outputStream);
        } catch (Exception e) {
            throw new MojoExecutionException("Error writing file to '" + zip.getAbsolutePath() + "'", e);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }

        return zip;
    }

    /**
     * Calculates checksums for every file in the downloaded archive, writes them to
     * the given project file, and stores the result on disk at a folder named for
     * {@link #projectFileDir}, relative to the given parent.
     *
     * @param project metadata used to provide the names of requested assets
     * @param parent the parent directory
     * @throws Exception whenany error occurs
     */
    private void writeProjectFile(Document project, File parent) throws Exception {

        for (Node screen : getScreenNodes(project)) {

            File file = getScreen(screen, parent);
            String checksum = Files.asByteSource(file).hash(Hashing.sha256()).toString();

            Element element = ((Element) screen).addElement("checksum");
            element.setText(checksum);
        }
        for (Node datasource : getDataSourceNodes(project)) {

            File file = getDataSource(datasource, parent);
            String checksum = Files.asByteSource(file).hash(Hashing.sha256()).toString();

            Element element = ((Element) datasource).addElement("checksum");
            element.setText(checksum);
        }

        // pretty print the file with changes
        File projectFile = FileUtils.getFile(parent, projectFileDir, projectFileName);
        OutputFormat purdy = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter(new FileWriter(projectFile), purdy);

        writer.write(project);
        writer.flush();
        writer.close();
    }

    private void modifyWelcomeFiles() throws Exception {

        File welcomeFilesConfig = new File(webappDir, "WEB-INF/web.xml");
        if (! welcomeFilesConfig.exists()) {
            String msg = String.format("No web.xml found at '%s'", welcomeFilesConfig.getCanonicalPath());
            throw new RuntimeException(msg);
        }

        List<File> welcomeFiles = new ArrayList<File>();

        SAXReader reader = new SAXReader();
        Document webXml = reader.read(welcomeFilesConfig);
        
        XPath xpath = webXml.createXPath("//jee:welcome-file");
        xpath.setNamespaceURIs(ImmutableMap.of("jee", "http://java.sun.com/xml/ns/javaee"));
        List<Node> list = xpath.selectNodes(webXml);
        for (Node node : list) {
            String val = node.getStringValue();
            File file = FileUtils.getFile(webappDir, val);
            if (! file.exists()) {
                getLog().warn(String.format("Welcome file '{}' not found.  Skipping.", val));
                continue;
            }
            welcomeFiles.add(file);
        }
        if (welcomeFiles.isEmpty()) {
            File jsp = FileUtils.getFile(webappDir, "index.jsp");
            File html = FileUtils.getFile(webappDir, "index.html");
            if (jsp.exists()) {
                welcomeFiles.add(jsp);
            }
            if (html.exists()) {
                welcomeFiles.add(html);
            }
        }
        
        for (File file : welcomeFiles) {

            String extension = FilenameUtils.getExtension(file.getCanonicalPath()).toLowerCase();

            Source source=new Source(file);
            OutputDocument target=new OutputDocument(source);

            net.htmlparser.jericho.Element scriptElement =
                    source.getElementById("isc-maven-plugin.reify-import.modifyWelcomeFiles");

            Set<String> projectNames = new LinkedHashSet<String>();
            if (scriptElement != null) {
                if ("jsp".equals(extension)) {

                    String html = scriptElement.toString().trim();
                    int start = html.toLowerCase().indexOf("name=\"") + 6;
                    int end = html.indexOf("\"", start);
                    String val = html.substring(start, end);

                    projectNames.addAll(Sets.newHashSet(val.split(",")));

                } else {

                    String src = scriptElement.getAttributeValue("src");
                    int start = src.indexOf("projectName=") + 12;
                    int end = src.indexOf("&", start);
                    String val = src.substring(start, end);

                    projectNames.addAll(Sets.newHashSet(val.split(",")));
                }
            }
            projectNames.add(projectName);

            String template = "LoadProjectScriptFragment.ftl";
            if ("jsp".equals(extension)) {
                template = "LoadProjectTagFragment.ftl";
            }

            Map context = ImmutableMap.of(
                    "isomorphicDir", getIsomorphicDirPath(testHtmlPathname),
                    "projects", projectNames.toArray(),
                    "draw", drawOnWelcomeFiles);
            StringWriter tagWriter = new StringWriter();
            freemarkerConfig.getTemplate(template).process(context, tagWriter);

            if (scriptElement != null) {
                target.replace(scriptElement, tagWriter.toString());
            } else {

                // for now just drop the script at the end of the body
                int index = source.getFirstElement("body").getEndTag().getBegin();
                target.insert(index, tagWriter.toString());
            }

            FileUtils.write(file, target.toString());
        }
    }

    /*
     * Convenience methods for returning various representations of screens from xml document
     */
    private List<Node> getScreenNodes(Document project) {
        return project.selectNodes("/Project/screens/root/children/TreeNode");
    }
    private List<String> getScreenNames(Document project) {
        List<Node> nodes = getScreenNodes(project);
        List<String> names = new ArrayList<String>();
        for (Node node : nodes) {
            names.add(getScreenName(node));
        }
        return names;
    }
    private String getScreenName(Node screen) {
        return screen.valueOf("fileName");
    }
    private File getScreen(Node metadata, File parent) {
        return FileUtils.getFile(parent, uiDir, getScreenName(metadata) + ".ui.xml");
    }

    /*
     * Convenience methods for returning various representations of datasources from xml doc
     */
    private List<Node> getDataSourceNodes(Document project) {
        return project.selectNodes("/Project/datasources/Record");
    }
    private List<String> getDataSourceNames(Document project) {
        List<Node> nodes = getDataSourceNodes(project);
        List<String> names = new ArrayList<String>();
        for (Node node : nodes) {
            names.add(getDataSourceName(node));
        }
        return names;
    }
    private String getDataSourceName(Node ds) {
        return ds.valueOf("dsName");
    }
    private File getDataSource(Node metadata, File parent) {

        String name = getDataSourceName(metadata);

        // Check 'mock' subdirectory first, fallback to datasourcesDir if not found.
        File file = FileUtils.getFile(parent, mockDataSourcesDir, name + ".ds.xml");
        if (! file.exists()) {
            file = FileUtils.getFile(parent, dataSourcesDir, name + ".ds.xml");
        }
        return file;
    }

    /*
     * Setters to allow execution from an Ant build (ImportTask)
     */
    protected void setHost(String uri) {
        this.host = URIUtils.extractHost(URI.create(uri));
    }

    protected void setWorkdir(File workdir) {
        this.workdir = workdir;
    }

    protected void setWebappDir(File webappDir) {
        this.webappDir = webappDir;
    }

    protected void setSmartclientRuntimeDir(File smartclientRuntimeDir) {
        this.smartclientRuntimeDir = smartclientRuntimeDir;
    }

    protected void setIncludeTestJsp(boolean includeTestJsp) {
        this.includeTestJsp = includeTestJsp;
    }

    protected void setTestJspPathname(String testJspPathname) {
        this.testJspPathname = testJspPathname;
    }

    protected void setIncludeTestHtml(boolean includeTestHtml) {
        this.includeTestHtml = includeTestHtml;
    }

    protected void setTestHtmlPathname(String testHtmlPathname) {
        this.testHtmlPathname = testHtmlPathname;
    }

    protected void setDataSourcesDir(String dataSourcesDir) {
        this.dataSourcesDir = dataSourcesDir;
    }

    protected void setMockDataSourcesDir(String mockDataSourcesDir) {
        this.mockDataSourcesDir = mockDataSourcesDir;
    }

    protected void setSkipValidationOnImport(boolean skipValidationOnImport) {
        this.skipValidationOnImport = skipValidationOnImport;
    }

    protected void setValidationFailureThreshold(Severity validationFailureThreshold) {
        this.validationFailureThreshold = validationFailureThreshold;
    }

    protected void setUiDir(String uiDir) {
        this.uiDir = uiDir;
    }

    protected void setProjectFileDir(String projectFileDir) {
        this.projectFileDir = projectFileDir;
    }

    protected void setModifyWelcomeFiles(boolean modifyWelcomeFiles) {
        this.modifyWelcomeFiles = modifyWelcomeFiles;
    }

    protected void setDrawOnWelcomeFiles(boolean drawOnWelcomeFiles) {
        this.drawOnWelcomeFiles = drawOnWelcomeFiles;
    }

    protected void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    protected void setProjectFileName(String projectFileName) {
        this.projectFileName = projectFileName;
    }

    protected void setZipFileName(String zipFileName) {
        this.zipFileName = zipFileName;
    }

    protected void setSkipOverwriteProtection(boolean skipOverwriteProtection) {
        this.skipOverwriteProtection = skipOverwriteProtection;
    }

    protected void setCredentials(UsernamePasswordCredentials credentials) {
        this.credentials = credentials;
    }

    protected void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }




}