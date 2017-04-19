package com.isomorphic.maven.util;

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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

/**
 * A collection of static utilities useful for working with JAR/ZIP archives.
 */
public class ArchiveUtils {

	/**
	 * Builds a JAR file from the contents of a directory on the filesystem (recursively).
	 * Adapted from stackoverflow solution.  
	 * <p>
	 * Refer to http://stackoverflow.com/questions/1281229/how-to-use-jaroutputstream-to-create-a-jar-file
	 * 
	 * @param directory the directory containing the content to be xzipped up
	 * @param output the zip file to be written to
	 */
	public static void jar(File directory, File output) throws IOException {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		output.getParentFile().mkdirs();
		JarOutputStream target = new JarOutputStream(new FileOutputStream(output), manifest);
		zip(directory, directory, target);
		target.close();
	}

	/**
	 * Builds a ZIP file from the contents of a directory on the filesystem (recursively).
	 * <p>
	 * Refer to http://stackoverflow.com/questions/1281229/how-to-use-jaroutputstream-to-create-a-jar-file
	 * 
	 * @param directory the directory containing the content to be xzipped up
	 * @param output the zip file to be written to
	 * @throws IOException
	 */
	public static void zip(File directory, File output) throws IOException {
		output.getParentFile().mkdirs();
		ZipOutputStream target = new ZipOutputStream(new FileOutputStream(output));
		zip(directory, directory, target);
		target.close();
	}

	/**
	 * Unzips the <code>source</code> file to the <code>target</code> directory.
	 * 
	 * @param source The file to be unzipped
	 * @param target The directory to which the source file should be unzipped
	 * @throws IOException
	 */
	public static void unzip(File source, File target) throws IOException {
		
		ZipFile zip = new ZipFile(source);
	    Enumeration<? extends ZipEntry> entries = zip.entries();
	    while (entries.hasMoreElements()) {
	        ZipEntry entry = entries.nextElement();
	        File file = new File(target,  entry.getName());
	        FileUtils.copyInputStreamToFile(zip.getInputStream(entry), file);
	    }
	}
	
	/**
	 * Derives a new file path, in whole or in part, from an existing path.  The following use cases are supported explicitly:
	 * <ul>	
	 * 	<li>
	 * 		Rename a file (example: smartgwt-lgpl.jar)
	 * 		<p>
	 * 		<code>
	 * 			ArchiveUtils.rewritePath("smartgwtee-4.1d/lib/smartgwt.jar", "smartgwt-lgpl.jar");	
	 * 		</code> 
	 * 	</li>
	 * 	<li>
	 * 		Move to another directory (example: target/smartgwt.jar)
	 *		<p>
	 * 		<code>
	 * 			ArchiveUtils.rewritePath("smartgwtee-4.1d/lib/smartgwt.jar", "target");
	 * 		</code>
	 * </li>
	 * <li>Move and rename (example: target/smartgwt-lgpl.jar)
	 * 		<p>
	 * 		<code>
	 * 			ArchiveUtils.rewritePath("smartgwtee-4.1d/lib/smartgwt.jar", "target/smartgwt-lgpl.jar"); 
	 * 		</code> 
	 * </li>
	 *  <li>Move to new root directory, preserving some part of the existing path
	 *  	<ul>
	 *  		<li>
	 *  			example: doc/api/com/isomorphic/servlet/IDACall.html
	 *  			<p>
	 *  			<code>
	 *  				ArchiveUtils.rewritePath("smartgwtee-4.1d/doc/javadoc/com/isomorphic/servlet/IDACall.html","doc/api/#javadoc");
	 *  			</code>
	 *    		</li>
	 *    		<li>
	 *  			example: doc/api/com/isomorphic/servlet/network/FileAssembly.html
	 *  			<p>
	 *  			<code>
	 * 	 				ArchiveUtils.rewritePath("smartgwtee-4.1d/doc/javadoc/com/isomorphic/servlet/CompressionFilter.html", "doc/api/#javadoc/network");
	 *  			</code>
	 *    		</li>
	 *  	</ul>
	 *   </li>
	 * </ul>
	 * 
	 * @param oldValue the existing path
	 * @param newValue the value to use for the new path, including optional tokens
	 * @return
	 */
	public static String rewritePath(String oldValue, String newValue) {

		String path = newValue;
		String filename;

		if ("".equals(FilenameUtils.getExtension(newValue))) {
			filename = FilenameUtils.getName(oldValue);
		} else {
			path = FilenameUtils.getPath(newValue);
			filename = FilenameUtils.getName(newValue);
		}
		
		Pattern p = Pattern.compile("(.*?)#(.*?)(?:$|(/.*))");
		Matcher m = p.matcher(path);
		
		if (m.find()) {
			
			String prefix = m.group(1);
			String dir = m.group(2);
			String suffix = m.group(3);

			int index = oldValue.indexOf(dir);
			int length = dir.length();
			
			String remainder = FilenameUtils.getPath(oldValue.substring(index + length));
			
			if (prefix != null && !prefix.equals("")) {
				path = prefix + remainder;				
			} else {
				path = remainder;
			}
			
			if (suffix != null && !suffix.equals("")) {
				path += suffix;				
			}
		}
		
		return FilenameUtils.normalize(path + "/" + filename);
	}
	
	/**
	 * Steps common to archiving both zip and jar files, which include reading files from disk and using
	 * their contents to create {@link ZipEntry ZipEntries} and writing them to a ZipOutputStream.
	 * 
	 * @param root
	 * @param source
	 * @param target
	 * @throws IOException
	 */
	private static void zip(File root, File source, ZipOutputStream target) throws IOException {
		String relativePath = root.toURI().relativize(source.toURI()).getPath().replace("\\", "/");
		
		BufferedInputStream in = null;
		try {
			if (source.isDirectory()) {

				if (!relativePath.endsWith("/")) {
					relativePath += "/";
				}
				ZipEntry entry = ZipEntryFactory.get(target, relativePath);
				entry.setTime(source.lastModified());
				target.putNextEntry(entry);
				target.closeEntry();

				for (File nestedFile : source.listFiles()) {
					zip(root, nestedFile, target);
				}
				return;
			}

			ZipEntry entry = ZipEntryFactory.get(target, relativePath);
			entry.setTime(source.lastModified());
			target.putNextEntry(entry);
			in = new BufferedInputStream(FileUtils.openInputStream(source));
			IOUtils.copy(in, target);
			target.closeEntry();

		} finally {
			IOUtils.closeQuietly(in);
		}
	}
	
	private static class ZipEntryFactory {
		static ZipEntry get(ZipOutputStream target, String name) {
			if (target instanceof JarOutputStream) {
				return new JarEntry(name);
			}
			return new ZipEntry(name);
		}
	}

}