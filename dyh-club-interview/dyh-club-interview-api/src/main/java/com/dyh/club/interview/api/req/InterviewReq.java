package com.dyh.club.interview.api.req;

import com.dyh.club.interview.api.enums.EngineEnum;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;


@Getter
@Setter
public class InterviewReq implements Serializable {

    private String url;

    private String engine = EngineEnum.DYH.name();

}
