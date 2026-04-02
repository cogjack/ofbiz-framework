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
package org.apache.ofbiz.manufacturing.ports.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.manufacturing.ports.ProductConfiguration;
import org.apache.ofbiz.manufacturing.ports.ProductConfigurationOption;
import org.apache.ofbiz.product.config.ProductConfigWrapper;
import org.apache.ofbiz.product.config.ProductConfigWrapper.ConfigOption;

/**
 * Adapter that delegates to the Product module's {@code ProductConfigWrapper}.
 * This class lives in {@code ports/impl/} so that the cross-module import
 * of {@code ProductConfigWrapper} is confined to the boundary layer.
 */
public final class ProductConfigWrapperAdapter implements ProductConfiguration {

    private final ProductConfigWrapper delegate;

    /**
     * Wraps a raw object (expected to be a {@code ProductConfigWrapper}) from the service context.
     * @param rawConfig the ProductConfigWrapper instance from the service context
     */
    public ProductConfigWrapperAdapter(Object rawConfig) {
        this.delegate = (ProductConfigWrapper) rawConfig;
    }

    @Override
    public boolean isCompleted() {
        return delegate.isCompleted();
    }

    @Override
    public String getProductId() {
        return delegate.getProduct().getString("productId");
    }

    @Override
    public String getConfigId() {
        return delegate.getConfigId();
    }

    @Override
    public List<ProductConfigurationOption> getSelectedOptions() {
        List<ProductConfigurationOption> options = new ArrayList<>();
        for (ConfigOption co : delegate.getSelectedOptions()) {
            options.add(new ConfigOptionAdapter(co));
        }
        return options;
    }

    /**
     * Inner adapter for {@code ProductConfigWrapper.ConfigOption}.
     */
    private static final class ConfigOptionAdapter implements ProductConfigurationOption {

        private final ConfigOption delegate;

        ConfigOptionAdapter(ConfigOption delegate) {
            this.delegate = delegate;
        }

        @Override
        public List<GenericValue> getComponents() {
            return delegate.getComponents();
        }

        @Override
        public boolean isVirtualComponent(GenericValue component) {
            return delegate.isVirtualComponent(component);
        }

        @Override
        public Map<String, String> getComponentOptions() {
            return delegate.getComponentOptions();
        }

        @Override
        public String getComments() {
            return delegate.getComments();
        }

        @Override
        public String getDescription() {
            return delegate.getDescription();
        }
    }
}
