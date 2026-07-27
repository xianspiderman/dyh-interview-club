package com.dyh.club.practice.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SpecialPracticeLabelVO implements Serializable {


    private Long id;//标签ID

    /**
     * 分类id-标签ID
     */
    private String assembleId;

    private String labelName;//标签名称

}
