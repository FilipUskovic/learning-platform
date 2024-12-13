package com.micro.learningplatform.models.dto.courses;

import com.micro.learningplatform.models.CourseStatus;
import com.micro.learningplatform.models.EntityCategory;
import com.micro.learningplatform.models.dto.DifficultyLevel;
import com.micro.learningplatform.models.dto.coursestatistic.CourseStatisticsSnapshotDTO;
import com.micro.learningplatform.models.dto.module.ModuleResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CourseResponseWithModules(

        UUID id,
        String title,
        String description,
        CourseStatus status,
        EntityCategory category,
        DifficultyLevel difficultyLevel,
        CourseStatisticsSnapshotDTO statistics,
        List<ModuleResponse> modules,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

       /*
        String publicId,
        String title,
        String description,
        String status,
        LocalDateTime createdAt,
        List<ModuleResponse> modules

        */
) {
}
