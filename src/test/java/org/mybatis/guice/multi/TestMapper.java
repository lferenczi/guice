package org.mybatis.guice.multi;

import org.apache.ibatis.annotations.Select;

/**
 * @author ferenczil
 */
public interface TestMapper {

    @Select("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS")
    public int selectOne();

}
