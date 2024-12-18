package com.micro.learningplatform.models.dto.courses;

public record CourseSearchResultDTO(
        String courseName,
        String title,
        String description,
        String difficultyLevel,
        double relevanceScore
) {
}
