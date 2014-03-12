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

import static com.isomorphic.maven.packaging.License.ANALYTICS_MODULE;
import static com.isomorphic.maven.packaging.License.ENTERPRISE;
import static com.isomorphic.maven.packaging.License.EVAL;
import static com.isomorphic.maven.packaging.License.LGPL;
import static com.isomorphic.maven.packaging.License.MESSAGING_MODULE;
import static com.isomorphic.maven.packaging.License.POWER;
import static com.isomorphic.maven.packaging.License.PRO;
import static com.isomorphic.maven.packaging.Product.SMARTCLIENT;
import static com.isomorphic.maven.packaging.Product.SMARTGWT;
import static com.isomorphic.maven.packaging.Product.SMARTGWT_MOBILE;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Joiner;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.isomorphic.maven.util.AntPathMatcherFilter;
import com.isomorphic.maven.util.ArchiveUtils;

/**
 * Models both the location/filenames of remote SDK bundles and their contents.  Takes care to normalize product families into 
 * some common denominator
 */
public final class Distribution {

	private static final Logger LOGGER = LoggerFactory.getLogger(Distribution.class);

	//a default regex pattern used to find the files available for download
	private static final String LINK_SELECTOR = "(?i)\\.(zip|jar)";
	
	//ant-style wildcards used at extraction for filtering the distribution for non-javadoc documentation resources
	private static final String DOC_INCLUDES = "**/*.pdf";
	private static final String DOC_EXCLUDES = "**/apache-ant*/**";
	
	//ant-style wildcards used at extraction for filtering the distribution for the JARs to be included 
	private static final String JAR_INCLUDES = "**/isc-*.jar, **/isomorphic_*.jar, **/smartgwt-*.jar";
	private static final String JAR_EXCLUDES = "**/samples/**, **/*examples.jar, **/*tomcat.jar, **/*isomorphic_web_services.jar, **/isomorphic_applets.jar";
	
	//the following ant patterns currently yield files that are deliberately renamed (see static initialization block) - exclude them as well
	private static final String JAR_CONFLICTS = "**/smartgwtee.jar, **/isc-jakarta-oro*.jar, **/isomorphic_realtime_messaging.jar";		
	
	//ant-style wildcards used at extraction for filtering the distribution for the POMs to be included
	private static final String POM_SMARTCLIENT = "**/smartclient-*resources.pom, **/smartclient-tools.xml, **/smartclient-messaging.xml, **/smartclient-analytics.xml";
	private static final String POM_SMARTGWT = "**/smartgwt-skins.pom, **/smartgwt-analytics.pom, **/smartgwt-messaging.pom";
	private static final String POM_SERVER = "**/isomorphic-*.pom, **/isomorphic-*.xml, **/dependencygroup-*.xml";
	private static final String POM_SHARED = "**/isc-*.pom, **/isc-*.xml";

	//ant-style wildcards used at extraction for filtering the distribution for Selenium support resources
	private static final String SELENIUM_INCLUDES = "**/selenium/**, **/batchReport.template";

	private static final String SMARTCLIENT_RUNTIME_INCLUDES = "**/smartclientRuntime/isomorphic/**, **/smartclientRuntime/WEB-INF/classes/**, **/smartclientRuntime/WEB-INF/iscTaglib.xml";
	private static final String SMARTCLIENT_SDK_INCLUDES = "**/smartclientSDK/tools/**";
	private static final String SMARTCLIENT_SDK_EXCLUDES = "**/dsBrowser.jsp,**/classBrowser.jsp,**/sqlBrowser.jsp,**/maven/**";
	
	//ant-style wildcards used at extraction for filtering the distribution for javadoc documentation resources
	private static final String SMARTCLIENT_JAVADOC = "**/smartclientSDK/isomorphic/system/reference/server/javadoc/**";
	private static final String SMARTGWT_CLIENT_JAVADOC = "**/doc/javadoc/**";
	private static final String SMARTGWT_SERVER_JAVADOC = "**/doc/server/javadoc/**";

