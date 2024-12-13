package com.micro.learningplatform.shared;

import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.CourseModule;
import com.micro.learningplatform.models.dto.courses.CourseResponseWithModules;
import com.micro.learningplatform.models.dto.module.ModuleResponse;

public class CourseModuleMapper {


    public static ModuleResponse toDTO(CourseModule module) {
        return new ModuleResponse(
                module.getId() != null ? module.getId().toString() : null,
                module.getTitle(),
                module.getDescription(),
                module.getSequenceNumber(),
                module.getDuration(),
                module.getEstimatedDuration()
        );
    }
    public static CourseResponseWithModules toResponseWithModules(Course course) {
        return new CourseResponseWithModules(
                course.getId() != null ? course.getId().toString() : null,
                course.getTitle(),
                course.getDescription(),
                course.getCourseStatus().toString(),
                course.getCreatedAt(),
                course.getModules().stream()
                        .map(module -> new ModuleResponse(
                                module.getId() != null ? module.getId().toString() : null,
                                module.getTitle(),
                                module.getDescription(),
                                module.getSequenceNumber(),
                                module.getDuration(),
                                module.getEstimatedDuration()))
                        .toList()
        );
    }
}

