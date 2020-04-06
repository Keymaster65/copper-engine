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
package org.copperengine.regtest.test;

import java.io.Serializable;

import org.copperengine.core.Interrupt;
import org.copperengine.core.Response;
import org.copperengine.core.WaitMode;
import org.copperengine.core.persistent.PersistentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IssueClassCastExceptionWorkflow3<Data extends Serializable> extends PersistentWorkflow<Serializable> {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(IssueClassCastExceptionWorkflow3.class);

    private int retriesLeft = 5;

    @Override
    public void main() throws Interrupt {
        this.callPartner(20);

    }

    protected void exceptionSimulation() throws Interrupt {
        wait(WaitMode.FIRST, 20, "x");
        throw new RuntimeException("Simulate exception.");
    }

    protected void callPartner(int theWaitInterval) throws Interrupt {
        logger.warn("Start " + this.getClass().getName());
        boolean retryInterrupted = false;
        while (!retryInterrupted && retriesLeft > 0) {
            retryInterrupted = withCatch(theWaitInterval);
        }
    }

    private boolean withCatch(int theWaitInterval) throws Interrupt {
        try {
            exceptionSimulation();
            return false;
        } catch (Exception e) {
            logger.warn("Handle exception: " + e);
            return waitForNetRetry(theWaitInterval);
        }
    }

    private boolean waitForNetRetry(int theWaitInterval) throws Interrupt {
        boolean interupted = false;
        if (retriesLeft > 0) {
            retriesLeft--;
            String correlationID = "RETRY-" + this.getEngine().createUUID();
            wait(WaitMode.FIRST, theWaitInterval, correlationID);
            Response<String> r = getAndRemoveResponse(correlationID);
            if (logger.isInfoEnabled())
                logger.info("Response for " + correlationID + ": " + r);
            if (!r.isTimeout()) {
                if (logger.isInfoEnabled())
                    logger.info("Receiver no TIMEOUT while retring, so must be INTERRUPT_RETRY.");
                interupted = true;
            }
        }
        return interupted;
    }
}