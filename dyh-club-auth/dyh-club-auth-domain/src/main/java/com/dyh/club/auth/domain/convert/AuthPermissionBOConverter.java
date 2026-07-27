package com.dyh.club.auth.domain.convert;

import com.dyh.club.auth.domain.entity.AuthPermissionBO;
import com.dyh.club.auth.domain.entity.AuthRoleBO;
import com.dyh.club.auth.infra.basic.entity.AuthPermission;
import com.dyh.club.auth.infra.basic.entity.AuthRole;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 权限bo转换器
 * 
 */
@Mapper
public interface AuthPermissionBOConverter {

    AuthPermissionBOConverter INSTANCE = Mappers.getMapper(AuthPermissionBOConverter.class);

    AuthPermission convertBOToEntity(AuthPermissionBO authPermissionBO);

}
