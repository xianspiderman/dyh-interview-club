package com.dyh.club.auth.infra.basic.service.impl;

import com.dyh.club.auth.infra.basic.entity.AuthRole;
import com.dyh.club.auth.infra.basic.entity.AuthRolePermission;
import com.dyh.club.auth.infra.basic.mapper.AuthRolePermissionDao;
import com.dyh.club.auth.infra.basic.service.AuthRolePermissionService;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import javax.annotation.Resource;
import java.util.List;

/**
 * (AuthRolePermission)表服务实现类
 *
 * @since 2023-11-04 22:16:00
 */
@Service("authRolePermissionService")
public class AuthRolePermissionServiceImpl implements AuthRolePermissionService {
    @Resource
    private AuthRolePermissionDao authRolePermissionDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public AuthRolePermission queryById(Long id) {
        return this.authRolePermissionDao.queryById(id);
    }

    /**
     * 新增数据
     *
     * @param authRolePermission 实例对象
     * @return 实例对象
     */
    @Override
    public AuthRolePermission insert(AuthRolePermission authRolePermission) {
        this.authRolePermissionDao.insert(authRolePermission);
        return authRolePermission;
    }

    @Override
    public int batchInsert(List<AuthRolePermission> authRolePermissionList) {
        return this.authRolePermissionDao.insertBatch(authRolePermissionList);
    }

    /**
     * 修改数据
     *
     * @param authRolePermission 实例对象
     * @return 实例对象
     */
    @Override
    public AuthRolePermission update(AuthRolePermission authRolePermission) {
        this.authRolePermissionDao.update(authRolePermission);
        return this.queryById(authRolePermission.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.authRolePermissionDao.deleteById(id) > 0;
    }

    @Override
    public List<AuthRolePermission> queryByCondition(AuthRolePermission authRolePermission) {
        return this.authRolePermissionDao.queryAllByLimit(authRolePermission);
    }

}
