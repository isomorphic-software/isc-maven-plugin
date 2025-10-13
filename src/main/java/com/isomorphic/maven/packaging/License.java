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

/**
 * An Isomorphic product 'edition'.   
 * <p>
 * Refer to http://www.smartclient.com/product/editions.jsp
 */
public enum License {

    LGPL("LGPL"),
    EVAL("Eval"),
    PRO("Pro"),
    POWER("PowerEdition", "power"),
    ENTERPRISE("Enterprise"),

    ANALYTICS_MODULE("AnalyticsModule", "analytics"),
    MESSAGING_MODULE("RealtimeMessagingModule", "messaging"),
    AI_MODULE("AIModule", "ai");

    String label;
    String name;

    private License(String label) {
        this(label, label);
    }

    private License(String label, String name) {
        this.label = label;
        this.name = name.toLowerCase();
    }

    @Override
    public String toString() {
        return getLabel();
    }

    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }
}
