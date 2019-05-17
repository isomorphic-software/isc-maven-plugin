package com.isomorphic.maven.mojo.reify;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.isomorphic.maven.mojo.AbstractBaseMojo;
import com.isomorphic.maven.util.ArchiveUtils;
import com.isomorphic.maven.util.HttpRequestManager;
import com.isomorphic.maven.util.LoggingCountingOutputStream;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

/**
 * Provides for single-step download and extraction of assets hosted on the Reify platform.
 * While there is nothing to prevent a user from modifying the imported resources, changes 
 * should almost always be made using Reify and then re-imported.
 * <p>
 * If necessary, use the API (e.g., RPCManager.createScreen, Canvas.getByLocalId) to modify 
 * imported screen definitions at runtime.
 * <p>
 * Imported MockDataSources can be replaced using e.g., SQLDataSources having the same ID in
 * another directory - there is no reason to modify the contents of the 'mock' subdirectory.
 * <p> 
 * To encourage recommended usage, the reify-import goal takes steps to detect local changes 
 * and fail when any are found.  Refer to the {@link #skipOverwriteProtection}
 * parameter for details.
 */
// Set requiresProject: false so that we can run the whole thing from an Ant project w/ no POM
@Mojo(name="reify-import", requiresProject=false)
public class ImportMojo extends AbstractBaseMojo {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImportMojo.class);

    /**
     * The id of a <a href="http://maven.apache.org/settings.html#Servers">server
     * configuration</a> containing authentication credentials for the
     * reify.com website, used to download exported projects.
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
    private File workdir;
    
    /**
     * The directory containing web application sources.
     * 
     * @since 1.4.0
     */
    @Parameter(property = "webappDir", defaultValue = "${project.basedir}/src/main/webapp")
    private File webappDir;

    /**
     * The directory to which exported DataSources should ultimately reside, relative to {@link ImportMojo#webappDir}.  
     * Note that this will need to conform to the webapp's server.properties <code>project.datasources</code> configuration.
     * Also note that MockDataSources will be imported to a 'mock' subdirectory.  e.g., WEB-INF/ds/mock.
     * 
     * @since 1.4.0 
     */
    @Parameter(property = "datasourcesDir", defaultValue = "WEB-INF/ds")
    private String datasourcesDir;
    
    /**
     * The directory to which exported screens should ultimately reside, relative to {@link ImportMojo#webappDir}. 
     * Note that this will need to conform to the webapp's server.properties <code>project.ui</code> configuration.
     * 
     * @since 1.4.0 
     */
    @Parameter(property = "uiDir", defaultValue = "WEB-INF/ui")
    private String uiDir;
    
    /**
     * The directory to which the exported project file should ultimately reside, relative to {@link ImportMojo#webappDir}.
     * Note that this will need to conform to the webapp's server.properties <code>project.project</code> configuration. 
     * 
     * @since 1.4.0 
     */
    @Parameter(property="projectFileDir", defaultValue = "WEB-INF/ui")
    private String projectFileDir;
    
    /**
     * The name of the project as its known by the reify.com environment.
     */
    @Parameter(property="projectName", defaultValue = "${project.artifactId}", required=true)
    private String projectName;
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
     */
    @Parameter(property="skipOverwriteProtection", defaultValue = "false")
    private boolean skipOverwriteProtection;
    
    private UsernamePasswordCredentials credentials;
	private Configuration freemarkerConfig;
    private Proxy proxy;
    
	private HttpHost host;
	private HttpRequestManager httpWorker;
	
	public ImportMojo() {

		// undocumented parameter to allow for testing against QA environment
		String hostname = System.getProperty("reify-hostname");
		if (hostname == null) {
			hostname = "app.reify.com";
		}
		host = new HttpHost(hostname, -1, "https");

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

			LOGGER.info("Importing Reify project assets from download at '{}'...", archive.getCanonicalPath());
			File unpacked = new File(workdir, projectName);
			ArchiveUtils.unzip(archive, unpacked);

			// update the new project file with checksums of file content for overwrite protection on future imports
			writeProjectFile(project, unpacked);

			// and copy the result to application source
			FileUtils.copyDirectory(unpacked, webappDir);
			
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
	 * Checks for local modifications, as documented at {@link #skipOverwriteProtection}},
	 * and fails when any local changes are detected.
	 * <p>
	 * Note that files are expected in the configured locations - screens in {@link #uiDir}
	 * and datasources in {@link #datasourcesDir} (or the {@link #datasourcesDir}/mock 
	 * subdirectory).
	 * 
 	 * @throws MojoExecutionException when any local changes are detected
 	 * @throws Exception on any other error
	 */
	private void checkForLocalModifications() throws MojoExecutionException, Exception {
		
		File projectFile = new File(webappDir, projectFileDir + "/" + projectFileName);
		if (! projectFile.exists()) {
			LOGGER.info("Project file does not exist at '{}', but will be created during import.", projectFile.getCanonicalPath());
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
        		LOGGER.debug("Checksum mismatch - Expected: '{}' Actual: '{}'", expected, actual);
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
        		LOGGER.debug("Checksum mismatch - Expected: '{}' Actual: '{}'", expected, actual);
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
		
        LOGGER.info("Contacting server for Reify project metadata...");
        
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
        	
    		LOGGER.error("Response from server:" + System.getProperty("line.separator") + body);
    		throw new Exception(String.format("Unexpected response to request for project file.  Check that user '%s' is able to access the project named '%s'", 
    				credentials.getUserName(), projectName));
		}
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
    	
    	context.put("datasourcesDir", datasourcesDir);
    	context.put("uiDir", uiDir);
    	context.put("projectFileDir", projectFileDir);
    	context.put("projectFileName", projectFileName);
    	context.put("zipFileName", zipFileName);
    	
		context.put("screens", getScreenNames(project));
		context.put("datasources", getDataSourceNames(project));

    	StringWriter writer = new StringWriter();
		freemarkerConfig.getTemplate("transaction.ftl").process(context, writer);

		List<NameValuePair> params = new ArrayList<NameValuePair>();
	    params.add(new BasicNameValuePair("_transaction", writer.toString()));
	    
		HttpPost request = new HttpPost("/isomorphic/IDACall?isc_rpc=1");
	    request.setEntity(new UrlEncodedFormEntity(params));
		
        LOGGER.info("Downloading '{}' project export from '{}'...", projectName, host.getHostName());
	    
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
		File file = FileUtils.getFile(parent, datasourcesDir, "mock", name + ".ds.xml");
    	if (! file.exists()) {
        	file = FileUtils.getFile(parent, datasourcesDir, name + ".ds.xml");
    	}
		return file;
	}	
	
	/*
	 * Setters to allow execution from an Ant build (ImportTask)
	 */
	protected void setWorkdir(File workdir) {
		this.workdir = workdir;
	}

	protected void setWebappDir(File webappDir) {
		this.webappDir = webappDir;
	}

	protected void setDatasourcesDir(String datasourcesDir) {
		this.datasourcesDir = datasourcesDir;
	}

	protected void setUiDir(String uiDir) {
		this.uiDir = uiDir;
	}

	protected void setProjectFileDir(String projectFileDir) {
		this.projectFileDir = projectFileDir;
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

	protected void setCredentials(UsernamePasswordCredentials credentials) {
		this.credentials = credentials;
	}

	protected void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}
	
}