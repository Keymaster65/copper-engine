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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Collection;

import org.copperengine.core.Acknowledge;
import org.copperengine.core.Response;
import org.copperengine.core.batcher.AbstractBatchCommand;
import org.copperengine.core.batcher.AcknowledgeCallbackWrapper;
import org.copperengine.core.batcher.BatchCommand;
import org.copperengine.core.batcher.BatchExecutor;

class OracleNotifyNoEarlyResponseHandling {

    private static final String SQL =
            "INSERT INTO COP_RESPONSE (CORRELATION_ID,RESPONSE_TS,RESPONSE,LONG_RESPONSE,RESPONSE_TIMEOUT,RESPONSE_META_DATA) " +
                    "SELECT D.* FROM " +
                    "(select correlation_id from COP_WAIT where correlation_id = ?) W, " +
                    "(select ? as correlation_id, ? as response_ts, ? as response, ? as long_response, ? as response_timeout, ? as response_meta_data from dual) D " +
                    "WHERE D.correlation_id = W.correlation_id";

    static final class Command extends AbstractBatchCommand<Executor, Command> {

        final Response<?> response;
        final Serializer serializer;
        final long defaultStaleResponseRemovalTimeout;

        public Command(Response<?> response, Serializer serializer, long defaultStaleResponseRemovalTimeout, final long targetTime, Acknowledge ack) {
            super(new AcknowledgeCallbackWrapper<Command>(ack), targetTime);
            this.response = response;
            this.serializer = serializer;
            this.defaultStaleResponseRemovalTimeout = defaultStaleResponseRemovalTimeout;
        }

        @Override
        public Executor executor() {
            return Executor.INSTANCE;
        }

    }

    static final class Executor extends BatchExecutor<Executor, Command> {

        private static final Executor INSTANCE = new Executor();

        @Override
        public int maximumBatchSize() {
            return 100;
        }

        @Override
        public int preferredBatchSize() {
            return 50;
        }

        @Override
        public void doExec(final Collection<BatchCommand<Executor, Command>> commands, final Connection con) throws Exception {
            final Timestamp now = new Timestamp(System.currentTimeMillis());
            try (final PreparedStatement stmt = con.prepareStatement(SQL)) {
                for (BatchCommand<Executor, Command> _cmd : commands) {
                    Command cmd = (Command) _cmd;
                    stmt.setString(1, cmd.response.getCorrelationId());
                    stmt.setString(2, cmd.response.getCorrelationId());
                    stmt.setTimestamp(3, now);
                    String payload = cmd.serializer.serializeResponse(cmd.response);
                    stmt.setString(4, payload.length() > 4000 ? null : payload);
                    stmt.setString(5, payload.length() > 4000 ? payload : null);
                    stmt.setTimestamp(6, TimeoutProcessor.processTimout(cmd.response.getInternalProcessingTimeout(), cmd.defaultStaleResponseRemovalTimeout));
                    stmt.setString(7, cmd.response.getMetaData());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        }

    }

}
