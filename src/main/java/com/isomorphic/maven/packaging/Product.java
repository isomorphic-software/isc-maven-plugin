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
 * The collection of Isomorphic products for which Maven support is a possibility.
 */
public enum Product {

    SMARTCLIENT("SmartClient"),
    SMARTGWT("SmartGWT"),
    SMARTGWT_MOBILE("SmartGWT.mobile");

    private String label;
    private String name;

    Product(String label) {
        this(label, label);
    }

    Product(String label, String name) {
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
