package org.mybatis.guice.multi;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import junit.framework.Assert;
import org.apache.ibatis.session.ExecutorType;
import org.junit.Test;
import org.mybatis.guice.annotation.DB;
import org.mybatis.guice.transactional.Transactional;

/**
 * @author ferenczil
 */
public class TransactionTest {

    public static class TxTester {

        @Inject @DB
        TestMapper mapper;
        @Inject @DB("test") TestMapper mapper2;

        @Transactional
        public void testTransaction() {
            int one = mapper.selectOne();
            Assert.assertEquals(one, 1);
            testOtherTransaction();
        }

        @Transactional(executorType = ExecutorType.REUSE)
        public void testOtherTransaction() {
            int one = mapper2.selectOne();
            Assert.assertEquals(one, 1);
        }
    }

    @Test
    public void testTransaction() {
        Injector i = Guice.createInjector(new InjectionTest.TestModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(TxTester.class);
                    }
                });

        TxTester tx = i.getInstance(TxTester.class);
        tx.testTransaction();
    }

}
