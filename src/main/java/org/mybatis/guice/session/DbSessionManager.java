/*
 *    Copyright 2010-2012 The MyBatis Team
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.guice.session;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.*;
import org.mybatis.guice.transactional.MultiTransactionManager;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * Variant of the {@code SqlSessionManager} from MyBatis.
 *
 * Since the class uses the factory pattern inherited override is not possible to provide
 * the extra functionality needed for the MultiMapper
 *
 * The main difference is how transactions are created and closed.
 *
 * {@code SqlSessionInterceptor} uses a slightly different approach now:
 *
 *
 * @author ferenczil
 */
public class DbSessionManager implements SqlSessionFactory, SqlSession {
    private static final Logger log = LoggerFactory.getLogger(DbSessionManager.class);

    private final SqlSessionFactory sqlSessionFactory;
    private final SqlSession sqlSessionProxy;

    private ThreadLocal<SqlSession> localSqlSession = new ThreadLocal<>();

    @Inject @Named("mybatis.configuration.allowTransactionWithoutContext")
    boolean allowTransactionWithoutContext;

    String environmentId;
    MultiTransactionManager txManager;

    @Inject
    public DbSessionManager(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.sqlSessionProxy = (SqlSession) Proxy.newProxyInstance(
                SqlSessionFactory.class.getClassLoader(),
                new Class[]{SqlSession.class},
                new SqlSessionInterceptor());
    }

    @Inject
    public void setTxManager(@Named("mybatis.environment.id") String environmentId, MultiTransactionManager txManager) {
        this.environmentId = environmentId;
        this.txManager = txManager;
        txManager.register(environmentId, this);
    }

    public void startManagedSession() {
        this.localSqlSession.set(openSession());
    }

    public void startManagedSession(boolean autoCommit) {
        this.localSqlSession.set(openSession(autoCommit));
    }

    public void startManagedSession(Connection connection) {
        this.localSqlSession.set(openSession(connection));
    }

    public void startManagedSession(TransactionIsolationLevel level) {
        this.localSqlSession.set(openSession(level));
    }

    public void startManagedSession(ExecutorType execType) {
        this.localSqlSession.set(openSession(execType));
    }

    public void startManagedSession(ExecutorType execType, boolean autoCommit) {
        this.localSqlSession.set(openSession(execType, autoCommit));
    }

    public void startManagedSession(ExecutorType execType, TransactionIsolationLevel level) {
        this.localSqlSession.set(openSession(execType, level));
    }

    public void startManagedSession(ExecutorType execType, Connection connection) {
        this.localSqlSession.set(openSession(execType, connection));
    }

    public boolean isManagedSessionStarted() {
        return this.localSqlSession.get() != null;
    }

    public SqlSession openSession() {
        return sqlSessionFactory.openSession();
    }

    public SqlSession openSession(boolean autoCommit) {
        return sqlSessionFactory.openSession(autoCommit);
    }

    public SqlSession openSession(Connection connection) {
        return sqlSessionFactory.openSession(connection);
    }

    public SqlSession openSession(TransactionIsolationLevel level) {
        return sqlSessionFactory.openSession(level);
    }

    public SqlSession openSession(ExecutorType execType) {
        return sqlSessionFactory.openSession(execType);
    }

    public SqlSession openSession(ExecutorType execType, boolean autoCommit) {
        return sqlSessionFactory.openSession(execType, autoCommit);
    }