	private static final Table<Product, License, Distribution> DISTRIBUTIONS = HashBasedTable.create();
	
	/*
	 * Configure every possible Product/License combination and store the result in a table for lookup on demand.
	 * Note that smartgwt JARs are effectively renamed to take the form smartgwt-${license} to provide clarity 
	 * and to better facilitate POM matching.  
	 */
	static {	
		create(SMARTCLIENT, LGPL);
		create(SMARTCLIENT, EVAL);
		create(SMARTCLIENT, PRO);
		create(SMARTCLIENT, POWER);
		create(SMARTCLIENT, ENTERPRISE);
		create(SMARTCLIENT, ANALYTICS_MODULE);
		create(SMARTCLIENT, MESSAGING_MODULE);

		//the smartgwt lgpl edition provides links to a bunch of resources that are also contained in the .zip - ignore them
		create(SMARTGWT, LGPL)
			.include("smartgwt-.*\\.zip")
			.contents("lib/smartgwt-lgpl.jar", "**/smartgwt.jar" , null);
		
		//the smartgwt eval URL takes a different form than the smartclient eval URL, for some reason
		create(SMARTGWT, EVAL)
			.index("#license", "EnterpriseEval")
			.contents("lib/smartgwt-eval.jar", "**/smartgwtee.jar", null);
		
		create(SMARTGWT, PRO)
			.contents("lib/smartgwt-pro.jar", "**/smartgwtpro.jar", null);;
		
		create(SMARTGWT, POWER)
			.contents("lib/smartgwt-power.jar", "**/smartgwtpower.jar", null);;
		
		create(SMARTGWT, ENTERPRISE)
			.contents("lib/smartgwt-enterprise.jar", "**/smartgwtee.jar", null);

		create(SMARTGWT, ANALYTICS_MODULE);
		create(SMARTGWT, MESSAGING_MODULE);
		
		//mobile user documentation is not currently in pdf format, so the default pattern does not match  
		create(SMARTGWT_MOBILE, LGPL)
			.include("smartgwt-.*\\.zip")
			.contents("doc/user", "smartgwt-mobile*/user_guide.*", null);
	}

	/**
	 * Object creation method whose invocation has the same effect as calling {@link #create(Product, License, String...)}
	 * with {@link #LINK_SELECTOR} in the String argument.
     *
	 * @param product 
	 * @param license 
	 * @return A Distribution object having default configuration values applied
	 */
	private static Distribution create(Product product, License license) {
		return create(product, license, LINK_SELECTOR);
	}
	
