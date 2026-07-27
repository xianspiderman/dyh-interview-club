package com.dyh.club.subject.application.convert;

import com.dyh.club.subject.application.dto.SubjectInfoDTO;
import com.dyh.club.subject.domain.entity.SubjectInfoBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 题目信息dto转换器
 *
 */
@Mapper
public interface SubjectInfoDTOConverter {

    SubjectInfoDTOConverter INSTANCE = Mappers.getMapper(SubjectInfoDTOConverter.class);

    SubjectInfoBO convertDTOToBO(SubjectInfoDTO subjectInfoDTO);

    SubjectInfoDTO convertBOToDTO(SubjectInfoBO subjectInfoBO);

    List<SubjectInfoDTO> convertBOToDTOList(List<SubjectInfoBO> subjectInfoBO);

}
