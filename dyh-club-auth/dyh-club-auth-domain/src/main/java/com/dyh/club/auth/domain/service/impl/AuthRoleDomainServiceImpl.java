package com.dyh.club.auth.domain.service.impl;

import cn.dev33.satoken.secure.SaSecureUtil;
import com.dyh.club.auth.common.enums.AuthUserStatusEnum;
import com.dyh.club.auth.common.enums.IsDeletedFlagEnum;
import com.dyh.club.auth.domain.convert.AuthRoleBOConverter;
import com.dyh.club.auth.domain.convert.AuthUserBOConverter;
import com.dyh.club.auth.domain.entity.AuthRoleBO;
import com.dyh.club.auth.domain.entity.AuthUserBO;
import com.dyh.club.auth.domain.service.AuthRoleDomainService;
import com.dyh.club.auth.domain.service.AuthUserDomainService;
import com.dyh.club.auth.infra.basic.entity.AuthRole;
import com.dyh.club.auth.infra.basic.entity.AuthUser;
import com.dyh.club.auth.infra.basic.service.AuthRoleService;
import com.dyh.club.auth.infra.basic.service.AuthUserService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class AuthRoleDomainServiceImpl implements AuthRoleDomainService {

    @Resource
    private AuthRoleService authRoleService;

    @Override
    public Boolean add(AuthRoleBO authRoleBO) {
        AuthRole authRole = AuthRoleBOConverter.INSTANCE.convertBOToEntity(authRoleBO);
        authRole.setIsDeleted(IsDeletedFlagEnum.UN_DELETED.getCode());
        Integer count = authRoleService.insert(authRole);
        return count > 0;
    }

    @Override
    public Boolean update(AuthRoleBO authRoleBO) {
        AuthRole authRole = AuthRoleBOConverter.INSTANCE.convertBOToEntity(authRoleBO);
        Integer count = authRoleService.update(authRole);
        return count > 0;
    }

    @Override
    public Boolean delete(AuthRoleBO authRoleBO) {
        AuthRole authRole = new AuthRole();
        authRole.setId(authRoleBO.getId());
        authRole.setIsDeleted(IsDeletedFlagEnum.DELETED.getCode());
        Integer count = authRoleService.update(authRole);
        return count > 0;
    }

}
