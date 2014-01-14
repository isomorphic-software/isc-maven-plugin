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

import org.junit.Assert;
import org.junit.Test;

import com.isomorphic.maven.util.ArchiveUtils;


public class UnpackRelocationTest {

	@Test
	public void dropRoot() {
		String entry = "smartgwtee-4.1d/lib/smartgwt.jar";
		String target = "target";
		
		String rewritten = ArchiveUtils.rewritePath(entry, target);
		
		Assert.assertEquals("target/smartgwt.jar", rewritten);
	}

	@Test
	public void dropRootAndRename() {
		String entry = "SmartClient_SNAPSHOT_v91d_2013-11-20/smartclientSDK/WEB-INF/lib/isomorphic_realtime_messaging.jar";
		String target = "target/isomorphic_messaging.jar";

		String rewritten = ArchiveUtils.rewritePath(entry, target);
		
		Assert.assertEquals("target/isomorphic_messaging.jar", rewritten);
	}
	
	@Test
	public void changeRoot() {

		String entry = "smartgwtee-4.1d/doc/javadoc/com/isc/servlet/Foo.html";
		String target = "doc/api/#javadoc";
		
		String rewritten = ArchiveUtils.rewritePath(entry, target);
		
		Assert.assertEquals("doc/api/com/isc/servlet/Foo.html", rewritten);
	}
	
	@Test
	public void sigh() {

		String entry = "smartgwtee-4.1d/doc/javadoc/com/isomorphic/servlet/CompressionFilter.html";
		String target = "doc/api/#javadoc/network";
		
		String rewritten = ArchiveUtils.rewritePath(entry, target);
		
		Assert.assertEquals("doc/api/com/isomorphic/servlet/network/CompressionFilter.html", rewritten);
	}
	
	@Test
	public void changeRootAndAddDirectoryAndRename() {
		String entry = "smartgwtee-4.1d/lib/smartgwtee.jar";
		String target = "foo/bar/#lib/baz/smartgwt-eval.jar";
		
		String rewritten = ArchiveUtils.rewritePath(entry, target);
		
		Assert.assertEquals("foo/bar/baz/smartgwt-eval.jar", rewritten);
	}
	

}