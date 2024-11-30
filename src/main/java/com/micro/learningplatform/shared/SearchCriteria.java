package com.micro.learningplatform.shared;

import com.micro.learningplatform.models.CourseStatus;

public class SearchCriteria {
    private String searchTerm;
    private CourseStatus status;

    // Konstruktor
    public SearchCriteria(String searchTerm, CourseStatus status) {
        this.searchTerm = searchTerm;
        this.status = status;
    }

    // Getteri
    public String getSearchTerm() {
        return searchTerm;
    }

    public CourseStatus getStatus() {
        return status;
    }

    // Metode za provjeru prisutnosti kriterija
    public boolean hasSearchTerm() {
        return searchTerm != null && !searchTerm.isBlank();
    }

    public boolean hasStatus() {
        return status != null;
    }
}
