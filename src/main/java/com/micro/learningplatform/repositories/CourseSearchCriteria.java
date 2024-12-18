package com.micro.learningplatform.repositories;

import com.micro.learningplatform.models.CourseStatus;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSearchCriteria {
    private String searchTerm;
    private CourseStatus status;
    private Integer minDuration;
    private Integer maxDuration;
    private String category;
    private String title;
    private SearchType searchType;

    public enum SearchType {
        BASIC,
        ADVANCED,
        FULL_TEXT
    }

}




