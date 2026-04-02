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
package org.apache.ofbiz.manufacturing.ports;

import java.util.List;

/**
 * Thin adapter interface abstracting {@code ProductConfigWrapper} from the Product module.
 * Only exposes the methods that the Manufacturing module actually consumes
 * in {@code ProductionRunServices.createProductionRunFromConfiguration}.
 *
 * <p>In Phase 1B, this is backed by {@code ProductConfigWrapperAdapter} which delegates
 * to the real {@code ProductConfigWrapper}.</p>
 */
public interface ProductConfiguration {

    /**
     * Returns whether the configuration is complete (all required options selected).
     * @return true if the configuration is completed
     */
    boolean isCompleted();

    /**
     * Returns the product ID of the configurable product.
     * @return the product ID
     */
    String getProductId();

    /**
     * Returns the configuration ID.
     * @return the config ID
     */
    String getConfigId();

    /**
     * Returns the list of selected configuration options.
     * @return list of selected options
     */
    List<? extends ProductConfigurationOption> getSelectedOptions();
}
