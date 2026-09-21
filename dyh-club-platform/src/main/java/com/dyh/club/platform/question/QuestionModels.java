package com.dyh.club.platform.question;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

public final class QuestionModels {
    private QuestionModels() {}

    public static class QuestionRequest {
        public Long id;
        @NotBlank @Size(max = 500) public String name;
        @Size(max = 10000) public String analysis;
        @NotNull @Min(1) @Max(5) public Integer difficulty = 2;
        @NotNull @Min(1) @Max(100) public Integer score = 1;
        @NotBlank public String type;
        public String status = "DRAFT";
        @NotEmpty public List<Long> categoryIds = new ArrayList<>();
        public List<Long> labelIds = new ArrayList<>();
        @Valid public List<OptionRequest> options = new ArrayList<>();
        public String referenceAnswer;
    }

    public static class OptionRequest {
        @NotBlank @Size(max = 8) public String code;
        @NotBlank @Size(max = 1000) public String content;
        public boolean correct;
    }

    public static class QuestionQuery {
        public String keyword;
        public String type;
        public String status = "PUBLISHED";
        public Long categoryId;
        public Long labelId;
        @Min(1) public int page = 1;
        @Min(1) @Max(50) public int size = 20;
    }

    public static class AggregateResponse {
        public long categoryId;
        public String categoryName;
        public boolean degraded;
        public List<java.util.Map<String, Object>> labels = new ArrayList<>();
    }
}
