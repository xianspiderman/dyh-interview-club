package com.dyh.club.auth.application.convert;

import com.dyh.club.auth.domain.entity.AuthUserBO;
import com.dyh.club.auth.entity.AuthUserDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 用户dto转换器
 *
 */
@Mapper
public interface AuthUserDTOConverter {

    AuthUserDTOConverter INSTANCE = Mappers.getMapper(AuthUserDTOConverter.class);

    AuthUserBO convertDTOToBO(AuthUserDTO authUserDTO);

    AuthUserDTO convertBOToDTO(AuthUserBO authUserBO);

    List<AuthUserDTO> convertBOToDTO(List<AuthUserBO> authUserBO);

}
