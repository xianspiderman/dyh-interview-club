package com.dyh.club.auth.application.convert;

import com.dyh.club.auth.application.dto.AuthPermissionDTO;
import com.dyh.club.auth.application.dto.AuthRolePermissionDTO;
import com.dyh.club.auth.domain.entity.AuthPermissionBO;
import com.dyh.club.auth.domain.entity.AuthRolePermissionBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 权限dto转换器
 * 
 */
@Mapper
public interface AuthRolePermissionDTOConverter {

    AuthRolePermissionDTOConverter INSTANCE = Mappers.getMapper(AuthRolePermissionDTOConverter.class);

    AuthRolePermissionBO convertDTOToBO(AuthRolePermissionDTO authRolePermissionDTO);

}
