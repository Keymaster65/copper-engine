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
package org.copperengine.core.monitoring;

import java.util.concurrent.TimeUnit;

/**
 * Collects runtime statistics of named measure points for the purpose of monitoring the performance of an application.
 * It depends on the implementation how runtime statistics are handled.
 *
 * @author austermann
 */
public interface RuntimeStatisticsCollector {
    public void submit(String measurePointId, int elementCount, long elapsedTime, TimeUnit timeUnit);
}
