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

###FAQ

1. __Why does my install fail with an error like, "Failure during assembly collection: error in opening zip file"?__

    This indicates that the zip file is somehow corrupt, as can happen when an execution is interrupted.  Just run the goal again, providing a 'true' value to the [overwrite](./apidocs/com/isomorphic/maven/mojo/AbstractPackagerMojo.html#overwrite) property.

2. __Does your plugin detect and use proxy settings?__

    Yes, the plugin should respect your (active) [Maven proxy settings](http://maven.apache.org/guides/mini/guide-proxies.html).

3. __Which dependencies do I need for my project?__

    Users of SmartGWT or SmartClient LGPL editions only get 

        com.isomorphic.smartgwt.lgpl:smartgwt-lgpl
    
    and 

        com.isomorphic.smartclient.lgpl:smartclient-lgpl
    
    artifacts, respectively. Users of Pro+ features with the server framework have many more options, and it depends on what you're licensed for and which features you're trying to use.  As a rule, start with smartclient / smartgwt-eval|pro|power|enterprise, which gets you the core transitively, then refer to the [official documentation for server modules](http://www.smartclient.com/smartgwtee/javadoc/com/smartgwt/client/docs/JavaModuleDependencies.html) to discover what each of them do.  Finally, refer to this plugin's own documentation for [supplemental artifacts](./artifacts.html) and use the [SmartClient forums](http://forums.smartclient.com) to ask for help if you somehow get stuck.
    