	/**
	 * Object creation method meant to facilitate a 'fluent interface' style of configuration via a handful of private methods.
	 * Objects are created with a set of default property values and retruned for further configuration if so deired.  
	 * A reference to the return value is also available in the {@link #DISTRIBUTIONS} table under the given product/key combination.
	 * 
	 * @param product
	 * @param license
	 * @param links
	 * @return A Distribution object having default configuration values applied 
	 */
	private static Distribution create(Product product, License license, String... links) {
		
		Distribution distribution = new Distribution();
		distribution.include(links);

		//e.g., smartclient downloads currently include smartgwt poms and vice-versa.  ignore when inapplicable 
		List<String> pomIncludes = new ArrayList<String>();
		if (product == SMARTCLIENT) {
			pomIncludes.add(POM_SMARTCLIENT);
			distribution
				.contents("sdk/#smartclientSDK", "**/smartclientSDK/**", SMARTCLIENT_SDK_EXCLUDES)
				.contents("assembly/smartclient-resources/#smartclientRuntime", SMARTCLIENT_RUNTIME_INCLUDES, null)
				.contents("assembly/smartclient-analytics-resources/isomorphic/system/modules", "ISC_Analytics*,ISC_Drawing*", null)
				.contents("assembly/smartclient-analytics-resources/isomorphic/system/modules-debug", "modules-debug/ISC_Analytics*,modules-debug/ISC_Drawing*", null)
				.contents("assembly/smartclient-messaging-resources/isomorphic/system/modules", "ISC_RealtimeMessaging*", null)
				.contents("assembly/smartclient-messaging-resources/isomorphic/system/modules-debug", "modules-debug/ISC_RealtimeMessaging*", null)
				.contents("assembly/smartclient-tools-resources/#smartclientSDK", SMARTCLIENT_SDK_INCLUDES, SMARTCLIENT_SDK_EXCLUDES);
		} else if (product == SMARTGWT) {
			pomIncludes.add(POM_SMARTGWT);
		}
		pomIncludes.add("**/" + product.getName() + "-" + license.getName() + "*");
		pomIncludes.add(POM_SHARED);
		
		//similarly lgpl includes server framework poms.  ignore
		if (license != LGPL) {
			pomIncludes.add(POM_SERVER);
		}

		/*
		 * Map the relevant sdk resources to their paths as they should be when extracted.
		 * Note that some resources are renamed to provide clarity, facilitate POM matching,
		 * and/or adhere to conventions.   
		 */
		distribution
			.contents("pom", Joiner.on(",").join(pomIncludes), null)
			.contents("doc/user", DOC_INCLUDES, DOC_EXCLUDES)
			.contents("doc/api/client/#javadoc", SMARTGWT_CLIENT_JAVADOC, null)
			.contents("doc/api/server/#javadoc", product == SMARTGWT ? SMARTGWT_SERVER_JAVADOC : SMARTCLIENT_JAVADOC, null)
			.contents("lib", JAR_INCLUDES, JAR_EXCLUDES + ", " + JAR_CONFLICTS)
			.contents("lib/isc-jakarta-oro.jar", "**/isc-jakarta-oro*.jar", null)
			.contents("lib/smartgwt-analytics.jar", "**/analytics.jar", null)
			.contents("lib/smartgwt-messaging.jar", "**/messaging.jar", null)
			.contents("lib/isomorphic-messaging.jar", "**/isomorphic_realtime_messaging.jar", null)
			.contents("assembly/isc-selenium-resources", SELENIUM_INCLUDES, null);

		if (license == EVAL || license == POWER || license == ENTERPRISE) {
			distribution.contents("assembly/isc-batchuploader-resources/ds", "**/batchUpload.ds.xml", null);
		}
		
		DISTRIBUTIONS.put(product, license, distribution);
		return distribution;
	}
	
	/**
	 * Returns a fully-prepared Distribution instance representing an Isomorphic build, suitable for invoking download or repackaging 
	 * operations against SDK bundles and their contents.
	 * 
	 * @param product The propduct for which a distribution exists and has been previously configured
	 * @param license The license under which the product is distributed and with which to execute some repackaing operation
	 * @param version The version number of the Isomorphic build, as described at smartclient.com
	 * @param date The date on which the build was made available
	 * @return The fully-prepared Distribution instance, suitable for downloading and/or manipulating SDK bundles and their contents
	 * @throws IllegalArgumentException when no instance is found for the given Product, License combination
	 */
	public static Distribution get(Product product, License license, String version, String date) {
		Distribution result = DISTRIBUTIONS.get(product, license);
		if (result == null) {
			throw new IllegalArgumentException("Unknown distribution for product " + product + " and license " + license + ".");
		}
		//determine the location of the distribution by replacing url tokens with provided values 
		String url = result.getRemoteIndex()
			.replaceAll("#product", product.toString())
			.replaceAll("#version", version)
			.replaceAll("#license", license.toString())
			.replaceAll("#date", date);

		result.setRemoteIndex(url);
		return result;
	} 	
	
	//a string representing a relative url from which a given distribution may be downloaded 
	private String remoteIndex = "/builds/#product/#version/#license/#date/";	
	private List<String> selectors = new ArrayList<String>();
	private Map<String, AntPathMatcherFilter> content = new HashMap<String, AntPathMatcherFilter>();
	private Set<File> files = new HashSet<File>();
	
