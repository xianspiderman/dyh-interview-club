package com.dyh.club.subject.application.convert;

import com.dyh.club.subject.application.dto.SubjectLabelDTO;
import com.dyh.club.subject.domain.entity.SubjectLabelBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 标签dto的转换
 * 
 */
@Mapper
public interface SubjectLabelDTOConverter {

    SubjectLabelDTOConverter INSTANCE = Mappers.getMapper(SubjectLabelDTOConverter.class);

    SubjectLabelBO convertDtoToLabelBO(SubjectLabelDTO subjectLabelDTO);

    List<SubjectLabelDTO> convertBOToLabelDTOList(List<SubjectLabelBO> boList);

}
