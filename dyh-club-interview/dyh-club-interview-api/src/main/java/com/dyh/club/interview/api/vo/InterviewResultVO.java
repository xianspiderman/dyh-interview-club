package com.dyh.club.interview.api.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;


@Getter
@Setter
public class InterviewResultVO implements Serializable {

    private Double avgScore;

    private String tips;

    private String avgTips;

}
