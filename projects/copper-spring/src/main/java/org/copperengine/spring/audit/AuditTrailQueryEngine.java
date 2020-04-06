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
package org.copperengine.spring.audit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.copperengine.core.audit.MessagePostProcessor;
import org.copperengine.management.AuditTrailQueryMXBean;
import org.copperengine.management.model.AuditTrailInfo;
import org.copperengine.management.model.AuditTrailInstanceFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.util.StringUtils;

// Use org.copperengine.core.audit.ScottyAuditTrailQueryEngine instead
@Deprecated
public class AuditTrailQueryEngine extends JdbcDaoSupport implements AuditTrailQueryMXBean {
    private static final Logger logger = LoggerFactory.getLogger(AuditTrailQueryEngine.class);

    private MessagePostProcessor messagePostProcessor;

    @Override
    public List<AuditTrailInfo> getAuditTrails(String transactionId, String conversationId, String correlationId, Integer level, int maxResult) {

        SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();

        String sortClause = "SEQ_ID";
        String whereClause = "where 1=1 ";
        List<Object> args = new ArrayList<Object>();

        if (level != null) {
            whereClause += " and LOGLEVEL <= ? ";
            sortClause = "LOGLEVEL";
            args.add(level);
        }
        if (StringUtils.hasText(correlationId)) {
            whereClause += " and CORRELATION_ID = ? ";
            sortClause = "CORRELATION_ID";
            args.add(correlationId);
        }

        if (StringUtils.hasText(conversationId)) {
            whereClause += " and CONVERSATION_ID = ? ";
            sortClause = "CONVERSATION_ID";
            args.add(conversationId);
        }

        if (StringUtils.hasText(transactionId)) {
            whereClause += " and TRANSACTION_ID = ? ";
            sortClause = "TRANSACTION_ID";
            args.add(transactionId);
        }

        String selectClause = "select "
                + "SEQ_ID,"
                + "TRANSACTION_ID,"
                + "CONVERSATION_ID,"
                + "CORRELATION_ID,"
                + "OCCURRENCE,"
                + "LOGLEVEL,"
                + "CONTEXT,"
                + "INSTANCE_ID,"
                + "MESSAGE_TYPE";

        factory.setDataSource(getDataSource());
        factory.setFromClause("from COP_AUDIT_TRAIL_EVENT ");

        factory.setSelectClause(selectClause);

        factory.setWhereClause(whereClause);
        factory.setSortKey(sortClause);

        PagingQueryProvider queryProvider;
        try {
            queryProvider = (PagingQueryProvider) factory.getObject();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
        if(queryProvider == null) return null;
        String query = queryProvider.generateFirstPageQuery(maxResult);

        // this.getJdbcTemplate().setQueryTimeout(1000);

        long start = System.currentTimeMillis();
        RowMapper<AuditTrailInfo> rowMapper = new RowMapper<AuditTrailInfo>() {

            public AuditTrailInfo mapRow(ResultSet rs, int arg1)
                    throws SQLException {

                return new AuditTrailInfo(
                        rs.getLong("SEQ_ID"),
                        rs.getString("TRANSACTION_ID"),
                        rs.getString("CONVERSATION_ID"),
                        rs.getString("CORRELATION_ID"),
                        rs.getTimestamp("OCCURRENCE").getTime(),
                        rs.getInt("LOGLEVEL"),
                        rs.getString("CONTEXT"),
                        rs.getString("INSTANCE_ID"),
                        rs.getString("MESSAGE_TYPE")
                );
            }

        };
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        List<AuditTrailInfo> res = (jdbcTemplate != null)
                ? jdbcTemplate.query(query, rowMapper, args.toArray())
                : Collections.emptyList();

        long end = System.currentTimeMillis();

        logger.info("query took: " + (end - start) + " ms : " + query);

        return res;
    }

    @Deprecated
    public byte[] getMessage(long id) {
        String customSelect = "select LONG_MESSAGE from COP_AUDIT_TRAIL_EVENT where SEQ_ID = ? ";

        ResultSetExtractor<byte[]> rse = new ResultSetExtractor<byte[]>() {

            @Override
            public byte[] extractData(ResultSet rs) throws SQLException,
                    DataAccessException {
                rs.next();
                return convertToArray(rs.getBinaryStream("LONG_MESSAGE"));
            }

        };

        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        return (jdbcTemplate != null) ? jdbcTemplate.query(customSelect, rse, new Object[] { id }) : null;
    }

    @Override
    public List<AuditTrailInfo> getAuditTrails(AuditTrailInstanceFilter filter) {
        throw new UnsupportedOperationException("Not supported. Use org.copperengine.core.audit.ScottyAuditTrailQueryEngine instead");
    }

    @Override
    public int countAuditTrails(AuditTrailInstanceFilter filter) {
        throw new UnsupportedOperationException("Not supported. Use org.copperengine.core.audit.ScottyAuditTrailQueryEngine instead");
    }

    public String getMessageString(long id) {
        if (messagePostProcessor == null) {
            throw new NullPointerException("Message Post Processor is not set. use byte[] getMessage(long id) method or set Message Post Processor");
        }

        String customSelect = "select LONG_MESSAGE from COP_AUDIT_TRAIL_EVENT where SEQ_ID = ? ";
        ResultSetExtractor<String> rse = new ResultSetExtractor<String>() {

            @Override
            public String extractData(ResultSet rs) throws SQLException,
                    DataAccessException {
                rs.next();
                Clob message = rs.getClob("LONG_MESSAGE");

                if ((int) message.length() == 0) {
                    return null;
                }

                return messagePostProcessor.deserialize(message.getSubString(1, (int) message.length()));
            }
        };

        JdbcTemplate jdbcTemplate = getJdbcTemplate();

        return (jdbcTemplate != null) ? jdbcTemplate.query(customSelect, rse, new Object[] { id }) : null;
    }

    private byte[] convertToArray(InputStream messageStream) {
        if (messageStream == null) {
            return new byte[0];
        }

        byte[] bytes = new byte[1024];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int read = 0;
        int off = 0;
        try {
            while ((read = messageStream.read(bytes)) > 0) {
                out.write(bytes, off, read);
                off += read;
            }
            messageStream.close();
            return out.toByteArray();
        } catch (IOException e) {
        }
        return null;
    }

    public MessagePostProcessor getMessagePostProcessor() {
        return messagePostProcessor;
    }

    public void setMessagePostProcessor(MessagePostProcessor messagePostProcessor) {
        this.messagePostProcessor = messagePostProcessor;
    }
}
