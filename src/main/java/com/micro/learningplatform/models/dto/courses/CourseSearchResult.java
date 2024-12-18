package com.micro.learningplatform.models.dto.courses;

public record CourseSearchResult(String courseId,
                                 String title,
                                 String description,
                                 String difficultyLevel,
                                 double rank ) {
}
