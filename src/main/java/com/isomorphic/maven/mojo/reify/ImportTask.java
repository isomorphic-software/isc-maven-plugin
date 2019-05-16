package com.isomorphic.maven.mojo.reify;

import java.io.File;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.maven.settings.Proxy;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An Ant task allowing the Reify Import Mojo to be run from Ant builds.
 */
public class ImportTask extends Task {


	private static final Logger LOGGER = LoggerFactory.getLogger(ImportTask.class);
	
	private String datasourcesDir = "WEB-INF/ds";
	private boolean includeProjectFile = true;
	private String password;
	private String projectFileDir = "WEB-INF/ui";
	private String projectFileName;
	private String projectName;
	private String uiDir = "WEB-INF/ui";
	private String username;
	private String webappDir = "war";
	
	private String workdir = "build";
	private String zipFileName;

	@Override
	public void execute() throws BuildException {

		// Sadly, there doesn't seem to be any good way to get the Mojo's slf4j logger output into the Ant console.  
		log("Importing reify assets from server...");
		
		ImportMojo mojo = new ImportMojo();
		
		if (username == null || password == null) {
			throw new BuildException("Username and password parameters required.");
		}

		mojo.setCredentials(new UsernamePasswordCredentials(username, password));
		
		String name = projectName != null ? projectName : getProject().getName();
		
		mojo.setProjectName(name);
		mojo.setProjectFileName(projectFileName != null ? projectFileName : name + ".proj.xml");
		mojo.setProjectFileDir(projectFileDir);
		mojo.setUiDir(uiDir);
		mojo.setWebappDir(new File(getProject().getBaseDir(), webappDir));
		mojo.setZipFileName(zipFileName != null ? zipFileName : name + "proj.zip");
		mojo.setWorkdir(new File(getProject().getBaseDir(), workdir));

		mojo.setDatasourcesDir(datasourcesDir);
//		mojo.setIncludeProjectFile(includeProjectFile);
		
		//export ANT_OPTS="-Dhttp.proxyHost=myproxyhost -Dhttp.proxyPort=8080 -Dhttp.proxyUser=myproxyusername -Dhttp.proxyPassword=myproxypassword"
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
	
	/*
	 * Configuration property setters
	 */
	public void setDatasourcesDir(String datasourcesDir) {
		this.datasourcesDir = datasourcesDir;
	}

	public void setIncludeProjectFile(boolean includeProjectFile) {
		this.includeProjectFile = includeProjectFile;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setProjectFileDir(String projectFileDir) {
		this.projectFileDir = projectFileDir;
	}

	public void setProjectFileName(String projectFileName) {
		this.projectFileName = projectFileName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public void setUiDir(String uiDir) {
		this.uiDir = uiDir;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setWebappDir(String webappDir) {
		this.webappDir = webappDir;
	}

	public void setWorkdir(String workdir) {
		this.workdir = workdir;
	}

	public void setZipFileName(String zipFileName) {
		this.zipFileName = zipFileName;
	}	
	
}
