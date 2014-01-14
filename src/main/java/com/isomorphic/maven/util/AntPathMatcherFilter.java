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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.util.AntPathMatcher;

/**
 * Provides a mechanism to see whether or not a String path is matched by one or more ant-style patterns.
 */
public class AntPathMatcherFilter {

		private static AntPathMatcher matcher = new AntPathMatcher();
	
		private List<String> includes = new ArrayList<String>();
		private List<String> excludes = new ArrayList<String>();
		
		/**
		 * Default constructor whose behavior is to include all files.
		 */ 
		public AntPathMatcherFilter() {
			include("**/*");
		}
		
		/**
		 * Allows the specification of one or more patterns for inclusion and/or exclusion. 
		 * 
		 * @param includes a comma-separated list of patterns to be included in matching
		 * @param excludes a comma-separated list of patterns to be excluded from matching
		 */
		public AntPathMatcherFilter(String includes, String excludes) {
			include(includes);
			exclude(excludes);
		}
		
		/**
		 * Add the given patterns to the current list of inclusions.
		 * 
		 * @param patterns a comma-separated list of patterns to be included in matching
		 * @return this AntPathMatcherFilter instance, in fluid interface style
		 */
		public AntPathMatcherFilter include(String patterns) {
			if (patterns != null) {
				String[] split = patterns.split(",");
				for (String pattern : split) {
					includes.add(pattern.trim());				
				}
			}
			return this;
		};
		
		/**
		 * Add the given patterns to the current list of exclusions.
		 * 
		 * @param patterns a comma-separated list of patterns to be excluded from matching
		 * @return this AntPathMatcherFilter instance, in fluid interface style
		 */
		public AntPathMatcherFilter exclude(String patterns) {
			if (patterns != null) {
				String[] split = patterns.split(",");
				for (String pattern : split) {
					excludes.add(pattern.trim());				
				}
			}
			return this;
		};
		
		/**
		 * Returns false if <code>path</code> matches any exclusion, otherwise evaluates each inclusion ti see whether any
		 * of them match and returns true if so.
		 * 
		 * @param path The path to be matched
		 * @return true if path matches one or more inclusions and no exclusions
		 */
		public boolean accept(String path) {

			for (String exclusion : excludes) {
				if (matcher.match(exclusion, path)) {
					return false;
				}
			}
			
			boolean matched = false;
			for (String inclusion : includes) {
				if (matcher.match(inclusion, path)) {
					matched = true;
					break;
				}
			}
			return matched;
		};

		/**
		 * Copies this object's inclusions and exclusions to a new instance.
		 * 
		 * @return a new AntPathMAtcherFilter instance with copies of this instance's inclusions and exclusions
		 */
		public AntPathMatcherFilter copy() {
			AntPathMatcherFilter source = this;
			AntPathMatcherFilter destination = new AntPathMatcherFilter();
			Collections.copy(destination.includes, source.includes);
			Collections.copy(destination.excludes, source.excludes);
			return destination;
		}
		
}