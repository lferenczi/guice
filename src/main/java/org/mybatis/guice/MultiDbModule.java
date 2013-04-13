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
package org.mybatis.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import org.mybatis.guice.module.DbBuilder;
import org.mybatis.guice.module.DbModule;
import org.mybatis.guice.transactional.MultiTransactionManager;
import org.mybatis.guice.transactional.MultiTransactionalMethodInterceptor;
import org.mybatis.guice.transactional.TransactionManager;
import org.mybatis.guice.transactional.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;
import static com.google.inject.name.Names.named;

/**
 * @author ferenczil
 */
public abstract class MultiDbModule extends AbstractModule {

    private ClassLoader driverClassLoader = getDefaultClassLoader();

    private List<DbModule> modules = new ArrayList<>();

    private boolean allowTransactionWithoutContext = false;

    public void configure() {
        bind(ClassLoader.class).annotatedWith(named("JDBC.driverClassLoader")).toInstance(driverClassLoader);

        bind(MultiTransactionManager.class).in(Scopes.SINGLETON);
        bind(TransactionManager.class).in(Scopes.SINGLETON);

        MultiTransactionalMethodInterceptor interceptor = new MultiTransactionalMethodInterceptor();
        requestInjection(interceptor);
        bindInterceptor(any(), annotatedWith(Transactional.class), interceptor);

        internalConfigure();

        // Install all private modules
        for (DbModule m : modules) {
            install(m);
        }
        bindConstant().annotatedWith(Names.named("mybatis.configuration.allowTransactionWithoutContext")).to(allowTransactionWithoutContext);
    }

    public abstract void internalConfigure();

    private ClassLoader getDefaultClassLoader() {
        return getClass().getClassLoader();
    }

    protected DbBuilder add(String environmentId) {
        DbModule module = new DbModule(environmentId);
        modules.add(module);
        return new DbBuilder(module);
    }

    protected void allowTransactionWithoutContext(boolean allow) {
        this.allowTransactionWithoutContext = allow;
    }

}
