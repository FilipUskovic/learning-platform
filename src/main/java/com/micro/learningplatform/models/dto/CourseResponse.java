package com.micro.learningplatform.models.dto;

import com.micro.learningplatform.models.CourseStatus;

import java.time.LocalDateTime;

public record CourseResponse(
        // publicId string umjesto UUID-a smanjujem izlaganje internih detalja, medu ostalom enkapsulaija podatka
        String publicId,
        String title,
        String description,
        CourseStatus status,
        LocalDateTime createdAt
        ) {
}
