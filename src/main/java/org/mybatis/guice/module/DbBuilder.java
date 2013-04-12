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

import com.google.inject.Provider;
import org.apache.ibatis.type.TypeHandler;

import javax.sql.DataSource;
import java.lang.annotation.Annotation;

/**
 * EDSL for building a MyBatis database configuration
 *
 * @author ferenczil
 */
public class DbBuilder {

    DbModule module;

    /**
     * Constructs a new environment
     *
     * @param module {@code EnvironmentMo
     */
    public DbBuilder(DbModule module) {
        this.module = module;
    }

    /**
     * Specify annotation for this environment
     *
     * @param annotatedWith Annotation of the Environment
     * @return builder
     */
    public DbBuilder annotatedWith(Annotation annotatedWith) {
        module.withAnnotation(annotatedWith);
        return this;
    }

    /**
     * Set the {@code DataSource} for the environment
     *
     * TODO refactor. maybe a new builder ?
     *
     * @param dataSourceProvider
     * @return builder
     */
    public DbBuilder dataSource(Provider<DataSource> dataSourceProvider) {
        module.withDataSource(dataSourceProvider);
        return this;
    }

    /**
     * Add a mapper to the given environment
     * Method can be invoked more than once, all mappers will be configured
     *
     * @param mapper Mapper interface
     * @return builder
     */
    public DbBuilder addMapper(Class<?> mapper) {
        this.module.addMapper(mapper);
        return this;
    }

    /**
     * Add a list of mappers to the given environment
     * Method can be invoked more than once, all mappers will be configured
     *
     * @param mappers Mapper interfaces
     * @return builder
     */
    public DbBuilder addMappers(Class<?>... mappers) {
        for (Class<?> mapper : mappers) {
            this.module.addMapper(mapper);
        }
        return this;
    }

    /**
     * Add a mapper xml mapping
     * Method can be invoked more than once, all mappers will be configured
     *
     * @param resource Resource path of the XML mapper
     * @return builder
     */
    public DbBuilder addMapperXml(String resource) {
        this.module.addMapperXml(resource);
        return this;
    }

    /**
     * Add multiple mapper xml resources
     * Method can be invoked more than once, all mappers will be configured
     *
     * @param resources Path of resources to add
     * @return builder
     */
    public DbBuilder addMapperXmls(String... resources) {
        for (String res : resources) {
            this.module.addMapperXml(res);
        }
        return this;
    }

    /**
     * Create a MyBatis alias for a given type
     *
     * @param alias Alias for the type
     * @param type Type
     * @return builder
     */
    public <T> DbBuilder addAlias(String alias, Class<T> type) {
        module.addAlias(alias, type);
        return this;
    }

    /**
     * Create a simple alias which is the simple name of the class for a given type
     *
     * @param type Type to create the alias for
     * @return builder
     */
    public <T> DbBuilder addSimpleAlias(Class<T> type) {
        module.addAlias(type.getSimpleName(), type);
        return this;
    }

    /**
     * Create a simple aliases which is the simple name of the class for a given type
     *
     * @param types List of types to create aliases for
     * @return builder
     */
    public DbBuilder addSimpleAliases(Class<?>... types) {
        for (Class<?> clazz : types) {
            module.addAlias(clazz.getSimpleName(), clazz);
        }
        return this;
    }

    /**
     * Add handler to a given type
     *
     * @param type Type to handle
     * @param handler Handler handling type
     * @return builder
     */
    @SuppressWarnings("unchecked")
    public <T> DbBuilder addHandler(Class<? extends TypeHandler> handler, Class<T> type) {
        module.addHandler(type, handler);
        return this;
    }

    /**
     * Add multiple classes handled by the same handler
     *
     * @param handler Handler handing types
     * @param types List of classes handled
     * @return builder
     */
    @SuppressWarnings("unchecked")
    public DbBuilder addHandlers(Class<? extends TypeHandler> handler, Class<?>... types) {
        for (Class<?> clazz : types) {
            module.addHandler(clazz, handler);
        }
        return this;
    }
}
