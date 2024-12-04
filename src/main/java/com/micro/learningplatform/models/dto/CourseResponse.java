package com.micro.learningplatform.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.micro.learningplatform.models.CourseStatus;

import java.time.LocalDateTime;

public record CourseResponse(
        // publicId string umjesto UUID-a smanjujem izlaganje internih detalja, medu ostalom enkapsulaija podatka
        String publicId,
        String title,
        String description,
        CourseStatus status,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        LocalDateTime createdAt
        ) {
}
