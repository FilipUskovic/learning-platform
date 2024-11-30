package com.micro.learningplatform.shared;

import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.dto.CourseResponse;
import org.springframework.stereotype.Component;

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
}
