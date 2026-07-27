package com.dyh.club.auth.infra.basic.service;

import com.dyh.club.auth.infra.basic.entity.AuthPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/**
 * (AuthPermission)表服务接口
 *
 * @since 2023-11-03 00:45:50
 */
public interface AuthPermissionService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    AuthPermission queryById(Long id);


    /**
     * 新增数据
     *
     * @param authPermission 实例对象
     * @return 实例对象
     */
    int insert(AuthPermission authPermission);

    /**
     * 修改数据
     *
     * @param authPermission 实例对象
     * @return 实例对象
     */
    int update(AuthPermission authPermission);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

    List<AuthPermission> queryByRoleList(List<Long> roleIdList);

}
