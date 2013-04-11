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
package org.mybatis.guice.annotation;

import java.lang.annotation.Annotation;

/**
 * @author ferenczil
 */
public class DBImpl implements DB {

    private final String value;

    public DBImpl(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return this.value;
    }

    public int hashCode() {
        // This is specified in java.lang.Annotation.
        return (127 * "value".hashCode()) ^ value.hashCode();
    }

    public boolean equals(Object o) {
        if (!(o instanceof DB)) {
            return false;
        }

        DB other = (DB) o;
        return value.equals(other.value());
    }

    public String toString() {
        return "@" + DB.class.getName() + "(value=" + value + ")";
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return DB.class;
    }
}
