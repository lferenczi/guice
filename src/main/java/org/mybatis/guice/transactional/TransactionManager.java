package org.mybatis.guice.transactional;

import com.google.inject.Inject;
import com.google.inject.ProvisionException;

/**
 * Helper class for manual transaction management.
 * Inject it and call the appropriate methods to start/stop the transactional context
 *
 * @author ferenczil
 */
public class TransactionManager implements AutoCloseable {

    @Inject
    MultiTransactionManager manager;

    Transactional marker;

    public TransactionManager() {
        try {
            marker = (Transactional) Tx.class.getMethod("transaction").getDeclaredAnnotations()[0];
        }
        catch (Exception e) {
            throw new ProvisionException("Failed to initialize TransactionManager");
        }
    }

    public void begin() {
        manager.startTransactionalContext(marker);
    }

    public void commit() {
        manager.commit(false);
    }

    public void rollback() {
        manager.rollback(false);
    }

    @Override
    public void close() {
        manager.close();
    }

    /**
     * Dummy class to obtain @Transactional instance
     */
    private static class Tx {
        @Transactional
        public void transaction() {
        }
    }

}
