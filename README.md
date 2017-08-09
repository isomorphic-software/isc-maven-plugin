<!--
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
-->


#Overview

Prior to SmartGWT version 4.1 / SmartClient version 10.1, Isomorphic Software provided a [Maven repository](http://www.smartclient.com/maven2/) containing SmartGWT artifacts released under the LGPL license. [Licensed editions](http://www.smartclient.com/product/editions.jsp) containing the powerful [server framework](http://smartclient.com/product/whyupgrade.jsp) were never published on any public repository, instead requiring that they be installed / deployed locally. Since SmartGWT version 4.0p / SmartClient 9.0p, POMs have been distributed with the official SDK for this purpose.

The SmartClient/SmartGWT JARs you'll find in any SDK download are just like any other JAR - you could deploy them to your own Maven repositories with the maven-deploy-plugin's [deploy-file](http://maven.apache.org/plugins/maven-deploy-plugin/deploy-file-mojo.html) goal.  There are a lot of steps involved, though, to do that the right way for so many artifacts (version 4.1 of the SmartGWT eval bundle contains more than 20 of them).

This plugin, based on work done previously in a user-contributed [google code plugin](http://code.google.com/p/smartgwt-maven-plugin/), is meant to provide a community-based effort to simplify and automate the manual process, by providing one-step:

* Download and extraction of all distributions you're entitled to under your Isomorphic license agreement (i.e., any optional modules are downloaded for you in the same step)

* Extraction of select SDK resources (e.g., documentation, selenium extensions) into a 'latest' folder, suitable for bookmarking.

* Translation from traditional Isomorphic conventions to Maven standards for naming, versioning, etc.

* Creation and installation of [supplemental resource assemblies & overlays](http://github.smartclient.com/isc-maven-plugin/artifacts.html)
	
* Installation of each resulting SmartClient/SmartGWT artifact into your local repository, complete with javadoc attachments where applicable

* Deployment of each resulting SmartClient/SmartGWT artifact into your own repository manager, complete with javadoc attachments where applicable

Refer to the [plugin site](http://github.smartclient.com/isc-maven-plugin/) for complete documentation. 


