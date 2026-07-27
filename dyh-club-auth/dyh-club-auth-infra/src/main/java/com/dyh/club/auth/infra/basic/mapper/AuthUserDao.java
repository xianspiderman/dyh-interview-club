package com.dyh.club.auth.infra.basic.mapper;

import com.dyh.club.auth.infra.basic.entity.AuthUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * (AuthUser)表数据库访问层
 *
 * @since 2023-11-01 01:25:22
 */
public interface AuthUserDao {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    AuthUser queryById(Long id);

    /**
     * 查询指定行数据
     *
     * @param authUser 查询条件
     * @return 对象列表
     */
    List<AuthUser> queryAllByLimit(AuthUser authUser);

    /**
     * 统计总行数
     *
     * @param authUser 查询条件
     * @return 总行数
     */
    long count(AuthUser authUser);

    /**
     * 新增数据
     *
     * @param authUser 实例对象
     * @return 影响行数
     */
    int insert(AuthUser authUser);

    /**
     * 批量新增数据（MyBatis原生foreach方法）
     *
     * @param entities List<AuthUser> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<AuthUser> entities);

    /**
     * 批量新增或按主键更新数据（MyBatis原生foreach方法）
     *
     * @param entities List<AuthUser> 实例对象列表
     * @return 影响行数
     * @throws org.springframework.jdbc.BadSqlGrammarException 入参是空List的时候会抛SQL语句错误的异常，请自行校验入参
     */
    int insertOrUpdateBatch(@Param("entities") List<AuthUser> entities);

    /**
     * 修改数据
     *
     * @param authUser 实例对象
     * @return 影响行数
     */
    int update(AuthUser authUser);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);

    Integer updateByUserName(AuthUser authUser);

    List<AuthUser> listUserInfoByIds(@Param("userNameList") List<String> userNameList);
}

