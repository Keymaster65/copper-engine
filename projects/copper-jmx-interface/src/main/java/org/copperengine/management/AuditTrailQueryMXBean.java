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
package org.copperengine.management;

import java.util.List;

import org.copperengine.management.model.AuditTrailInfo;
import org.copperengine.management.model.AuditTrailInstanceFilter;

public interface AuditTrailQueryMXBean {

    @Deprecated
    List<AuditTrailInfo> getAuditTrails(String transactionId, String conversationId, String correlationId, Integer level, int maxResult);

    @Deprecated
    byte[] getMessage(long id);


    List<AuditTrailInfo> getAuditTrails(AuditTrailInstanceFilter filter);

    int countAuditTrails(AuditTrailInstanceFilter filter);

    String getMessageString(long id);
}
