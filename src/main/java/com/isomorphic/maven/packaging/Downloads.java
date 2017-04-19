package com.isomorphic.maven.packaging;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Proxy;
import org.codehaus.plexus.util.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.isomorphic.maven.util.LoggingCountingOutputStream;

/**
 * Connects to Isomorphic site, discovers which files exist for a given build, and downloads them 
 * to local file system. 
 */
public class Downloads {

	private static final Logger LOGGER = LoggerFactory.getLogger(Downloads.class);

	private static final String DOMAIN = "www.smartclient.com";
	private static final String LOGIN_URL = "/devlogin/login.jsp";
	private static final String LOGOUT_URL = "/logout.jsp";
	
	private File toFolder = new File(System.getProperty("java.io.tmpdir"));
	private Boolean overwriteExistingFiles = Boolean.FALSE;
	private Proxy proxyConfiguration;
	
	private UsernamePasswordCredentials credentials;
		
	private DefaultHttpClient httpClient = new DefaultHttpClient();
	private HttpHost host = new HttpHost(DOMAIN);
	
	/**
	 * Constructor taking the credentials needed for authentication on smartclient.com. 
	 * 
	 * @param credentials The credentials needed for authentication on smartclient.com.
	 */
	public Downloads(UsernamePasswordCredentials credentials) {
		this.credentials = credentials;
	}
	
	/**
	 * Set the proxy configuration, if any, needed to support network operations from behind
	 * a proxy.
	 * 
	 * @param proxyConfiguration the proxy configuration, if any, needed to support network operations from behind a proxy
	 * @see http://maven.apache.org/guides/mini/guide-proxies.html
	 */
	public void setProxyConfiguration(Proxy proxyConfiguration) {
		this.proxyConfiguration = proxyConfiguration;
	}

	/**
	 * Sets the directory to which the distribution/s should be downloaded.
	 * Defaults to the system property <code>java.io.tmpdir</code>.
	 * 
	 * @param toFolder The directory to which the distribution/s should be downloaded.
	 */
	public void setToFolder(File toFolder) {
		this.toFolder = toFolder;
	}
	
	/**
	 * If true, downloads files whether they already exist locally or not.  Skips
	 * the download otherwise.  Defaults to false. 
	 * 
	 * @param overwriteExistingFiles
	 */
	public void setOverwriteExistingFiles(Boolean overwriteExistingFiles) {
		this.overwriteExistingFiles = overwriteExistingFiles;
	}
	
	/**
	 * Retrieves a {@link Distribution} instance for each of the given licenses, downloads
	 * files if necessary, and {@link Distribution#getFiles() links} the local file to the distribution.
	 * 
	 * @param product The product built and distributed by Isomorphic Software.  e.g., SmartCLient 
	 * @param buildNumber The build number of the desired distribution.  e.g., 4.1d
	 * @param buildDate The date on which the distribution was made available
	 * @param licenses The licenses, or editions, that the product is released under, and for which the user is registered
	 * @return A collection of Distributions, each having its contents resolved to local files, and suitable for use in repacking operations
	 * @throws MojoExecutionException on any error
	 * 
	 * @see http://www.smartclient.com/builds/
	 */
	public List<Distribution> fetch(Product product, String buildNumber, String buildDate, License...licenses) throws MojoExecutionException {

		try {
			setup();
			login();
			
			List<Distribution> result = new ArrayList<Distribution>();
			
			for (License license : licenses) {
				Distribution distribution = Distribution.get(product, license);
				download(distribution, buildNumber, buildDate);
				result.add(distribution);
			}
			logout();

			return result;
			
		} finally {
	        httpClient.getConnectionManager().shutdown();
	    }
	}

