package org.mybatis.guice.multi;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.util.Providers;
import junit.framework.Assert;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.type.StringTypeHandler;
import org.junit.Test;
import org.mybatis.guice.MultiDbModule;
import org.mybatis.guice.annotation.Database;
import org.mybatis.guice.datasource.builtin.PooledDataSourceProvider;

/**
 * @author ferenczil
 */
public class InjectionTest {

    private static final String DRIVER = "com.mysql.jdbc.Driver";
    private static final String URL = "jdbc:mysql://localhost:3306";
    private static final String SCHEMA = "hub";
    private static final String USER = "hub";
    private static final String PASS = "hubpw";

    public static class TestModule extends MultiDbModule {
        @Override
        public void internalConfigure() {
            PooledDataSourceProvider p = new PooledDataSourceProvider("org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:aname", getClass().getClassLoader());
            p.setUser("sa");
            p.setPassword("");

            PooledDataSourceProvider p2 = new PooledDataSourceProvider("org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:aname", getClass().getClassLoader());
            p2.setUser("sa");
            p2.setPassword("");

            allowTransactionWithoutContext(true);

            add("default")
                    .annotatedWith(Database.defaultDb())
                    .addMapper(TestMapper.class)
                    .addAlias("Hello", String.class)
                    .addHandler(DummyTypeHandler.class, DummyType.class)
                    .addHandlers(DummyTypeHandler.class, DummyType.class)
                    .dataSource(Providers.guicify(p));

            add("test")
                    .annotatedWith(Database.named("test"))
                    .addMapper(TestMapper.class)
                    .addMapperXml("org/mybatis/guice/multi/TestMapper.xml")
                    .addHandler(StringTypeHandler.class, String.class)
                    .dataSource(Providers.guicify(p2));
        }
    }

    @Test
    public void testInjectorCreation() {
        Injector i = Guice.createInjector(new TestModule());

        TestMapper t = i.getInstance(Key.get(TestMapper.class, Database.defaultDb()));
        int one = t.selectOne();
        Assert.assertEquals(one, 1);

        TestMapper t2 = i.getInstance(Key.get(TestMapper.class, Database.named("test")));
        int one2 = t2.selectOne();
        Assert.assertEquals(one2, 1);

        SqlSession s = i.getInstance(Key.get(SqlSession.class, Database.named("test")));

        int one3 = s.selectOne("test");
        Assert.assertEquals(one3, 1);
    }

}
