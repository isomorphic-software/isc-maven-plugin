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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.AbstractArtifact;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.SubArtifact;


/**
 * Models a Maven artifact, useful when interfacing with a Maven repository, and specifically during install and deploy operations.
 */
public class Module extends AbstractArtifact implements Comparable<Module> {

	String groupId = "";
	String artifactId = "";
	String version = "";
	String classifier = "";
	String extension = "";

	File file;
	
	Map<String, String> propertiesMap = new HashMap<String, String>(0);
	
	Set<Artifact> subs = new HashSet<Artifact>();
	
	public Module(Model model) {
		this(model, model.getPomFile());
	}
	
	public Module(Model model, File file) {
		this.file = file;
		artifactId = model.getArtifactId();
		groupId = model.getGroupId();
		version = model.getVersion();
		
		extension = model.getPackaging().toLowerCase();
		if ("maven-archetype".equals(extension)) {
			extension = "jar";
		}

		if (! extension.equals("pom") && model.getPomFile() != null) {
			attach(model.getPomFile(), null);
		}
	}

	public Boolean isPom() {
		return extension.equalsIgnoreCase("pom");
	}
	
	/**
	 * Convenience method for attaching {@link SubArtifact SubArtifacts} to this Artifact.
	 * @param file
	 * @param classifier
	 */
	public void attach(File file, String classifier) {
		subs.add(new SubArtifact(this, classifier, FilenameUtils.getExtension(file.getName()), file));
	}

	public Artifact[] getAttachments() {
		return subs.toArray(new Artifact[0]);
	}
	
	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getVersion() {
		return version;
	}

	public Artifact setVersion(String version) {
		this.version = version;
		return this;
	}

	public String getClassifier() {
		return classifier;
	}

	public void setClassifier(String classifier) {
		this.classifier = classifier;
	}

	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	public File getFile() {
		return file;
	}
	
	public Artifact setFile(File file) {
		this.file = file;
		return this;
	}

	public String getArtifactId() {
		return artifactId;
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		return propertiesMap.containsKey(key) ? propertiesMap.get(key) : defaultValue;
	}

	@Override
	public Map<String, String> getProperties() {
		return propertiesMap;
	}
	
	@Override
	public Artifact setProperties(Map<String, String> properties) {
		propertiesMap = properties;
		return this;
	}

    @Override
    public int hashCode() {
    	return toString().hashCode();
    }
    
    @Override
	public boolean equals(Object that) {
		if (that == this) {
			return true;
		} else if (!(that instanceof Module)) {
			return false;
		}

		return this.toString().equals(that.toString());
	}

	@Override
	public int compareTo(Module o) {
		return toString().compareTo(o.toString());
	}	
	
}