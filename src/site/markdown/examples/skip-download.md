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

###Install From Local Distribution

Nightly builds are not necessarily available indefinitely, and you may find from time to time that you need to use a version that has been superseded by a newer release or is otherwise no longer found on the [builds page](http://www.smartclient.com/builds/).  In this case, the plugin can be made to look locally for a distribution: 
    
1. If necessary, create a directory that [looks like](../apidocs/com/isomorphic/maven/mojo/AbstractPackagerMojo.html#skipDownload) one the plugin would have created for you.

        export WORKDIR = /tmp/SmartGWT/PowerEdition/4.1d/2013-11-25/zip
        mkdir $WORKDIR

2. Drop a copy of the file/s in question where the plugin can find it

        cp /downloads/smartgwtee-4.1d_11-25-2013.zip $WORKDIR

3. Use the [skipDownload](../apidocs/com/isomorphic/maven/mojo/AbstractPackagerMojo.html#skipDownload) property to cause resolution from the local file system.  Note that the value of the -Dworkdir argument is set to the /tmp, as in the path above.

        mvn isc:install -Dproduct=SMARTGWT \
                        -Dlicense=EVAL \
                        -DbuildNumber=4.1d \
                        -DbuildDate=2013-11-25 \
                        -Dworkdir=/tmp \
                        -DskipDownload=true
