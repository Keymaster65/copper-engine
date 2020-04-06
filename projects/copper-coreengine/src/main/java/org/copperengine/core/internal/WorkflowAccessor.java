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
package org.copperengine.core.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;

import org.copperengine.core.ProcessingState;
import org.copperengine.core.Workflow;
import org.copperengine.core.persistent.ErrorData;
import org.copperengine.core.persistent.PersistentWorkflow;
import org.copperengine.core.persistent.RegisterCall;

public class WorkflowAccessor {

    private static final Method methodSetProcessingState;
    private static final Method methodSetCreationTS;
    private static final Method methodSetLastActivityTS;
    private static final Method methodSetTimeoutTS;
    private static final Method methodSetErrorData;
    private static final Field fieldRegisterCall;

    static {
        try {
            methodSetProcessingState = Workflow.class.getDeclaredMethod("setProcessingState", ProcessingState.class);
            methodSetProcessingState.setAccessible(true);

            methodSetCreationTS = Workflow.class.getDeclaredMethod("setCreationTS", Date.class);
            methodSetCreationTS.setAccessible(true);

            methodSetLastActivityTS = Workflow.class.getDeclaredMethod("setLastActivityTS", Date.class);
            methodSetLastActivityTS.setAccessible(true);

            methodSetTimeoutTS = Workflow.class.getDeclaredMethod("setTimeoutTS", Date.class);
            methodSetTimeoutTS.setAccessible(true);
            
            methodSetErrorData = PersistentWorkflow.class.getDeclaredMethod("setErrorData", ErrorData.class);
            methodSetErrorData.setAccessible(true);

            fieldRegisterCall = PersistentWorkflow.class.getDeclaredField("registerCall");
            fieldRegisterCall.setAccessible(true);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public static void setProcessingState(Workflow<?> w, ProcessingState s) {
        try {
            methodSetProcessingState.invoke(w, s);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setCreationTS(Workflow<?> w, Date creationTS) {
        try {
            methodSetCreationTS.invoke(w, creationTS);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setLastActivityTS(Workflow<?> w, Date lastActivityTS) {
        try {
            methodSetLastActivityTS.invoke(w, lastActivityTS);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setTimeoutTS(Workflow<?> w, Date timeoutTS) {
        try {
            methodSetTimeoutTS.invoke(w, timeoutTS);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setErrorData(PersistentWorkflow<?> w, ErrorData errorData) {
        try {
            methodSetErrorData.invoke(w, errorData);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static RegisterCall getRegisterCall(PersistentWorkflow<?> w) {
        try {
            return (RegisterCall)fieldRegisterCall.get(w);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
