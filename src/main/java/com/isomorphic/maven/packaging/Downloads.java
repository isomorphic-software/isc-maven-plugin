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

import com.isomorphic.maven.util.HttpRequestManager;
import com.isomorphic.maven.util.LoggingCountingOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Connects to Isomorphic site, discovers which files exist for a given build, and downloads them 
 * to local file system. 
 */
public class Downloads {

    private static final Logger LOGGER = LoggerFactory.getLogger(Downloads.class);
    private static HttpRequestManager httpWorker;

    private File toFolder = new File(System.getProperty("java.io.tmpdir"));
    private Boolean overwriteExistingFiles = Boolean.FALSE;

    /**
     * Constructor taking the request manager used to communicate with smartclient.com.
     *
     * @param worker the request manager used to communicate with smartclient.com.
     */
    public Downloads(HttpRequestManager worker) {
        httpWorker = worker;
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
     * @param overwriteExistingFiles true if files should be overwritten, false otherwise.
     */
    public void setOverwriteExistingFiles(Boolean overwriteExistingFiles) {
        this.overwriteExistingFiles = overwriteExistingFiles;
    }

    /**
     * Retrieves a {@link Distribution} instance for each of the given licenses, downloads
     * files if necessary, and {@link Distribution#getFiles() links} the local file to the distribution.
     * <p>
     * Refer to <a href="http://www.smartclient.com/builds/"></a>
     *
     * @param product The product built and distributed by Isomorphic Software.  e.g., SmartCLient
     * @param buildNumber The build number of the desired distribution.  e.g., 4.1d
     * @param buildDate The date on which the distribution was made available
     * @param licenses The licenses, or editions, that the product is released under, and for which the user is registered
     * @return A collection of Distributions, each having its contents resolved to local files, and suitable for use in repacking operations
     * @throws MojoExecutionException on any error
     */
    public List<Distribution> fetch(Product product, String buildNumber, String buildDate, License...licenses) throws MojoExecutionException {

        List<Distribution> result = new ArrayList<Distribution>();

        for (License license : licenses) {
            Distribution distribution = Distribution.get(product, license);
            download(distribution, buildNumber, buildDate);
            result.add(distribution);
        }

        return result;

    }

    public String findCurrentBuild(Distribution distribution, String buildNumber) throws MojoExecutionException {

        String url = distribution.getRemoteIndex(buildNumber, null);
        String selector = "a[href~=[0-9]{4}-[0-9]{2}-[0-9]{2}]";
        
        String[] links = list(url, selector);

        if (links.length > 0) {
            return links[0];
        } else {
            return null;
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
                response = httpWorker.execute(httpget);
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
     * @return a String array of html href attributes
     * @throws MojoExecutionException
     */
    private String[] list(String url, String selector) throws MojoExecutionException {

        HttpGet request = new HttpGet(url);
        HttpResponse response;

        try {

            LOGGER.debug("Requesting list of files from '{}{}'", httpWorker.getHostName(), url);
            response = httpWorker.execute(request);

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
            String msg = String.format("No downloads found at '%s%s'.  Response from server: \n\n%s\n", httpWorker.getHostName(), url, doc.html());
            LOGGER.warn(msg);
        }

        return result.toArray(new String[0]);
    }

}