	/**
	 * Private constructor, in the singleton style.
	 */
	private Distribution() {
	}
	
	/**
	 * Returns the set of files that were (at one time or another) downloaded from smartclient.com, usually compressed .zip or .jar files.  
	 * Note that the set is necessarily empty at initialization - Some index, local or remote, must first be interrogated to find a list of 
	 * files matching {@link #include(String...) the patterns configured for this object}.  Any matched resources must then be resolved 
	 * to local files before having their references added to this collection.
	 * 
	 * <ul>
	 * 	<li>SmartClient_AnalyticsModule-2014-01-01.zip</li>
	 * 	<li>SmartClient_DrawingModule-2014-01-01.zip</li>
	 * </ul>
	 * 
	 * @return the Set of files making up the distribution.
	 * @see Downloads
	 */
	public Set<File> getFiles() {
		return files;
	}
	
	/**
	 * Adds an entry to the map of patterns used to determine which resources should be extracted from this distribution's collection of file/s.      
	 * 
	 * @param key A path to be used with any resource(s) that match the patterns provided.  Usually denotes a directory, but it can be used to specify a filename in cases where renaming is desirable.
	 * @param includes A comma-separated list of Ant-style patterns to be used to determine whether a given resource should be included in extraction / repackaging operations.
	 * @param excludes A comma-separated list of Ant-style patterns to be used to determine whether a given resource should be excluded from extraction / repackaging operations.
	 * @return This object, in 'fluid interface' style
	 * 
	 * @see AntPathMatcherFilter
	 */
	private Distribution contents(String key, String includes, String excludes) {
		content.put(key, new AntPathMatcherFilter(includes, excludes));
		return this;
	}
	
	/**
	 * Sets entries to the collection of regular expressions used to determine which hyperlinks should be used to download files from smartclient.com,
	 * clearing any expressions provided previously.
	 * <p/>
	 * Each of the expressions provided is used to construct selection criteria used by an html parser.  Example:
	 * <pre>
	 *  	include("smartgwt-.*\\.zip");
	 * </pre> 
	 * yields an expression like
	 * <pre>
	 * 		a[href~=smartgwt-.*\.zip]
	 * </pre>
	 * which will match links like the following:
	 * <pre>
	 * 		&lt;a href="/builds/SmartGWT/4.0p/LGPL/2014-01-08/smartgwt-4.0p.zip"&gt;smartgwt-4.0p.zip&lt;/a&gt;
	 * </pre>
	 * 
	 * @param links The regular expressions used to determine which hyperlinks should be used to download files from smartclient.com
	 * @return This object, in 'fluid interface' style
	 * @see http://jsoup.org/cookbook/extracting-data/selector-syntax
	 */
	private Distribution include(String... links) {
		selectors.clear();
		for (String pattern : links) {
			selectors.add("a[href~=" + pattern + "]");
		}
		return this;
	}
	
	/**
	 * Performs token replacement on the URL template representing the location of the distribution's "remote index", or download page.
	 * Useful at initialization in the case where some product/license combination uses an unconventional URL.
	 * 
	 * @param key the Key, including token, needing replacement.  e.g., #license
	 * @param replacement the value to use in the substitution.  e.g., EnterpriseEval
	 * @return This object, in 'fluid interface' style
	 */
	private Distribution index(String key, String replacement) {
		remoteIndex = remoteIndex.replaceAll(key, replacement);
		return this;
	}

	/** 
	 * Returns the relative URL representing the location of the distribution's "remote index", or download page.  e.g.,
	 * <p/>
	 * /builds/SmartGWT/4.1d/EnterpriseEval/2014-01-01
	 * 
	 * @return the URL representing the location of the distribution's "remote index", or download page.
	 */
	protected String getRemoteIndex() {
		return remoteIndex;
	}
	
