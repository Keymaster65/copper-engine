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
package org.copperengine.core.persistent.txn;

import javax.sql.DataSource;

import org.copperengine.core.db.utility.RetryingTransaction;

/**
 * Implementation of the {@link TransactionController} interface that internally uses COPPERs
 * {@link RetryingTransaction} for transaction management
 * 
 * @author austermann
 */
public class CopperTransactionController implements TransactionController {

    private DataSource dataSource;
    private int maxConnectRetries = Integer.MAX_VALUE;

    public CopperTransactionController() {
    }

    public CopperTransactionController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setMaxConnectRetries(int maxConnectRetries) {
        this.maxConnectRetries = maxConnectRetries;
    }

    @SuppressWarnings("unchecked")
    public <T> T run(final DatabaseTransaction<T> txn) throws Exception {
        final T[] t = (T[]) new Object[1];
        new RetryingTransaction<Void>(dataSource) {
            @Override
            protected Void execute() throws Exception {
                t[0] = txn.run(getConnection());
                return null;
            }
        }.setMaxConnectRetries(maxConnectRetries).run();
        return t[0];
    }

    @Override
    public <T> T run(Transaction<T> txn) throws Exception {
        return txn.run();
    }
}
