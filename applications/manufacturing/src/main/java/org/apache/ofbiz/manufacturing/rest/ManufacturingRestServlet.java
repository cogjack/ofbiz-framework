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

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.manufacturing.rest.dto.ApiErrorResponse;

/**
 * Front-controller servlet for the Manufacturing REST API (v1).
 *
 * <p>Routes incoming requests to the appropriate handler method in
 * {@link ProductionRunController} based on the request path.</p>
 *
 * <p>Mapped to {@code /api/v1/manufacturing/*} in web.xml.</p>
 */
public class ManufacturingRestServlet extends HttpServlet {

    private static final String MODULE = ManufacturingRestServlet.class.getName();
    private static final long serialVersionUID = 1L;

    private static final String PATH_PRODUCTION_RUNS = "/production-runs";
    private static final String PATH_MKTG_PKG = "/production-runs/marketing-package";
    private static final String PATH_FROM_CONFIG = "/production-runs/from-configuration";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "";
        }

        try {
            if (PATH_MKTG_PKG.equals(pathInfo)) {
                ProductionRunController.handleCreateMktgPkgProductionRun(request, response);
            } else if (PATH_FROM_CONFIG.equals(pathInfo)) {
                ProductionRunController.handleCreateProductionRunFromConfig(request, response);
            } else if (PATH_PRODUCTION_RUNS.equals(pathInfo)) {
                ProductionRunController.handleCreateProductionRun(request, response);
            } else {
                RestUtil.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                        new ApiErrorResponse("NOT_FOUND",
                                "No endpoint matches: POST " + pathInfo, null));
            }
        } catch (Exception e) {
            Debug.logError(e, "Unexpected error in Manufacturing REST API", MODULE);
            RestUtil.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    new ApiErrorResponse("INTERNAL_ERROR",
                            "An unexpected error occurred", e.getMessage()));
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RestUtil.sendError(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                new ApiErrorResponse("METHOD_NOT_ALLOWED",
                        "GET is not supported; use POST", null));
    }
}