    public SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
        return sqlSessionFactory.openSession(execType, level);
    }

    public SqlSession openSession(ExecutorType execType, Connection connection) {
        return sqlSessionFactory.openSession(execType, connection);
    }

    public Configuration getConfiguration() {
        return sqlSessionFactory.getConfiguration();
    }

    public <T> T selectOne(String statement) {
        return sqlSessionProxy.<T> selectOne(statement);
    }

    public <T> T selectOne(String statement, Object parameter) {
        return sqlSessionProxy.<T> selectOne(statement, parameter);
    }

    public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
        return sqlSessionProxy.<K, V> selectMap(statement, mapKey);
    }

    public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
        return sqlSessionProxy.<K, V> selectMap(statement, parameter, mapKey);
    }

    public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
        return sqlSessionProxy.<K, V> selectMap(statement, parameter, mapKey, rowBounds);
    }

    public <E> List<E> selectList(String statement) {
        return sqlSessionProxy.<E> selectList(statement);
    }

    public <E> List<E> selectList(String statement, Object parameter) {
        return sqlSessionProxy.<E> selectList(statement, parameter);
    }

    public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
        return sqlSessionProxy.<E> selectList(statement, parameter, rowBounds);
    }

    public void select(String statement, ResultHandler handler) {
        sqlSessionProxy.select(statement, handler);
    }

    public void select(String statement, Object parameter, ResultHandler handler) {
        sqlSessionProxy.select(statement, parameter, handler);
    }

    public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
        sqlSessionProxy.select(statement, parameter, rowBounds, handler);
    }

    public int insert(String statement) {
        return sqlSessionProxy.insert(statement);
    }

    public int insert(String statement, Object parameter) {
        return sqlSessionProxy.insert(statement, parameter);
    }

    public int update(String statement) {
        return sqlSessionProxy.update(statement);
    }

    public int update(String statement, Object parameter) {
        return sqlSessionProxy.update(statement, parameter);
    }

    public int delete(String statement) {
        return sqlSessionProxy.delete(statement);
    }

    public int delete(String statement, Object parameter) {
        return sqlSessionProxy.delete(statement, parameter);
    }

    public <T> T getMapper(Class<T> type) {
        return getConfiguration().getMapper(type, this);
    }

    public Connection getConnection() {
        final SqlSession sqlSession = localSqlSession.get();
        if (sqlSession == null) throw new SqlSessionException("Error:  Cannot get connection.  No managed session is started.");
        return sqlSession.getConnection();
    }

    public void clearCache() {
        final SqlSession sqlSession = localSqlSession.get();
        if (sqlSession == null) throw new SqlSessionException("Error:  Cannot clear the cache.  No managed session is started.");
        sqlSession.clearCache();
    }

    public void commit() {
        final SqlSession sqlSession = localSqlSession.get();
        if (sqlSession == null) throw new SqlSessionException("Error:  Cannot commit.  No managed session is started.");
        sqlSession.commit();
    }

    public void commit(boolean force) {
        final SqlSession sqlSession = localSqlSession.get();
        if (sqlSession == null) throw new SqlSessionException("Error:  Cannot commit.  No managed session is started.");
        sqlSession.commit(force);
    }

    public void rollback() {
        final SqlSession sqlSession = localSqlSession.get();
        if (sqlSession == null) throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
        sqlSession.rollback();
    }

    public void rollback(boolean force) {
        final SqlSession sqlSession = localSqlSession.get();
        if (sqlSession == null) throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
        sqlSession.rollback(force);
    }

    public List<BatchResult> flushStatements() {
        final SqlSession sqlSession = localSqlSession.get();
        if (sqlSession == null) throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
        return sqlSession.flushStatements();
    }

    public void close() {
        final SqlSession sqlSession = localSqlSession.get();
        if (sqlSession == null) throw new SqlSessionException("Error:  Cannot close.  No managed session is started.");
        try {
            sqlSession.close();
        } finally {
            localSqlSession.set(null);
        }
    }

    private class SqlSessionInterceptor implements InvocationHandler {
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            Transactional tx = txManager.getContext();
            log.debug("Transactional context: {}", tx);
            if (null != tx) {

                // Creating sessions on demand
                if (DbSessionManager.this.localSqlSession.get() == null) {
                    log.debug("Starting managed session for environment: {}", environmentId);
                    DbSessionManager.this.startManagedSession(tx.executorType(), tx.isolation().getTransactionIsolationLevel());
                }
                try {
                    return method.invoke(DbSessionManager.this.localSqlSession.get(), args);
                }
                catch (Throwable t) {
                    throw ExceptionUtil.unwrapThrowable(t);
                }
            }
            else {
                if (allowTransactionWithoutContext) {
                    log.warn("No transactional context, starting one anyway");
                    final SqlSession autoSqlSession = openSession();
                    try {
                        final Object result = method.invoke(autoSqlSession, args);
                        autoSqlSession.commit();
                        return result;
                    }
                    catch (Throwable t) {
                        autoSqlSession.rollback();
                        throw ExceptionUtil.unwrapThrowable(t);
                    }
                    finally {
                        autoSqlSession.close();
                        log.debug("Auto-session closed");
                    }
                }
                else {
                    throw new SqlSessionException("Trying to execute transaction without a context in environment: " + environmentId);
                }
            }
        }
    }

}
