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

import org.apache.commons.io.FilenameUtils;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of static utilities useful for working with JAR/ZIP archives.
 */
public class ArchiveUtils {

    /**
     * Builds a JAR file from the contents of a directory on the filesystem (recursively).
     * Adapted from stackoverflow solution.
     * <p>
     * Refer to <a href="http://stackoverflow.com/questions/1281229/how-to-use-jaroutputstream-to-create-a-jar-file"></a>
     *
     * @param directory the directory containing the content to be xzipped up
     * @param output    the zip file to be written to
     * @throws IOException when any I/O error occurs
     */
    public static void jar(File directory, File output) throws IOException {
        JarArchiver ja = new JarArchiver();
        ja.addFileSet(new DefaultFileSet(directory));
        ja.setDestFile(output);

        ja.createArchive();
    }

    /**
     * Builds a ZIP file from the contents of a directory on the filesystem (recursively).
     * <p>
     * Refer to <a href="http://stackoverflow.com/questions/1281229/how-to-use-jaroutputstream-to-create-a-jar-file"></a>
     *
     * @param directory the directory containing the content to be xzipped up
     * @param output    the zip file to be written to
     * @throws IOException when any I/O error occurs
     */
    public static void zip(File directory, File output) throws IOException {
        ZipArchiver za = new ZipArchiver();
        za.addFileSet(new DefaultFileSet(directory));
        za.setDestFile(output);

        za.createArchive();
    }

    /**
     * Unzips the <code>source</code> file to the <code>target</code> directory.
     *
     * @param source The file to be unzipped
     * @param target The directory to which the source file should be unzipped
     * @throws IOException when any I/O error occurs
     */
    public static void unzip(File source, File target) throws IOException {

        ZipUnArchiver ua = new ZipUnArchiver(source);
        ua.setDestFile(target);
        ua.extract();
    }

    /**
     * Derives a new file path, in whole or in part, from an existing path.  The following use cases are supported explicitly:
     * <ul>
     * <li>
     *      Rename a file (example: smartgwt-lgpl.jar)
     *      <p>
     *      <code>
     *      ArchiveUtils.rewritePath("smartgwtee-4.1d/lib/smartgwt.jar", "smartgwt-lgpl.jar");
     *      </code>
     * </li>
     * <li>
     *      Move to another directory (example: target/smartgwt.jar)
     *      <p>
     *      <code>
     *      ArchiveUtils.rewritePath("smartgwtee-4.1d/lib/smartgwt.jar", "target");
     *      </code>
     * </li>
     * <li>Move and rename (example: target/smartgwt-lgpl.jar)
     *      <p>
     *      <code>
     *          ArchiveUtils.rewritePath("smartgwtee-4.1d/lib/smartgwt.jar", "target/smartgwt-lgpl.jar");
     *      </code>
     * </li>
     * <li>Move to new root directory, preserving some part of the existing path
     *      <ul>
     *          <li>
     *              example: doc/api/com/isomorphic/servlet/IDACall.html
     *              <p>
     *              <code>
     *                  ArchiveUtils.rewritePath("smartgwtee-4.1d/doc/javadoc/com/isomorphic/servlet/IDACall.html","doc/api/#javadoc");
     *              </code>
     *          </li>
     *          <li>
     *              example: doc/api/com/isomorphic/servlet/network/FileAssembly.html
     *              <p>
     *              <code>
     * 	                ArchiveUtils.rewritePath("smartgwtee-4.1d/doc/javadoc/com/isomorphic/servlet/CompressionFilter.html", "doc/api/#javadoc/network");
     *              </code>
     *          </li>
     *      </ul>
     * </li>
     * </ul>
     *
     * @param oldValue the existing path
     * @param newValue the value to use for the new path, including optional tokens
     * @return the new path
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



}
