package com.micro.learningplatform.models.dto.courses;

import java.util.UUID;

public record CourseBatchDTO(
         UUID id,
         String title,
        String status,
        int moduleCount
) {
}
