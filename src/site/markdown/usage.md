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

### Usage

Install and Deploy goals are meant to be invoked directly.  That is, executed outside the context of any build lifecycle. 
    
Though it is possible to configure the plugin in your project's POM, and even to bind it to a lifecycle phase, a better option is normally to move these "repackaging operations" to a workflow independent of your build with a command line like the following: 

    mvn isc:deploy -Dproduct=SMARTCLIENT -Dlicense=EVAL -DbuildNumber=10.0d

This just causes a collection of Maven artifacts to be deployed to your own repository, where Maven will be able to resolve dependencies in the usual way at the usual time.  Once deployed, there is no need to run the plugin again until you wish to update to a newer build.

### Configuration

All the usual Maven configuration mechanisms apply, including the use of system properties (as above) and build profile property values.  Examples will generally be written with the explicit activation of some profile present - assume required parameters and so on are provided there.  
    
Most users will need minor modifications to user settings in either case - the [configuration example](./examples/configuration.html) illustrates typical requirements.  Refer to each goal's [plugin documentation](./plugin-info.html) for a complete listing of configuration parameters.      
 
###Convention

Isomorphic historically uses a versioning scheme that may be unfamiliar to Maven users.  If you haven't already, you should have a read through the narrative at the [nightly builds](http://www.smartclient.com/builds) page. Do notice that the SmartClient version numbers and SmartGWT version numbers are not necessarily the same.  This is important because the plugin uses the product, license, and version number to determine both the URL of the download/s and the local path of the file as it should exist following the download.

With a few exceptions, artifacts take the same name as a corresponding file in the SDK, except that underscores are replaced by hyphens, in line with popular Maven conventions.  At least 5 kinds of artifacts are produced by install/deploy goals, identifiable by their prefix:

* __smartclient-__: Artifacts belonging to the client-side SmartClient framework.
    
* __smartgwt-__: Artifacts belonging to the client-side SmartGWT framework.

* __isomorphic-__: Artifacts belonging to the server framework.
    
* __dependencygroup-__: Logical grouping of related dependencies.

* __archetype-__: Archetypes useful for new project creation.
    
* __isc-__: Everything else, including support libraries, Maven assemblies, etc.
    
Version numbers are changed slightly to include the nightly build date, and again to more closely mirror popular Maven conventions.  e.g., a 4.0p _patch build_ produced by Isomorphic on September 16, 2013 would be versioned with a number like 4.0-p20130916 and a 4.1 _development build_ from the same day would get [by default](./apidocs/com/isomorphic/maven/mojo/AbstractPackagerMojo.html#snapshots) a version number of 4.1-d20130916-SNAPSHOT.
    
### Putting it all together

1. Execute a plugin goal, optionally providing configuration property values in a build profile.  Maven will install the plugin, and the plugin will install and/or deploy the SDK artifacts.

        mvn isc:install -Pisc -DbuildDate=2013-09-16
 
2. Depend on SDK artifacts in your project's POM

        <!-- SmartClient Evaluation edition -->
        <dependency>
            <groupId>com.isomorphic.smartclient.eval</groupId>
            <artifactId>smartclient-eval</artifactId>
            <version>9.0-p20130916</version>
            <type>pom</type>
        </dependency>

    or

        <!-- SmartGWT Evaluation edition -->
        <dependency>
            <groupId>com.isomorphic.smartgwt.eval</groupId>
            <artifactId>smartgwt-eval</artifactId>
            <version>4.0-p20130916</version>
        </dependency>
