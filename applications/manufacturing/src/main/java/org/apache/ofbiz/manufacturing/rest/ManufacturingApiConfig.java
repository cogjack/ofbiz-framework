/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.manufacturing.rest;

import org.apache.ofbiz.base.util.UtilProperties;

/**
 * Configuration utility for the Manufacturing REST API feature flag.
 *
 * <p>Reads properties from {@code manufacturing.properties} to determine
 * whether Order→Manufacturing calls should use the REST API or the
 * traditional ServiceDispatcher path.</p>
 */
public final class ManufacturingApiConfig {

    private static final String RESOURCE = "manufacturing";
    private static final String PROP_USE_REST = "manufacturing.api.use-rest";
    private static final String PROP_BASE_URL = "manufacturing.api.base-url";
    private static final String DEFAULT_BASE_URL =
            "https://localhost:8443/manufacturing/api/v1/manufacturing";

    private ManufacturingApiConfig() { }

    /**
     * Returns {@code true} if the REST API path should be used for
     * Order→Manufacturing calls instead of direct dispatcher invocation.
     * Defaults to {@code false} (dispatcher path).
     */
    public static boolean isUseRest() {
        return "true".equalsIgnoreCase(
                UtilProperties.getPropertyValue(RESOURCE, PROP_USE_REST, "false"));
    }

    /**
     * Returns the base URL for the Manufacturing REST API.
     */
    public static String getBaseUrl() {
        return UtilProperties.getPropertyValue(RESOURCE, PROP_BASE_URL, DEFAULT_BASE_URL);
    }
}
