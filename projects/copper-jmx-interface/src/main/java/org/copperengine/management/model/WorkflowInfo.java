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
package org.copperengine.management.model;

import java.beans.ConstructorProperties;
import java.io.Serializable;
import java.util.Date;

public class WorkflowInfo implements Serializable {

    private static final long serialVersionUID = -659602204078726674L;

    private String id;
    private String state;
    private int priority;
    private String engineId;
    private String processorPoolId;
    private Date timeout;
    private WorkflowClassInfo workflowClassInfo;
    private String dataAsString;
    private String lastWaitStackTrace;
    private ErrorData errorData;
    private Date lastModTS;
    private Date creationTS;

    public WorkflowInfo() {
    }

    @ConstructorProperties({"id","state","priority","processorPoolId","timeout","workflowClassInfo","dataAsString","lastWaitStackTrace","errorData", "lastModTS","creationTS"})
    public WorkflowInfo(String id, String state, int priority, String processorPoolId, Date timeout, WorkflowClassInfo workflowClassInfo, String dataAsString, String lastWaitStackTrace, ErrorData errorData, Date lastModTS, Date creationTS) {
        super();
        this.id = id;
        this.state = state;
        this.priority = priority;
        this.engineId = "";
        this.processorPoolId = processorPoolId;
        this.timeout = timeout;
        this.workflowClassInfo = workflowClassInfo;
        this.dataAsString = dataAsString;
        this.lastWaitStackTrace = lastWaitStackTrace;
        this.errorData = errorData;
        this.lastModTS = lastModTS;
        this.creationTS = creationTS;
    }

    public String getEngineId() { return engineId; }

    public void setEngineId(String id) { this.engineId = id; }

    public Date getCreationTS() {
        return creationTS;
    }
    
    public void setCreationTS(Date creationTS) {
        this.creationTS = creationTS;
    }
    
    public Date getLastModTS() {
        return lastModTS;
    }
    
    public void setLastModTS(Date lastModTS) {
        this.lastModTS = lastModTS;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getProcessorPoolId() {
        return processorPoolId;
    }

    public void setProcessorPoolId(String processorPoolId) {
        this.processorPoolId = processorPoolId;
    }

    public Date getTimeout() {
        return timeout;
    }

    public void setTimeout(Date timeout) {
        this.timeout = timeout;
    }

    public WorkflowClassInfo getWorkflowClassInfo() {
        return workflowClassInfo;
    }

    public void setWorkflowClassInfo(WorkflowClassInfo workflowClassInfo) {
        this.workflowClassInfo = workflowClassInfo;
    }

    public String getDataAsString() {
        return dataAsString;
    }

    public void setDataAsString(String dataAsString) {
        this.dataAsString = dataAsString;
    }

    public String getLastWaitStackTrace() {
        return lastWaitStackTrace;
    }

    public void setLastWaitStackTrace(String lastWaitStackTrace) {
        this.lastWaitStackTrace = lastWaitStackTrace;
    }

    public ErrorData getErrorData() {
        return errorData;
    }

    public void setErrorData(ErrorData errorData) {
        this.errorData = errorData;
    }

    @Override
    public String toString() {
        return "WorkflowInfo [id=" + id + ", state=" + state + ", priority=" + priority + ", processorPoolId=" + processorPoolId + ", timeout=" + timeout + ", workflowClassInfo=" + workflowClassInfo + ", dataAsString=" + dataAsString + ", lastWaitStackTrace=" + lastWaitStackTrace + ", errorData=" + errorData + ", lastModTS=" + lastModTS + ", creationTS=" + creationTS + "]";
    }

}
