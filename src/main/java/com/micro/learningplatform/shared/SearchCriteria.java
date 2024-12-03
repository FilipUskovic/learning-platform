package com.micro.learningplatform.shared;

import com.micro.learningplatform.models.CourseStatus;
import lombok.Getter;

@Getter
public class SearchCriteria {
    private String searchTerm;
    private CourseStatus status;

    public SearchCriteria(String searchTerm, CourseStatus status) {
        this.searchTerm = searchTerm;
        this.status = status;
    }

    // Metode za provjeru prisutnosti kriterija

    public boolean hasSearchTerm() {
        return searchTerm != null && !searchTerm.isBlank();
    }

    public boolean hasStatus() {
        return status != null;
    }
}
