package com.micro.learningplatform.shared;

import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.CourseModule;
import com.micro.learningplatform.models.dto.CourseResponse;
import com.micro.learningplatform.models.dto.CourseResponseWithModules;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CourseMapper {

    // Centralizirano mappiranje pruza dosljednost pri konverzi u dto-ove
    //ako odlucim mogu autoamtski korsiti npr tool mapstruct koji ce automatski mappirati podatke

    public static CourseResponse toDTO(Course course) {
        return new CourseResponse(
                course.getId().toString(),
                course.getTitle(),
                course.getDescription(),
                course.getCourseStatus(),
                course.getCreatedAt()
        );
    }

    public static CourseResponseWithModules toDTOAdvance(Course course) {
        return new CourseResponseWithModules(
                course.getId() != null ? course.getId().toString() : null,
                course.getTitle(),
                course.getDescription(),
                course.getCourseStatus().name(),
                course.getCreatedAt(),
                course.getModules().stream()
                        .map(CourseModuleMapper::toDTO)
                        .collect(Collectors.toList())
        );
    }

}
