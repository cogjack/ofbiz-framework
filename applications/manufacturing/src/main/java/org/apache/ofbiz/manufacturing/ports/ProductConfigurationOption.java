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
import java.util.Map;

import org.apache.ofbiz.entity.GenericValue;

/**
 * Thin adapter interface abstracting {@code ProductConfigWrapper.ConfigOption} from the Product module.
 * Only exposes the methods that the Manufacturing module actually consumes
 * in {@code ProductionRunServices.createProductionRunFromConfiguration}.
 */
public interface ProductConfigurationOption {

    /**
     * Returns the BOM components for this configuration option.
     * @return list of component GenericValues
     */
    List<GenericValue> getComponents();

    /**
     * Returns whether the given component is a virtual component that needs variant resolution.
     * @param component the component to check
     * @return true if the component is virtual
     */
    boolean isVirtualComponent(GenericValue component);

    /**
     * Returns the selected variant options for virtual components.
     * @return map of component product ID to selected variant product ID
     */
    Map<String, String> getComponentOptions();

    /**
     * Returns the user comments for this option.
     * @return the comments string, or null
     */
    String getComments();

    /**
     * Returns the description of this option.
     * @return the description string
     */
    String getDescription();
}
