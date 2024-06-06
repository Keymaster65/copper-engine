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
package org.copperengine.regtest.test.persistent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.copperengine.core.Acknowledge;
import org.copperengine.core.DuplicateIdException;
import org.copperengine.core.EngineState;
import org.copperengine.core.PersistentProcessingEngine;
import org.copperengine.core.Response;
import org.copperengine.core.Workflow;
import org.copperengine.core.WorkflowFactory;
import org.copperengine.core.WorkflowInstanceDescr;
import org.copperengine.core.audit.AuditTrailEvent;
import org.copperengine.core.audit.CompressedBase64PostProcessor;
import org.copperengine.core.audit.DummyPostProcessor;
import org.copperengine.core.db.utility.RetryingTransaction;
import org.copperengine.core.persistent.DatabaseDialect;
import org.copperengine.core.persistent.PersistentScottyEngine;
import org.copperengine.core.persistent.ScottyDBStorageInterface;
import org.copperengine.regtest.test.backchannel.BackChannelQueue;
import org.copperengine.regtest.test.backchannel.WorkflowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class BasePersistentWorkflowTest {

    private static final Logger logger = LoggerFactory.getLogger(BasePersistentWorkflowTest.class);

    private static final long DEQUEUE_TIMEOUT = 120;

    static final String PersistentUnitTestWorkflow_NAME = "org.copperengine.regtest.test.persistent.PersistentUnitTestWorkflow";
    static final String WaitForEverTestWF_NAME = "org.copperengine.regtest.test.WaitForEverTestWF";

    public final void testDummy() {
        // for junit only
    }

    protected boolean skipTests() {
        return false;
    }

    void cleanDB(DataSource ds) throws Exception {
        new RetryingTransaction<Void>(ds) {
            @Override
            protected Void execute() throws Exception {
                Statement stmt = createStatement(getConnection());
                stmt.execute("DELETE FROM COP_AUDIT_TRAIL_EVENT");
                stmt.close();
                stmt = createStatement(getConnection());
                stmt.execute("DELETE FROM COP_WAIT");
                stmt.close();
                stmt = createStatement(getConnection());
                stmt.execute("DELETE FROM COP_RESPONSE");
                stmt.close();
                stmt = createStatement(getConnection());
                stmt.execute("DELETE FROM COP_QUEUE");
                stmt.close();
                stmt = createStatement(getConnection());
                stmt.execute("DELETE FROM COP_WORKFLOW_INSTANCE");
                stmt.close();
                stmt = createStatement(getConnection());
                stmt.execute("DELETE FROM COP_WORKFLOW_INSTANCE_ERROR");
                stmt.close();
                stmt = createStatement(getConnection());
                stmt.execute("DELETE FROM COP_LOCK");
                stmt.close();
                return null;
            }
        }.run();
    }

    private Statement createStatement(Connection con) throws SQLException {
        return con.createStatement();
    }

    final String createTestData(int length) {
        StringBuilder dataSB = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int pos = (int) (Math.random() * 70.0);
            dataSB.append("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890!§$%&/()=?".substring(pos, pos + 1));
        }
        return dataSB.toString();
    }

    public void testWaitForEver(String dsContext) throws Exception {
        assumeFalse(skipTests());
        logger.info("running testWaitForEver");
        final ConfigurableApplicationContext context = createContext(dsContext);
        cleanDB(context.getBean(DataSource.class));
        final PersistentScottyEngine engine = context.getBean(PersistentScottyEngine.class);
        engine.startup();
        final BackChannelQueue backChannelQueue = context.getBean(BackChannelQueue.class);
        try {
            assertEquals(EngineState.STARTED, engine.getEngineState());

            final String uuid = UUID.randomUUID().toString();
            engine.run(WaitForEverTestWF_NAME, uuid);

            WorkflowResult x = backChannelQueue.dequeue(5, TimeUnit.SECONDS);
            assertNull(x);

        } finally {
            closeContext(context);
        }
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertEquals(0, engine.getNumberOfWorkflowInstances());
    }

    public void testAsynchResponse(String dsContext) throws Exception {
        assumeFalse(skipTests());
        logger.info("running testAsynchResponse");
        final int NUMB = 50;
        final String DATA = createTestData(50);
        final ConfigurableApplicationContext context = createContext(dsContext);
        cleanDB(context.getBean(DataSource.class));
        final PersistentScottyEngine engine = context.getBean(PersistentScottyEngine.class);
        engine.startup();
        final BackChannelQueue backChannelQueue = context.getBean(BackChannelQueue.class);
        try {
            assertEquals(EngineState.STARTED, engine.getEngineState());

            for (int i = 0; i < NUMB; i++) {
                engine.run(PersistentUnitTestWorkflow_NAME, DATA);
            }

            for (int i = 0; i < NUMB; i++) {
                WorkflowResult x = backChannelQueue.dequeue(DEQUEUE_TIMEOUT, TimeUnit.SECONDS);
                assertNotNull(x);
                assertNotNull(x.getResult());
                assertNotNull(x.getResult().toString().length() == DATA.length());
                assertNull(x.getException());
            }
        } finally {
            closeContext(context);
        }
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertEquals(0, engine.getNumberOfWorkflowInstances());

    }

    public void testFailOnDuplicateInsert(String dsContext) throws Exception {
        assumeFalse(skipTests());
        logger.info("running testFailOnDuplicateInsert");
        final String DATA = createTestData(50);
        final ConfigurableApplicationContext context = createContext(dsContext);
        cleanDB(context.getBean(DataSource.class));
        context.getBean(DatabaseDialect.class).setRemoveWhenFinished(false);
        final PersistentScottyEngine engine = context.getBean(PersistentScottyEngine.class);
        engine.startup();
        try {
            WorkflowInstanceDescr<String> desc = new WorkflowInstanceDescr<String>(PersistentUnitTestWorkflow_NAME, DATA, "DUPLICATE#ID", 1, null);
            engine.run(desc);
            engine.run(desc);
            org.junit.Assert.fail("expected an DuplicateIdException");
        } catch (DuplicateIdException e) {
            // ok
        } finally {
            closeContext(context);
        }
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertTrue(engine.getNumberOfWorkflowInstances() <= 1);

    }

    protected void closeContext(final ConfigurableApplicationContext context) {
        context.close();
    }

    public void testAsynchResponseLargeData(String dsContext, int dataSize) throws Exception {
        assumeFalse(skipTests());
        logger.info("running testAsynchResponse");
        final int NUMB = 20;
        final String DATA = createTestData(dataSize);
        final ConfigurableApplicationContext context = createContext(dsContext);
        cleanDB(context.getBean(DataSource.class));
        final PersistentScottyEngine engine = context.getBean(PersistentScottyEngine.class);
        engine.startup();
        final BackChannelQueue backChannelQueue = context.getBean(BackChannelQueue.class);
        try {
            assertEquals(EngineState.STARTED, engine.getEngineState());

            for (int i = 0; i < NUMB; i++) {
                engine.run(PersistentUnitTestWorkflow_NAME, DATA);
            }

            for (int i = 0; i < NUMB; i++) {
                WorkflowResult x = backChannelQueue.dequeue(DEQUEUE_TIMEOUT, TimeUnit.SECONDS);
                assertNotNull(x);
                assertNotNull(x.getResult());
                assertNotNull(x.getResult().toString().length() == DATA.length());
                assertNull(x.getException());
            }
        } finally {
            closeContext(context);
        }
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertEquals(0, engine.getNumberOfWorkflowInstances());

    }

    protected ConfigurableApplicationContext createContext(String dsContext) {
        final ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(new String[] { dsContext, "/CopperTxnPersistentWorkflowTest/persistent-engine-unittest-context.xml" });
        return context;
    }

    public void testWithConnection(String dsContext) throws Exception {
        assumeFalse(skipTests());
        logger.info("running testWithConnection");
        final int NUMB = 20;
        final ConfigurableApplicationContext context = createContext(dsContext);
        final DataSource ds = context.getBean(DataSource.class);
        cleanDB(ds);
        final PersistentScottyEngine engine = context.getBean(PersistentScottyEngine.class);
        engine.startup();
        final BackChannelQueue backChannelQueue = context.getBean(BackChannelQueue.class);
        try {
            assertEquals(EngineState.STARTED, engine.getEngineState());

            new RetryingTransaction<Void>(ds) {
                @Override
                protected Void execute() throws Exception {
                    for (int i = 0; i < NUMB; i++) {
                        engine.run(new WorkflowInstanceDescr<Serializable>("org.copperengine.regtest.test.persistent.DBMockAdapterUsingPersistentUnitTestWorkflow"), getConnection());
                    }
                    return null;
                }
            }.run();

            for (int i = 0; i < NUMB; i++) {
                WorkflowResult x = backChannelQueue.dequeue(DEQUEUE_TIMEOUT, TimeUnit.SECONDS);
                assertNotNull(x);
                assertNull(x.getResult());
                assertNull(x.getException());
            }
        } finally {
            closeContext(context);
        }
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertEquals(0, engine.getNumberOfWorkflowInstances());

    }

    public void testWithConnectionBulkInsert(String dsContext) throws Exception {
        assumeFalse(skipTests());
        logger.info("running testWithConnectionBulkInsert");
        final int NUMB = 50;
        final ConfigurableApplicationContext context = createContext(dsContext);
        final DataSource ds = context.getBean(DataSource.class);
        cleanDB(ds);
        final PersistentScottyEngine engine = context.getBean(PersistentScottyEngine.class);
        engine.startup();
        final BackChannelQueue backChannelQueue = context.getBean(BackChannelQueue.class);
        try {
            assertEquals(EngineState.STARTED, engine.getEngineState());

            final List<Workflow<?>> list = new ArrayList<Workflow<?>>();
            for (int i = 0; i < NUMB; i++) {
                WorkflowFactory<?> wfFactory = engine.createWorkflowFactory(PersistentUnitTestWorkflow_NAME);
                Workflow<?> wf = wfFactory.newInstance();
                list.add(wf);
            }

            new RetryingTransaction<Void>(ds) {
                @Override
                protected Void execute() throws Exception {
                    engine.run(list, getConnection());
                    return null;
                }
            }.run();

            for (int i = 0; i < NUMB; i++) {
                WorkflowResult x = backChannelQueue.dequeue(DEQUEUE_TIMEOUT, TimeUnit.SECONDS);
                assertNotNull(x);
                assertNull(x.getResult());
                assertNull(x.getException());
            }
        } finally {
            closeContext(context);
        }
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertEquals(0, engine.getNumberOfWorkflowInstances());

    }

    public void testTimeouts(String dsContext) throws Exception {
        assumeFalse(skipTests());
        logger.info("running testTimeouts");
        final int NUMB = 10;
        final ConfigurableApplicationContext context = createContext(dsContext);
        cleanDB(context.getBean(DataSource.class));
        final PersistentScottyEngine engine = context.getBean(PersistentScottyEngine.class);
        engine.startup();
        final BackChannelQueue backChannelQueue = context.getBean(BackChannelQueue.class);
        try {
            assertEquals(EngineState.STARTED, engine.getEngineState());

            for (int i = 0; i < NUMB; i++) {
                engine.run("org.copperengine.regtest.test.persistent.TimingOutPersistentUnitTestWorkflow", null);
            }

            for (int i = 0; i < NUMB; i++) {
                WorkflowResult x = backChannelQueue.dequeue(DEQUEUE_TIMEOUT, TimeUnit.SECONDS);
                assertNotNull(x);
                assertNull(x.getResult());
                assertNull(x.getException());
            }
        } finally {
            closeContext(context);
        }
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertEquals(0, engine.getNumberOfWorkflowInstances());

    }

    public void testErrorHandlingInCoreEngine(String dsContext) throws Exception {
        assumeFalse(skipTests());
        final ConfigurableApplicationContext context = createContext(dsContext);
        cleanDB(context.getBean(DataSource.class));
        final PersistentScottyEngine engine = context.getBean(PersistentScottyEngine.class);
        try {
            engine.startup();
            final WorkflowInstanceDescr<Serializable> wfInstanceDescr = new WorkflowInstanceDescr<Serializable>("org.copperengine.regtest.test.persistent.ExceptionThrowingPersistentUnitTestWorkflow");
            wfInstanceDescr.setId(engine.createUUID());
            engine.run(wfInstanceDescr);
            Thread.sleep(5000);
            // check
            new RetryingTransaction<Void>(context.getBean(DataSource.class)) {
                @Override
                protected Void execute() throws Exception {
                    Statement stmt = createStatement(getConnection());
                    ResultSet rs = stmt.executeQuery("select * from COP_WORKFLOW_INSTANCE_ERROR");
                    assertTrue(rs.next());
                    assertEquals(wfInstanceDescr.getId(), rs.getString("WORKFLOW_INSTANCE_ID"));
                    assertNotNull(rs.getString("EXCEPTION"));
                    assertFalse(rs.next());
                    rs.close();
                    stmt.close();
                    return null;
                }
            }.run();
            engine.restart(wfInstanceDescr.getId());
            Thread.sleep(5000);
            new RetryingTransaction<Void>(context.getBean(DataSource.class)) {
                @Override
                protected Void execute() throws Exception {
                    Statement stmt = createStatement(getConnection());
                    ResultSet rs = stmt.executeQuery("select * from COP_WORKFLOW_INSTANCE_ERROR");
                    assertTrue(rs.next());
                    assertEquals(wfInstanceDescr.getId(), rs.getString("WORKFLOW_INSTANCE_ID"));
                    assertNotNull(rs.getString("EXCEPTION"));
                    assertTrue(rs.next());
                    assertEquals(wfInstanceDescr.getId(), rs.getString("WORKFLOW_INSTANCE_ID"));
                    assertNotNull(rs.getString("EXCEPTION"));
                    assertFalse(rs.next());
                    rs.close();
                    stmt.close();
                    return null;
                }
            }.run();
        } finally {
            closeContext(context);
        }
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertEquals(0, engine.getNumberOfWorkflowInstances());
    }

    public void testErrorHandlingInCoreEngine_restartAll(String dsContext) throws Exception {
        assumeFalse(skipTests());
        final ConfigurableApplicationContext context = createContext(dsContext);
        cleanDB(context.getBean(DataSource.class));
        final PersistentScottyEngine engine = context.getBean(PersistentScottyEngine.class);
        try {
            engine.startup();
            final WorkflowInstanceDescr<Serializable> wfInstanceDescr = new WorkflowInstanceDescr<Serializable>("org.copperengine.regtest.test.persistent.ExceptionThrowingPersistentUnitTestWorkflow");
            wfInstanceDescr.setId(engine.createUUID());
            engine.run(wfInstanceDescr);
            Thread.sleep(5000);
            // check
            new RetryingTransaction<Void>(context.getBean(DataSource.class)) {
                @Override
                protected Void execute() throws Exception {
                    Statement stmt = createStatement(getConnection());
                    ResultSet rs = stmt.executeQuery("select * from COP_WORKFLOW_INSTANCE_ERROR");
                    assertTrue(rs.next());
                    assertEquals(wfInstanceDescr.getId(), rs.getString("WORKFLOW_INSTANCE_ID"));
                    assertNotNull(rs.getString("EXCEPTION"));
                    assertFalse(rs.next());
                    rs.close();
                    stmt.close();
                    return null;
                }
            }.run();
            engine.restartAll();
            Thread.sleep(5000);
            new RetryingTransaction<Void>(context.getBean(DataSource.class)) {
                @Override
                protected Void execute() throws Exception {
                    Statement stmt = createStatement(getConnection());
                    ResultSet rs = stmt.executeQuery("select * from COP_WORKFLOW_INSTANCE_ERROR");
                    assertTrue(rs.next());
                    assertEquals(wfInstanceDescr.getId(), rs.getString("WORKFLOW_INSTANCE_ID"));
                    assertNotNull(rs.getString("EXCEPTION"));
                    assertTrue(rs.next());
                    assertEquals(wfInstanceDescr.getId(), rs.getString("WORKFLOW_INSTANCE_ID"));
                    assertNotNull(rs.getString("EXCEPTION"));
                    assertFalse(rs.next());
                    rs.close();
                    stmt.close();
                    return null;
                }
            }.run();
        } finally {
            closeContext(context);
        }
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertEquals(0, engine.getNumberOfWorkflowInstances());
    }

    public void testParentChildWorkflow(String dsContext) throws Exception {
        assumeFalse(skipTests());
        logger.info("running testParentChildWorkflow");
        final int NUMB = 20;
        final ConfigurableApplicationContext context = createContext(dsContext);
        cleanDB(context.getBean(DataSource.class));
        final PersistentScottyEngine engine = context.getBean(PersistentScottyEngine.class);
        engine.startup();
        final BackChannelQueue backChannelQueue = context.getBean(BackChannelQueue.class);
        try {
            assertEquals(EngineState.STARTED, engine.getEngineState());

            for (int i = 0; i < NUMB; i++) {
                engine.run("org.copperengine.regtest.test.persistent.subworkflow.TestParentWorkflow", null);
            }

            for (int i = 0; i < NUMB; i++) {
                WorkflowResult x = backChannelQueue.dequeue(DEQUEUE_TIMEOUT, TimeUnit.SECONDS);
                assertNotNull(x);
                assertNull(x.getResult());
                assertNull(x.getException());
            }
        } finally {
            closeContext(context);
        }
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertEquals(0, engine.getNumberOfWorkflowInstances());
    }

    public void testErrorKeepWorkflowInstanceInDB(String dsContext) throws Exception {
        assumeFalse(skipTests());
        logger.info("running testErrorKeepWorkflowInstanceInDB");
        final int NUMB = 20;
        final String DATA = createTestData(50);
        final ConfigurableApplicationContext context = createContext(dsContext);
        cleanDB(context.getBean(DataSource.class));
        final PersistentScottyEngine engine = context.getBean(PersistentScottyEngine.class);
        final ScottyDBStorageInterface dbStorageInterface = context.getBean(ScottyDBStorageInterface.class);
        dbStorageInterface.setRemoveWhenFinished(false);
        engine.startup();
        final BackChannelQueue backChannelQueue = context.getBean(BackChannelQueue.class);
        try {
            assertEquals(EngineState.STARTED, engine.getEngineState());

            for (int i = 0; i < NUMB; i++) {
                engine.run(PersistentUnitTestWorkflow_NAME, DATA);
            }

            for (int i = 0; i < NUMB; i++) {
                WorkflowResult x = backChannelQueue.dequeue(DEQUEUE_TIMEOUT, TimeUnit.SECONDS);
                assertNotNull(x);
                assertNotNull(x.getResult());
                assertNotNull(x.getResult().toString().length() == DATA.length());
                assertNull(x.getException());
            }

            new RetryingTransaction<Void>(context.getBean(DataSource.class)) {
                @Override
                protected Void execute() throws Exception {
                    Statement stmt = createStatement(getConnection());
                    ResultSet rs = stmt.executeQuery("select count(*) from COP_WORKFLOW_INSTANCE");
                    assertTrue(rs.next());
                    int x = rs.getInt(1);
                    assertEquals(NUMB, x);
                    rs.close();
                    stmt.close();
                    return null;
                }
            }.run();

        } finally {
            closeContext(context);
        }
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertEquals(0, engine.getNumberOfWorkflowInstances());
    }

    public void testAutoCommit(String dsContext) throws Exception {
        assumeFalse(skipTests());
        logger.info("running testAutoCommit");
        final ConfigurableApplicationContext context = createContext(dsContext);
        try {
            DataSource ds = context.getBean(DataSource.class);
            new RetryingTransaction<Void>(ds) {
                @Override
                protected Void execute() throws Exception {
                    assertFalse(getConnection().getAutoCommit());
                    return null;
                }
            };
        } finally {
            closeContext(context);
        }
    }

    private static String createTestMessage(int size) {
        final StringBuilder sb = new StringBuilder(4000);
        for (int i = 0; i < (size / 10); i++) {
            sb.append("0123456789");
        }
        final String msg = sb.toString();
        return msg;
    }

    public void testErrorHandlingWithWaitHook(String dsContext) throws Exception {
        assumeFalse(skipTests());
        final ConfigurableApplicationContext context = createContext(dsContext);
        cleanDB(context.getBean(DataSource.class));
        final PersistentScottyEngine engine = context.getBean(PersistentScottyEngine.class);
        try {
            engine.startup();
            final WorkflowInstanceDescr<Serializable> wfInstanceDescr = new WorkflowInstanceDescr<Serializable>("org.copperengine.regtest.test.persistent.ErrorWaitHookUnitTestWorkflow");
            wfInstanceDescr.setId(engine.createUUID());
            engine.run(wfInstanceDescr, null);
            Thread.sleep(3500);
            // check
            new RetryingTransaction<Void>(context.getBean(DataSource.class)) {
                @Override
                protected Void execute() throws Exception {
                    Statement stmt = createStatement(getConnection());
                    ResultSet rs = stmt.executeQuery("select * from COP_WORKFLOW_INSTANCE_ERROR");
                    assertTrue(rs.next());
                    assertEquals(wfInstanceDescr.getId(), rs.getString("WORKFLOW_INSTANCE_ID"));
                    assertNotNull(rs.getString("EXCEPTION"));
                    assertFalse(rs.next());
                    rs.close();
                    stmt.close();
                    return null;
                }
            }.run();
        } finally {
            closeContext(context);
        }
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertEquals(0, engine.getNumberOfWorkflowInstances());
    }

    public void testNotifyWithoutEarlyResponseHandling(String dsContext) throws Exception {
        assumeFalse(skipTests());
        logger.info("running testNotifyWithoutEarlyResponseHandling");
        final ConfigurableApplicationContext context = createContext(dsContext);
        cleanDB(context.getBean(DataSource.class));
        final PersistentScottyEngine engine = context.getBean(PersistentScottyEngine.class);
        try {
            engine.startup();
            new RetryingTransaction<Void>(context.getBean(DataSource.class)) {
                @Override
                protected Void execute() throws Exception {
                    try {
                        Response<?> response = new Response<String>("CID#withEarlyResponse", "TEST", null);
                        engine.notify(response, getConnection());
                        Statement stmt = createStatement(getConnection());
                        ResultSet rs = stmt.executeQuery("select * from COP_RESPONSE");
                        assertTrue(rs.next());
                        assertEquals(response.getCorrelationId(), rs.getString("CORRELATION_ID"));
                        assertFalse(rs.next());
                        getConnection().rollback();

                        response = new Response<String>("CID#withoutEarlyResponse", "TEST", null);
                        response.setEarlyResponseHandling(false);
                        engine.notify(response, getConnection());
                        rs = stmt.executeQuery("select * from COP_RESPONSE");
                        assertFalse(rs.next());
                        rs.close();
                        stmt.close();
                        getConnection().rollback();
                    } catch (Exception e) {
                        logger.error("testNotifyWithoutEarlyResponseHandling failed", e);
                        throw e;
                    }
                    return null;
                }
            }.run();
        } finally {
            closeContext(context);
        }
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertEquals(0, engine.getNumberOfWorkflowInstances());

    }

    public void testQueryAllActive(String dsContext) throws Exception {
        assumeFalse(skipTests());
        final ConfigurableApplicationContext context = createContext(dsContext);
        cleanDB(context.getBean(DataSource.class));
        final PersistentScottyEngine engine = context.getBean(PersistentScottyEngine.class);
        try {
            engine.startup();
            // just check, that the underlying SQL statements are ok.
            assertEquals(0, engine.queryActiveWorkflowInstances(null, 100).size());
            assertEquals(0, engine.queryActiveWorkflowInstances("foo.john.Doe", 100).size());
        } finally {
            closeContext(context);
        }
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertEquals(0, engine.getNumberOfWorkflowInstances());
    }

    private <T> void manualSend(PersistentProcessingEngine engine, String correlationId, T data) {
        Response<T> response = new Response<>(correlationId, data, null);
        response.setResponseId(UUID.randomUUID().toString());
        System.out.println("manualSend: " + response.getResponseId());
        Acknowledge.DefaultAcknowledge ack = new Acknowledge.DefaultAcknowledge();
        engine.notify(response, ack);
        ack.waitForAcknowledge();

    }

    public void testMulipleResponsesForSameCidPersistentTestWorkflow(String dsContext) throws Exception {
        assumeFalse(skipTests());
        final ConfigurableApplicationContext context = createContext(dsContext);
        cleanDB(context.getBean(DataSource.class));
        final PersistentScottyEngine engine = context.getBean(PersistentScottyEngine.class);
        try {
            engine.startup();
            String cid = "testSingleCID";
            try
            {
                engine.run("MulipleResponsesForSameCidPersistentTestWorkflow", cid);
                Thread.sleep(1000); // wait for it to start up
                for (int i = 1; i <= 9; i++)
                {
                    manualSend(engine, cid, "Response#" + i);
                }
                manualSend(engine, cid, "GG");
                Thread.sleep(1000);
            } catch (Exception e)
            {
                e.printStackTrace();
            }

        } finally {
            closeContext(context);
        }
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertEquals(0, engine.getNumberOfWorkflowInstances());
    }
}
