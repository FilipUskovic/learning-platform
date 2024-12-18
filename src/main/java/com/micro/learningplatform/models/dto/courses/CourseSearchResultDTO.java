package com.micro.learningplatform.models.dto.courses;

import java.util.UUID;

public record CourseSearchResultDTO(
        String courseName,
        String title,
        String description,
        String difficultyLevel,
        double relevanceScore
) {
}
