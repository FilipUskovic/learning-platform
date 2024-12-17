package com.micro.learningplatform.models.dto.courses;

public record CourseSearchResult(String courseName,
                                 String title,
                                 String description,
                                 String difficultyLevel,
                                 double rank ) {
}
