package com.dyh.club.auth.domain.service.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.dyh.club.auth.common.enums.IsDeletedFlagEnum;
import com.dyh.club.auth.domain.convert.AuthPermissionBOConverter;
import com.dyh.club.auth.domain.convert.AuthRoleBOConverter;
import com.dyh.club.auth.domain.entity.AuthPermissionBO;
import com.dyh.club.auth.domain.entity.AuthRoleBO;
import com.dyh.club.auth.domain.redis.RedisUtil;
import com.dyh.club.auth.domain.service.AuthPermissionDomainService;
import com.dyh.club.auth.domain.service.AuthRoleDomainService;
import com.dyh.club.auth.infra.basic.entity.AuthPermission;
import com.dyh.club.auth.infra.basic.entity.AuthRole;
import com.dyh.club.auth.infra.basic.service.AuthPermissionService;
import com.dyh.club.auth.infra.basic.service.AuthRoleService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuthPermissionDomainServiceImpl implements AuthPermissionDomainService {

    @Resource
    private AuthPermissionService authPermissionService;

    @Resource
    private RedisUtil redisUtil;

    private String authPermissionPrefix = "auth.permission";

    @Override
    public Boolean add(AuthPermissionBO authPermissionBO) {
        AuthPermission authPermission = AuthPermissionBOConverter.INSTANCE.convertBOToEntity(authPermissionBO);
        authPermission.setIsDeleted(IsDeletedFlagEnum.UN_DELETED.getCode());
        Integer count = authPermissionService.insert(authPermission);
        return count > 0;
    }

    @Override
    public Boolean update(AuthPermissionBO authPermissionBO) {
        AuthPermission authPermission = AuthPermissionBOConverter.INSTANCE.convertBOToEntity(authPermissionBO);
        Integer count = authPermissionService.update(authPermission);
        return count > 0;
    }

    @Override
    public Boolean delete(AuthPermissionBO authPermissionBO) {
        AuthPermission authPermission = new AuthPermission();
        authPermission.setId(authPermissionBO.getId());
        authPermission.setIsDeleted(IsDeletedFlagEnum.DELETED.getCode());
        Integer count = authPermissionService.update(authPermission);
        return count > 0;
    }

    @Override
    public List<String> getPermission(String userName) {
        String permissionKey = redisUtil.buildKey(authPermissionPrefix, userName);
        String permissionValue = redisUtil.get(permissionKey);//这里拿到权限，之前在AuthUserDomainServiceImpl.java写过根据roleId查权限
        if (StringUtils.isBlank(permissionValue)) {
            return Collections.emptyList();
        }
        // 下面时进行序列化的代码，之前在网关gateway的Stplnterfacelmpl.java写过
        List<AuthPermission> permissionList = new Gson().fromJson(permissionValue,
                new TypeToken<List<AuthPermission>>() {
                }.getType());
        List<String> authList = permissionList.stream().map(AuthPermission::getPermissionKey).collect(Collectors.toList());
        return authList;
    }

}
