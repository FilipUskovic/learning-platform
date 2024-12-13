package com.micro.learningplatform.models.dto.courses;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.micro.learningplatform.models.CourseStatus;
import com.micro.learningplatform.models.EntityCategory;
import com.micro.learningplatform.models.dto.DifficultyLevel;
import com.micro.learningplatform.models.dto.coursestatistic.CourseStatisticsSnapshotDTO;

import java.time.LocalDateTime;
import java.util.UUID;

public record CourseResponse(

        String id,
        String title,
        String description,
        CourseStatus status,
        EntityCategory category,
        DifficultyLevel difficultyLevel,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        CourseStatisticsSnapshotDTO statistics,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
){
        /*
        // publicId string umjesto UUID-a smanjujem izlaganje internih detalja, medu ostalom enkapsulaija podatka
        String publicId,
        String title,
        String description,
        CourseStatus status,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        LocalDateTime createdAt
        ) {

         */
}