	public String findCurrentBuild(Distribution distribution, String buildNumber) throws MojoExecutionException {

        try {
            setup();
            login();
            
            String url = distribution.getRemoteIndex(buildNumber, null);
            String selector = "a[href~=[0-9]{4}-[0-9]{2}-[0-9]{2}]";
            
            String[] links = list(url, selector);
            
            logout();

            if (links.length > 0) {
                return links[0];
            } else {
                return null;
            }
            
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }
	
	/**
	 * Obtains a list of hyperlinks, downloads the file/s represented by each, and links it/them to the given distribution.
	 * 
	 * @param distribution
	 * @throws MojoExecutionException
	 */
	private void download (Distribution distribution, String buildNumber, String buildDate) throws MojoExecutionException {

	    String url = distribution.getRemoteIndex(buildNumber, buildDate);
        String selector = distribution.getRemoteIndexFilter();
		
        String[] links = list(url, selector);
        
		for (String link : links) {
		
			String filename = FilenameUtils.getName(link);
			File file = new File(toFolder, filename);

			if (file.exists() && !overwriteExistingFiles) {
				LOGGER.info("Existing archive found at '{}'.  Skipping download.", file.getAbsolutePath());
				distribution.getFiles().add(file);
				continue;
			}

			HttpGet httpget = new HttpGet(link);
			HttpResponse response;
			
			try {			
				response = httpClient.execute(host, httpget);
			} catch (Exception e) {
				throw new MojoExecutionException("Error issuing GET request for bundle at '" + httpget + "'", e);
			}

			HttpEntity entity = response.getEntity();
			
			if(!toFolder.mkdirs() && !toFolder.exists()) {
				throw new MojoExecutionException("Could not create specified working directory '" + toFolder.getAbsolutePath() + "'");
	        }
			
			FileUtils.deleteQuietly(file);

            OutputStream outputStream = null;

            try {
                LOGGER.info("Downloading file to '{}'", file.getAbsolutePath());
                outputStream = new LoggingCountingOutputStream(new FileOutputStream(file), entity.getContentLength());
                entity.writeTo(outputStream);
                distribution.getFiles().add(file);
            } catch (Exception e) {
                throw new MojoExecutionException("Error writing file to '" + file.getAbsolutePath() + "'", e);
            } finally {
                IOUtils.closeQuietly(outputStream);
            }
		}
	}
	
	/**
	 * Interrogates the remote server for a list of hyperlinks matching the given distribution's {@link Distribution#getRemoteIndexFilter() filter}.
	 * 
	 * @param dist the build in which some files should exist
	 * @return a String array of html href attributes
	 * @throws MojoExecutionException
	 */
	private String[] list(String url, String selector) throws MojoExecutionException {
		
		HttpGet request = new HttpGet(url);
		HttpResponse response;
		
		try {
		
			LOGGER.debug("Requesting list of files from {}{}", DOMAIN, url);
			response = httpClient.execute(host, request);
		
		} catch (Exception e) {
			throw new MojoExecutionException("Error issuing GET request for bundle at '" + request + "'", e);
		}
		
		Document doc;

		try {
		
			String html = EntityUtils.toString(response.getEntity());
			doc = Jsoup.parse(html);
			doc.outputSettings().prettyPrint(true);
		
		} catch (Exception e) {
			throw new MojoExecutionException("Error processing response from '" + request + "'", e);
		}

		List<String> result = new ArrayList<String>(); 
		
		Elements links = doc.select(selector);

		for (Element element : links) {
			String href = element.attr("href");
			result.add(href);
		}
		
		if (result.isEmpty()) {
			String msg = String.format("No downloads found at '%s%s'.  Response from server: \n\n%s\n", DOMAIN, url, doc.html());
			LOGGER.warn(msg);
		}
		
		return result.toArray(new String[0]);
	}
	
	/**
	 * If {@link #credentials} have been supplied, uses them to autthenticate to the isomorphic web site,
	 * allowing download of protected resources.
	 * 
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private void login() throws MojoExecutionException {

		if (credentials == null) {
			return;
		}
		
		String username = credentials.getUserName();
		String password = credentials.getPassword();
		
		LOGGER.debug("Authenticating to '{}' with username: '{}'", DOMAIN + LOGIN_URL, username);
		
        HttpPost login = new HttpPost(LOGIN_URL);

        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("USERNAME", username));
        nvps.add(new BasicNameValuePair("PASSWORD", password));

    	try {
			login.setEntity(new UrlEncodedFormEntity(nvps));
			HttpResponse response = httpClient.execute(host, login);
			EntityUtils.consume(response.getEntity());
		} catch (IOException e) {
			throw new MojoExecutionException("Error during POST request for authentication", e);
		}
	}
	
	/**
	 * Logs off at smartclient.com.
	 *  
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private void logout() {
        HttpPost logout = new HttpPost(LOGOUT_URL);
        LOGGER.debug("Logging off at '{}'", DOMAIN + LOGOUT_URL);
        try {
	        HttpResponse response = httpClient.execute(host, logout);
	        EntityUtils.consume(response.getEntity());
		} catch (Exception e) {
			LOGGER.warn("Error at logout ", e);
		}	
	}
	
	/**
	 * Configures the {@link #httpClient}, specifically with the current {@link #proxyConfiguration}. 
	 * 
	 * @throws MojoExecutionException
	 */
	private void setup() throws MojoExecutionException {
		try {
			if (proxyConfiguration != null && isProxied(proxyConfiguration) ) {
				if (proxyConfiguration.getUsername() != null) {
					httpClient.getCredentialsProvider().setCredentials(
						new AuthScope(proxyConfiguration.getHost(), proxyConfiguration.getPort()),
						new UsernamePasswordCredentials(proxyConfiguration.getUsername(), proxyConfiguration.getPassword()));	
				}
				HttpHost proxy = new HttpHost(proxyConfiguration.getHost(), proxyConfiguration.getPort());
				httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
			}
		} catch (Exception e) {
			throw new MojoExecutionException("Error obtaining Maven settings", e);
		}
	}
	
	/**
	 * Adapted from the Site plugin's AbstractDeployMojo to allow http operations through proxy.
	 * 
	 * @see http://maven.apache.org/guides/mini/guide-proxies.html
	 * @see http://maven.apache.org/plugins/maven-site-plugin/xref/org/apache/maven/plugins/site/AbstractDeployMojo.html.
	 */
	private boolean isProxied(Proxy proxyConfig) throws MalformedURLException {
	    String nonProxyHostsAsString = proxyConfig.getNonProxyHosts();

		for (String nonProxyHost : StringUtils.split(nonProxyHostsAsString, ",;|")) {
			
			if (StringUtils.contains(nonProxyHost, "*")) {
				
				// Handle wildcard at the end, beginning or middle of the nonProxyHost
				final int pos = nonProxyHost.indexOf('*');
				String nonProxyHostPrefix = nonProxyHost.substring(0, pos);
				String nonProxyHostSuffix = nonProxyHost.substring(pos + 1);

				// prefix*
				if (StringUtils.isNotEmpty(nonProxyHostPrefix)
						&& DOMAIN.startsWith(nonProxyHostPrefix)
						&& StringUtils.isEmpty(nonProxyHostSuffix)) {

					return false;
				}
				
				// *suffix
				if (StringUtils.isEmpty(nonProxyHostPrefix)
						&& StringUtils.isNotEmpty(nonProxyHostSuffix)
						&& DOMAIN.endsWith(nonProxyHostSuffix)) {
					
					return false;
				}
				
				// prefix*suffix
				if (StringUtils.isNotEmpty(nonProxyHostPrefix)
						&& DOMAIN.startsWith(nonProxyHostPrefix)
						&& StringUtils.isNotEmpty(nonProxyHostSuffix)
						&& DOMAIN.endsWith(nonProxyHostSuffix)) {
					
					return false;
				}

			} else if (DOMAIN.equals(nonProxyHost)) {
				return false;
			}
		}
	    return true;
	}

}