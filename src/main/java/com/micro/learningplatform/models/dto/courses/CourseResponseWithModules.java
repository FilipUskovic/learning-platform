package com.micro.learningplatform.models.dto.courses;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.micro.learningplatform.models.CourseStatus;
import com.micro.learningplatform.models.EntityCategory;
import com.micro.learningplatform.models.dto.DifficultyLevel;
import com.micro.learningplatform.models.dto.module.ModuleBasicInfo;

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
        CourseStatisticsDTO statistics,
        /* korsitim  ModuleBasicInfo umjesto punog ModulaReposnsa
         jednostavnije verzije zbog izbjegavanje circule reference i hebrnate lazy incilaizacja error-a
         */
        List<ModuleBasicInfo> modules,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        LocalDateTime createdAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        LocalDateTime updatedAt

) {
}
