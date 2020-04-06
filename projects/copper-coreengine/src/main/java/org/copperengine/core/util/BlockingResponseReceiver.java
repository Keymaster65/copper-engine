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
package org.copperengine.core.util;

public class BlockingResponseReceiver<E> implements AsyncResponseReceiver<E> {

    private E response;
    private Exception exception;
    private boolean responseReceived = false;

    @Override
    public void setException(Exception exception) {
        synchronized (this) {
            if (responseReceived)
                return;
            this.exception = exception;
            this.responseReceived = true;
            this.notifyAll();
        }
    }

    @Override
    public void setResponse(E response) {
        synchronized (this) {
            if (responseReceived)
                return;
            this.response = response;
            this.responseReceived = true;
            this.notifyAll();
        }
    }

    public void wait4response(long timeoutMSec) throws InterruptedException {
        synchronized (this) {
            if (!responseReceived) {
                this.wait(timeoutMSec);
            }
        }
    }

    public boolean isResponseReceived() {
        synchronized (this) {
            return responseReceived;
        }
    }

    public Exception getException() {
        synchronized (this) {
            return exception;
        }
    }

    public E getResponse() {
        synchronized (this) {
            return response;
        }
    }

}
