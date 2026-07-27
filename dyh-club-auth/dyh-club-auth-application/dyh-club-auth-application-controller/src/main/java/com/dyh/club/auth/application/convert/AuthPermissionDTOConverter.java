package com.dyh.club.auth.application.convert;

import com.dyh.club.auth.application.dto.AuthPermissionDTO;
import com.dyh.club.auth.application.dto.AuthRoleDTO;
import com.dyh.club.auth.domain.entity.AuthPermissionBO;
import com.dyh.club.auth.domain.entity.AuthRoleBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 权限dto转换器
 * 
 */
@Mapper
public interface AuthPermissionDTOConverter {

    AuthPermissionDTOConverter INSTANCE = Mappers.getMapper(AuthPermissionDTOConverter.class);

    AuthPermissionBO convertDTOToBO(AuthPermissionDTO authPermissionDTO);

}
