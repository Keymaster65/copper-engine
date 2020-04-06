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
package org.copperengine.core.persistent;

import org.copperengine.core.ProcessingState;

public enum DBProcessingState {
    ENQUEUED,
    PROCESSING /* so far unused */,
    WAITING,
    FINISHED,
    INVALID,
    ERROR;

    public static DBProcessingState getByOrdinal(int ordinal) {
        return values()[ordinal];
    }

    public static ProcessingState getProcessingStateByState(DBProcessingState state){
        ProcessingState processingState;
        switch (state) {
            case ENQUEUED:
                processingState = ProcessingState.ENQUEUED;
                break;
            case PROCESSING:
                processingState = ProcessingState.RUNNING;
                break;
            case WAITING:
                processingState = ProcessingState.WAITING;
                break;
            case FINISHED:
                processingState = ProcessingState.FINISHED;
                break;
            case INVALID:
                processingState = ProcessingState.INVALID;
                break;
            case ERROR:
                processingState = ProcessingState.ERROR;
                break;
            default:
                processingState = ProcessingState.RAW;
        }
        return processingState;
    }

}