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
package org.mybatis.guice.module;

import com.google.common.base.Preconditions;
import com.google.inject.*;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.guice.configuration.ConfigurationProvider;
import org.mybatis.guice.configuration.Mappers;
import org.mybatis.guice.configuration.TypeAliases;
import org.mybatis.guice.environment.EnvironmentProvider;
import org.mybatis.guice.mappers.MultiMapperProvider;
import org.mybatis.guice.session.DbSessionManager;
import org.mybatis.guice.session.SqlSessionFactoryProvider;

import javax.sql.DataSource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.inject.multibindings.MapBinder.newMapBinder;
import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static com.google.inject.util.Providers.guicify;

/**
 * Internal module for configuring a single MyBatis database instance
 *
 * @author ferenczil
 */
public class DbModule extends PrivateModule {

    private Class<? extends ObjectFactory> objectFactoryType = DefaultObjectFactory.class;

    private MapBinder<String, Class<?>> aliases;
    private MapBinder<Class<?>, TypeHandler<?>> handlers;
//    private Multibinder<TypeHandler<?>> mappingTypeHandlers;
//    private Multibinder<Interceptor> interceptors;
    private Multibinder<Class<?>> mappers;
    private Multibinder<String> mapperXmls;

    // Configuration
    private String environmentId;
    private Annotation annotatedWith = null;
    private Provider<DataSource> dataSourceProvider;

    private Set<Class<?>> mappersSet;
    private Set<String> mapperXmlSet;
    private Map<String, Class> aliasesMap;
    private Map<Class<?>, Class<? extends TypeHandler<?>>> handlersMap;

    /**
     * Constructs a new DbModule for a named environment
     * @param environmentId Name of the environment
     */
    public DbModule(String environmentId) {
        this.environmentId = environmentId;

        this.mappersSet = new HashSet<>();
        this.mapperXmlSet = new HashSet<>();
        this.aliasesMap = new HashMap<>();
        this.handlersMap = new HashMap<>();
    }

    @Override
    protected void configure() {
        Preconditions.checkNotNull(annotatedWith, "Annotation must be specified for datasource: " + environmentId);

        aliases = newMapBinder(binder(), new TypeLiteral<String>(){}, new TypeLiteral<Class<?>>(){}, TypeAliases.class);
        handlers = newMapBinder(binder(), new TypeLiteral<Class<?>>(){}, new TypeLiteral<TypeHandler<?>>(){});
//        interceptors = newSetBinder(binder(), Interceptor.class);
//        mappingTypeHandlers = newSetBinder(binder(), new TypeLiteral<TypeHandler<?>>(){}, MappingTypeHandlers.class);
        mappers = newSetBinder(binder(), new TypeLiteral<Class<?>>(){}, Mappers.class);
        mapperXmls = newSetBinder(binder(), String.class, Mappers.class);

        bindConstant().annotatedWith(Names.named("mybatis.environment.id")).to(environmentId);

        bind(TransactionFactory.class).to(JdbcTransactionFactory.class).in(Scopes.SINGLETON);

        // Main
        bind(DbSessionManager.class).in(Scopes.SINGLETON);
        bind(SqlSession.class).to(DbSessionManager.class).in(Scopes.SINGLETON);
        bind(Environment.class).toProvider(EnvironmentProvider.class).in(Scopes.SINGLETON);
        bind(Configuration.class).toProvider(ConfigurationProvider.class).in(Scopes.SINGLETON);
        bind(SqlSessionFactory.class).toProvider(SqlSessionFactoryProvider.class).in(Scopes.SINGLETON);

        bind(DataSource.class).toProvider(dataSourceProvider).in(Scopes.SINGLETON);

        bind(ObjectFactory.class).to(objectFactoryType).in(Scopes.SINGLETON);

        bindWithKey(SqlSession.class);
        bindWithKey(SqlSessionFactory.class);
        bindWithKey(DbSessionManager.class);

        // Aliases
        for (Map.Entry<String, Class> e : aliasesMap.entrySet()) {
            aliases.addBinding(e.getKey()).toInstance(e.getValue());
        }

        // Mappers
        for (Class<?> mapper : mappersSet) {
            bindMapper(mapper);
            bindWithKey(mapper);
        }
        for (String mapperXml : mapperXmlSet) {
            mapperXmls.addBinding().toInstance(mapperXml);
        }

        // Type handlers
        for (Map.Entry<Class<?>, Class<? extends TypeHandler<?>>> e : handlersMap.entrySet()) {
            handlers.addBinding(e.getKey()).toInstance(createTypeHandler(e.getKey(), e.getValue()));
        }
    }

    public <T> void addMapper(Class<T> type) {
        mappersSet.add(type);
    }

    public void addMapperXml(String resource) {
        mapperXmlSet.add(resource);
    }

    public <T> void addAlias(String alias, Class<T> type) {
        aliasesMap.put(alias, type);
    }

    @SuppressWarnings("unchecked")
    public <T> void addHandler(Class<T> type, Class<? extends TypeHandler> handler) {
        handlersMap.put(type, (Class<? extends TypeHandler<?>>) handler);
    }

    public void withAnnotation(Annotation annotatedWith) {
        this.annotatedWith = annotatedWith;
    }

    public void withDataSource(Provider<DataSource> dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
    }


    // --------------------------------------------------------------------
    // Internal
    // --------------------------------------------------------------------


    /**
     * Internal binding of mappers
     *
     * @param mapperType
     * @param <T>
     */
    private <T> void bindMapper(Class<T> mapperType) {
        mappers.addBinding().toInstance(mapperType);
        bind(mapperType).toProvider(guicify(new MultiMapperProvider<>(mapperType))).in(Scopes.SINGLETON);
    }

    /**
     * Internal helper method to bind a class to a specific key (constructed with type + annotatedWith)
     *
     * @param clazz type for the key
     */
    private <T> void bindWithKey(Class<T> clazz) {
        Key<T> key = Key.get(clazz, annotatedWith);
        bind(key).to(clazz);
        expose(key);
    }

    /**
     * Constructs a TypeHandler instance. To support generic mappers without actual type information
     * the {@code TypeHandlerFactory.getInstance()} method's logic is re-used. The created class will be bound
     * by guice, allowing injection to happen correctly
     *
     * @param type Type to handle
     * @param handler Handler type
     * @return Constructed TypeHandler instance
     */
    private TypeHandler<?> createTypeHandler(Class<?> type, Class<? extends TypeHandler<?>> handler) {
        if (type != null) {
            try {
                Constructor<?> c = handler.getConstructor(Class.class);
                return (TypeHandler<?>) c.newInstance(type);
            }
            catch (NoSuchMethodException ignored) {
                // ignored
            }
            catch (Exception e) {
                throw new ProvisionException("Failed creating type handler for " + handler, e);
            }
        }
        try {
            Constructor<?> c = handler.getConstructor();
            return (TypeHandler<?>) c.newInstance();
        }
        catch (Exception e) {
            throw new ProvisionException("Failed creating type handler for " + handler, e);
        }
    }

}
