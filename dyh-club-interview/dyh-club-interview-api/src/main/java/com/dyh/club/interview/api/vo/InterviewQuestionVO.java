package com.dyh.club.interview.api.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;


@Getter
@Setter
public class InterviewQuestionVO implements Serializable {

    private List<Interview> questionList;

    @Data
    public static class Interview {
        private String labelName;

        private String keyWord;

        private String subjectName;

        private String subjectAnswer;

    }

}
