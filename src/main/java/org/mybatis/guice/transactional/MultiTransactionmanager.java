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
package org.mybatis.guice.transactional;

import org.apache.ibatis.exceptions.PersistenceException;
import org.mybatis.guice.session.DbSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ferenczil
 */
public class MultiTransactionManager {
    private static final Logger log = LoggerFactory.getLogger(MultiTransactionManager.class);

    Map<String, DbSessionManager> managerMap;

    ThreadLocal<Transactional> txContext;

    public MultiTransactionManager() {
        managerMap = new HashMap<>();
        txContext = new ThreadLocal<>();
    }

    public void register(String environmentId, DbSessionManager manager) {
        log.info("Registered DbSessionManager for environment {}", environmentId);
        managerMap.put(environmentId, manager);
    }

    public void startTransactionalContext(Transactional tx) {
        if (isWithinTransactionalContext()) {
            // If we already have a context do not override the settings
            return;
        }
        txContext.set(tx);
    }

    public Transactional getContext() {
        return txContext.get();
    }

    public void stopTransactionalContext() {
        txContext.remove();
    }

    public boolean isWithinTransactionalContext() {
        if (null != txContext.get()) {
            return true;
        }
        return false;
    }

    public void commit(boolean force) {
        boolean errors = false;
        for (Map.Entry<String, DbSessionManager> entry : managerMap.entrySet()) {
            final DbSessionManager man = entry.getValue();
            if (man.isManagedSessionStarted()) {
                log.debug("Committing transaction [environment: {}]", entry.getKey());
                try {
                    man.commit(force);
                }
                catch (Exception e) {
                    errors = true;
                    log.error("Failed to commit transaction for [environment: {}]", entry.getKey(), e);
                }
            }
        }
        if (errors) {
            throw new PersistenceException("One or more environments failed to commit. See log for details");
        }
    }

    public void rollback(boolean force) {
        boolean errors = false;
        for (Map.Entry<String, DbSessionManager> entry : managerMap.entrySet()) {
            final DbSessionManager man = entry.getValue();
            if (man.isManagedSessionStarted()) {
                log.debug("Rolling back transaction [environment: {}]", entry.getKey());
                try {
                    man.rollback(force);
                }
                catch (Exception e) {
                    errors = true;
                    log.error("Failed to rollback transaction for [environment: {}]", entry.getKey(), e);
                }
            }
        }
        if (errors) {
            throw new PersistenceException("One or more environments failed to roll back. See log for details");
        }

    }

    public void close() {
        boolean errors = false;
        for (Map.Entry<String, DbSessionManager> entry : managerMap.entrySet()) {
            final DbSessionManager man = entry.getValue();
            if (man.isManagedSessionStarted()) {
                log.debug("Closing session [environment: {}]", entry.getKey());
                try {
                    man.close();
                }
                catch (Exception e) {
                    errors = true;
                    log.error("Failed to close session for [environment: {}]", entry.getKey(), e);
                }
            }
        }

        // Ensure thread local cleanup on finishing
        stopTransactionalContext();

        if (errors) {
            throw new PersistenceException("One or more environments failed to close. See log for details");
        }
    }
}