	/**
	 * Sets url the URL representing the location of the distribution's "remote index", or download page.
	 * @param url the URL representing the location of the distribution's "remote index", or download page.
	 */
	private void setRemoteIndex(String url) {
		remoteIndex = url;
	}
	
	/**
	 * Returns a comma-separated list of all the {@link #selectors} that should be used to determine which 
	 * 		   hyperlinks should be used to download files from smartclient.com.
	 * @return a comma-separated list of all the {@link #selectors} that should be used to determine which 
	 * 		   hyperlinks should be used to download files from smartclient.com
	 */
	protected String getRemoteIndexFilter() {
		return Joiner.on(",").join(selectors);
	}

	/**
	 * Extract the relevant contents from each file in the distribution.  Additionally creates ZIP/JAR
	 * files from specified resources (e.g., javadoc).
	 * 
	 * @param to The directory to which each file should be extracted.
	 * @throws IOException
	 */
	public void unpack(File to) throws IOException {
		
		outer:
		for (File file : files) {
			
			String ext = FilenameUtils.getExtension(file.getName()).toUpperCase();
			
			//copy uncompressed files to target, renaming as necessary per 'contents' configuration
			if (! "ZIP".equals(ext)) {
				for(Map.Entry<String, AntPathMatcherFilter> filterEntry : content.entrySet()) {
					AntPathMatcherFilter filter = filterEntry.getValue();
					if (filter.accept(file.getName())) {
						File target = FileUtils.getFile(to, ArchiveUtils.rewritePath(file.getName(), filterEntry.getKey()));
						FileUtils.copyFile(file, target);
						LOGGER.debug("Copied file '{}' to file '{}'", file.getName(), target.getAbsolutePath());
						continue outer;
					}
				}
				FileUtils.copyFileToDirectory(file, new File(to, "lib"));
				continue outer;
			}

			//otherwise extract contents (again renaming / relocating contents as necessary)
			ZipFile zip = new ZipFile(file);
			Enumeration<? extends ZipEntry> entries = zip.entries();
			
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory()) {
					continue;
				}
				for(Map.Entry<String, AntPathMatcherFilter> filterEntry : content.entrySet()) {
					AntPathMatcherFilter filter = filterEntry.getValue();
					if (filter.accept(entry.getName())) {
						File target = FileUtils.getFile(to, ArchiveUtils.rewritePath(entry.getName(), filterEntry.getKey()));
						FileUtils.copyInputStreamToFile(zip.getInputStream(entry), target);
						LOGGER.debug("Copied input stream to file '{}'", target.getAbsolutePath());
					}	
				}
			}
			zip.close();
		}
	
		/*
		 * Create any number of assemblies by dropping their resources here.
		 * Each subdirectory will get zipped up and then deleted
		 */
		File assembliesDir = new File(to, "assembly");
	
		@SuppressWarnings("unchecked")
		Collection<File> assemblies = CollectionUtils.arrayToList(assembliesDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File arg0) {
				return arg0.isDirectory();
			}
		}));
		for (File assembly : assemblies) {
			String name = FilenameUtils.getBaseName(assembly.getName());
			LOGGER.debug("Copying resources for assembly '{}'", name);
			ArchiveUtils.zip(assembly, FileUtils.getFile(assembliesDir, name + ".zip"));
			FileUtils.deleteQuietly(assembly);
		}

		LOGGER.debug("Repackaging Javadoc...");
		File docLib = new File(to, "doc/lib");
		
		//TODO these paths should probably all be stuck in some constant
		File client = FileUtils.getFile(to, "doc/api/client");
		if (client.exists()) {
			ArchiveUtils.jar(client, new File(docLib, "smartgwt-javadoc.jar"));	
		}
		
		File server = FileUtils.getFile(to, "doc/api/server");
		if (server.exists()) {
			ArchiveUtils.jar(server, new File(docLib, "isomorphic-javadoc.jar"));	
		}
	}

}