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
                                                                          
###Sample Project POMs

The dependencies you'll need in your own project will vary depending on what you've licensed and what framework features you're using. 

POMs like those below are suitable for building the SmartGWT "BuiltInDS" and corresponding SmartClient "Component Data Binding" samples included with the SDK (assuming you have source and resources in standard locations).

### SmartGWT

    <?xml version="1.0" encoding="UTF-8"?>
        <project xmlns="http://maven.apache.org/POM/4.0.0"   
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">

        <modelVersion>4.0.0</modelVersion>
    
        <groupId>com.isomorphic.smartgwt.samples</groupId>
        <artifactId>builtinds</artifactId>
        <version>1.0.0</version>
        <packaging>war</packaging>
    
        <properties>
            <gwt.version>2.5.1</gwt.version>
            <smartgwt.version>4.1-p20140311</smartgwt.version>
        </properties>
    
        <dependencies>
    
            <!-- Standard GWT depdendency -->
            <dependency>
                <groupId>com.google.gwt</groupId>
                <artifactId>gwt-user</artifactId>
                <version>${gwt.version}</version>
                <scope>provided</scope>
            </dependency>
    
            <!-- The SmartGWT Evaluation edition -->
            <dependency>
                <groupId>com.isomorphic.smartgwt.eval</groupId>
                <artifactId>smartgwt-eval</artifactId>
                <version>${smartgwt.version}</version>
            </dependency>
    
            <!-- Add support for SQLDataSources -->
            <dependency>
                <groupId>com.isomorphic.smartgwt.eval</groupId>
                <artifactId>isomorphic-sql</artifactId>
                <version>${smartgwt.version}</version>
            </dependency>
    
            <!-- Add the Network Performance module -->
            <dependency>
                <groupId>com.isomorphic.smartgwt.eval</groupId>
                <artifactId>isomorphic-network</artifactId>
                <version>${smartgwt.version}</version>
                <type>pom</type>
            </dependency>
    
            <!-- Add support for the sample database -->
            <dependency>
                <groupId>org.hsqldb</groupId>
                <artifactId>hsqldb</artifactId>
                <version>2.2.9</version>
            </dependency>
            <dependency>
                <groupId>commons-dbcp</groupId>
                <artifactId>commons-dbcp</artifactId>
                <version>1.4</version>
            </dependency>
    
        </dependencies>
    
        <build>
            <plugins>
                <!-- Standard GWT Compile -->
                <plugin>
                    <groupId>org.codehaus.mojo</groupId>
                    <artifactId>gwt-maven-plugin</artifactId>
                    <version>${gwt.version}</version>
                    <executions>
                        <execution>
                            <goals>
                                <goal>compile</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
    </project>

### SmartClient
    
    <project xmlns="http://maven.apache.org/POM/4.0.0" 
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    
        <modelVersion>4.0.0</modelVersion>
    
        <groupId>com.isomorphic.smartclient.samples</groupId>
        <artifactId>component-databinding</artifactId>
        <version>1.0.0</version>
        <packaging>war</packaging>
    
        <properties>
            <smartclient.version>9.1-p20140311</smartclient.version>
        </properties>
    
        <dependencies>
            
            <!-- The SmartClient Evaluation edition -->
            <dependency>
                <groupId>com.isomorphic.smartclient.eval</groupId>
                <artifactId>smartclient-eval</artifactId>
                <version>${smartclient.version}</version>
                <type>pom</type>
            </dependency>
            
            <!-- Use SQLDataSources -->
            <dependency>
                <groupId>com.isomorphic.smartclient.eval</groupId>
                <artifactId>isomorphic-sql</artifactId>
                <version>${smartclient.version}</version>
            </dependency>
            
            <!-- Support sample database -->
            <dependency>
                <groupId>org.hsqldb</groupId>
                <artifactId>hsqldb</artifactId>
                <version>2.2.9</version>
            </dependency>
            <dependency>
                <groupId>commons-dbcp</groupId>
                <artifactId>commons-dbcp</artifactId>
                <version>1.4</version>
            </dependency>
            
            <!-- Including the filter that starts it -->
            <dependency>
                <groupId>javax.servlet</groupId>
                <artifactId>servlet-api</artifactId>
                <version>2.5</version>
                <scope>provided</scope>
            </dependency>
        </dependencies>
    
    </project>
