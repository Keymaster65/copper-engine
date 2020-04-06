/*
 * Copyright 2002-2015 SCOOP Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.copperengine.core.common;

import java.util.Collection;
import java.util.List;

import org.copperengine.core.ProcessingEngine;

/**
 * Interface for managing {@link ProcessorPool}s in one COPPER {@link ProcessingEngine}.
 *
 * @param <T> type of ProcessorPool hold by the manager
 * @author austermann
 */
public interface ProcessorPoolManager<T extends ProcessorPool> {
    public void setEngine(ProcessingEngine engine);

    public T getProcessorPool(String poolId);

    public List<String> getProcessorPoolIds();

    public void setProcessorPools(List<T> processorPools);

    public void addProcessorPool(T pool);

    public void removeProcessorPool(String poolId);

    public void startup();

    public void shutdown();

    public Collection<T> processorPools();
}
