package com.micro.learningplatform.shared;

import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.CourseModule;
import com.micro.learningplatform.models.CourseStatistics;
import com.micro.learningplatform.models.CourseStatisticsSnapshot;
import com.micro.learningplatform.models.dto.courses.CourseResponse;
import com.micro.learningplatform.models.dto.courses.CourseResponseWithModules;
import com.micro.learningplatform.models.dto.courses.CourseStatisticsDTO;
import com.micro.learningplatform.models.dto.coursestatistic.CourseStatisticsSnapshotDTO;
import com.micro.learningplatform.models.dto.module.ModuleResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.stream.Collectors;

@Component
public class CourseMapper {

    // Centralizirano mappiranje pruza dosljednost pri konverzi u dto-ove
    //ako odlucim mogu autoamtski korsiti npr tool mapstruct koji ce automatski mappirati podatke

    private CourseMapper() {} // Utility class

    public static CourseResponse toDTO(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCourseStatus(),
                course.getCategory(),
                course.getDifficultyLevel(),
                toStatisticsDTO(course.getStatisticsSnapshot(),course.getCourseStatistics()),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }

    public static CourseResponseWithModules toDTOWithModules(Course course) {
        return new CourseResponseWithModules(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCourseStatus(),
                course.getCategory(),
                course.getDifficultyLevel(),
                toStatisticsDTO(course.getStatisticsSnapshot(), course.getCourseStatistics()),
                course.getModules().stream()
                        .map(CourseMapper::toModuleDTO)
                        .sorted(Comparator.comparing(ModuleResponse::sequenceNumber))
                        .toList(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }

    public static ModuleResponse toModuleDTO(CourseModule module) {
        return new ModuleResponse(
                module.getId(),
                module.getTitle(),
                module.getDescription(),
                module.getSequenceNumber(),
                module.getDuration(),
                module.getStatus(),
                module.getPrerequisites(),
                module.getCreatedAt(),
                module.getUpdatedAt()
        );
    }

    private static CourseStatisticsSnapshotDTO toStatisticsDTO(
            CourseStatisticsSnapshot snapshot,
            CourseStatistics statistics) {
        return new CourseStatisticsSnapshotDTO(
                snapshot.getTotalModules(),
                snapshot.getTotalDuration(),
                statistics.getAverageModuleDuration(),
                statistics.getCompletionRate(),
                statistics.getDifficultyScore(),
                statistics.getLastCalculated()
        );
    }

}
