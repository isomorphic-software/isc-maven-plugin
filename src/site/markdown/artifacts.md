###Artifacts

The archives available on the Isomorphic Software [builds page](http://www.smartclient.com/builds) contain everything you need to write applications using SmartClient / SmartGWT.

The download, install, and deploy goals of this plugin repackage some these resources to facilitate or simplify usage with Maven.  In some cases, a file is just renamed to more closely align with popular conventions for Maven artifacts.  In other cases, existing resources are repackaged to create an entirely new supplemental artifact. 

Each artifact's POM contains a description outlining what it's for, and in many cases includes a link to documentation, but in general terms there are at least 4 kinds of supplemental artifacts.  

---

####Overlays

The Maven WAR Plugin provides an overlay feature, allowing you to "include" resources from one artifact in another at build-time.  From its [documentation](http://maven.apache.org/plugins/maven-war-plugin/overlays.html):

> Overlays are used to share common resources across multiple web applications. The dependencies of a WAR project are collected in WEB-INF/lib, except for WAR artifacts which are overlayed on the WAR project itself.

This provides a nice, clean mechanism for the installation of (and subsequent updates to) SmartClient resources onto any standard Java web application.  (SmartGWT applications obtain most of these resources through [GWT modules](http://www.gwtproject.org/doc/latest/DevGuideOrganizingProjects.html#DevGuideAutomaticResourceInclusion).)

By using the WAR plugin already in your build, your application need do nothing more than declare its dependency on one of the WAR artifacts provided by the plugin

    <dependency>
			<groupId>com.isomorphic.smartclient.enterprise</groupId>
			<artifactId>smartclient-enterprise</artifactId>
			<version>${smartclient.version}</version>
			<type>pom</type>
	</dependency>

to copy the SmartClient runtime and related resources (skins, etc.).  Note that, as in this case, artifacts with names taking the form smartclient-${license} have POM packaging, with their own dependencies on WAR and server-side JAR artifacts.

SmartClient client and server resources are made available in this way, as are a handful of unrelated artifacts (e.g., the batchUpload datasource required when using that feature.)

Again, details of each are documented in the POMs themselves.  Refer to your local repository following installation, or to the copies left in your [${workdir}/${product}/${license}/${buildNumber}/latest](./apidocs/com/isomorphic/maven/mojo/AbstractPackagerMojo.html) folder.

####Resources

Some resources have been repackaged and left in .zip format for you to unpack as desired with e.g., the Maven Dependency Plugin's [Unpack Depdencies](https://maven.apache.org/plugins/maven-dependency-plugin/unpack-dependencies-mojo.html) goal.  The isc-selenium-resources artifact is one such case.  It may be that these artifacts are used just as easily, or more so, in WAR format, so a change to that effect is possible at some point in the future.  Let us know if you prefer one mechanism to another.

####JavaDoc

Isomorphic builds currently include only 2 JavaDoc bundles - one for all server packages, and one for all client packages (in the case of SmartGWT).  The plugin just repackages the existing JavaDoc into JAR file format, so that each artifact can be installed / deployed to your repository with JavaDoc attached (useful for IDE integrations, etc.) - isomorphic-javadoc with isomorphic-* artifacts, and smartgwt-javadoc with smartgwt-*.

####Dependency Groups

These are just a means for grouping logical dependencies together, convenient when those lists get long, as is the case with e.g., the dependencies required only when using PDF Export functionality present in isomorphic-core-rpc. 


