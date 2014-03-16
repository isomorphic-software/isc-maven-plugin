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

###Configuration / Settings
                                                                            
Most users will need to add some or all of the following entries to their [Maven settings](http://maven.apache.org/ref/3.1.1/maven-settings/settings.html).

    <settings>

        <pluginGroups> 
    [1]     <pluginGroup>com.isomorphic</pluginGroup>
        </pluginGroups>

        <servers>
    [2]     <server>
                <id>smartclient-developer</id>
                <username>scott</username>
                <password>tiger</password>
            </server>
    [3]     <server>
                <id>repository-manager</id>
                <username>bruce.scott</username>
                <password>t1ger!</password>
            </server>
        </servers>

        <profiles>
    [4]     <profile>
                <id>isc</id>
                <properties>
                    <product>SMARTGWT</product>
                    <license>POWER</license>
                    <buildNumber>4.0p</buildNumber>
                    <workdir>/users/scott/downloads/isomorphic</workdir>
                    <repositoryId>repository-manager</repositoryId>
                    <repositoryUrl>http://nexus.corp.int/nexus/content/repositories/thirdparty/</repositoryUrl>
                </properties>
            </profile>
        </profiles>
    
    </settings>


1. A [pluginGroup](http://maven.apache.org/settings.html#Plugin_Groups) entry, enabling the shorthand __isc__ plugin prefix.
    
2. A [server](http://maven.apache.org/settings.html#Servers) entry, containing your SmartClient Developer credentials.
    
3. Another server entry, containing the credentials you would use to [deploy](../deploy-mojo.html) a file to your own repository manager.
    
4. A [profile](http://maven.apache.org/ref/2.2.1/maven-settings/settings.html#class_profile) containing configuration values to be used at goal execution, typically activated with a -P switch.  Note that system properties can be used to complement or override values provided in the profile.