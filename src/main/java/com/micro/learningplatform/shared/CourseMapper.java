package com.micro.learningplatform.shared;

import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.CourseModule;
import com.micro.learningplatform.models.CourseStatistics;
import com.micro.learningplatform.models.CourseStatisticsSnapshot;
import com.micro.learningplatform.models.dto.courses.CourseResponse;
import com.micro.learningplatform.models.dto.courses.CourseResponseWithModules;
import com.micro.learningplatform.models.dto.courses.CourseStatisticsDTO;
import com.micro.learningplatform.models.dto.module.ModuleBasicInfo;
import com.micro.learningplatform.models.dto.module.ModuleDetailResponse;
import com.micro.learningplatform.models.dto.module.ModuleReferenceInfo;
import org.springframework.stereotype.Component;

import java.util.*;
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
                toStatisticsDTO(course.getStatisticsSnapshot(), course.getCourseStatistics()),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }

    public static CourseResponseWithModules toCourseWithModulesResponse(Course course) {
        List<ModuleBasicInfo> moduleInfos = course.getModules().stream()
                .map(CourseMapper::toModuleBasicInfo)
                .sorted(Comparator.comparing(ModuleBasicInfo::sequenceNumber))
                .toList();

        return new CourseResponseWithModules(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCourseStatus(),
                course.getCategory(),
                course.getDifficultyLevel(),
                toStatisticsDTO(course.getStatisticsSnapshot(), course.getCourseStatistics()),
                moduleInfos,
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }

    public static ModuleDetailResponse toModuleDetailResponse(
            CourseModule module,
            Map<UUID, CourseModule> prerequisiteModules) {

        Set<ModuleReferenceInfo> prerequisiteInfos = module.getPrerequisites().stream()
                .map(prerequisiteModules::get)
                .filter(Objects::nonNull)  // Zaštita od null vrijednosti
                .map(CourseMapper::toModuleReferenceInfo)
                .collect(Collectors.toSet());

        return new ModuleDetailResponse(
                module.getId(),
                module.getTitle(),
                module.getDescription(),
                module.getSequenceNumber(),
                module.getDuration(),
                module.getStatus(),
                module.getCourse().getId(),
                prerequisiteInfos,
                module.getCreatedAt(),
                module.getUpdatedAt()
        );
    }

    private static ModuleBasicInfo toModuleBasicInfo(CourseModule module) {
        return new ModuleBasicInfo(
                module.getId(),
                module.getTitle(),
                module.getSequenceNumber(),
                module.getDuration(),
                module.getStatus(),
                module.getDifficultyLevel()
        );
    }

    private static ModuleReferenceInfo toModuleReferenceInfo(CourseModule module) {
        return new ModuleReferenceInfo(
                module.getId(),
                module.getTitle(),
                module.getStatus()
        );
    }

    private static CourseStatisticsDTO toStatisticsDTO(
            CourseStatisticsSnapshot snapshot,
            CourseStatistics statistics) {
        return new CourseStatisticsDTO(
                snapshot.getTotalModules(),
                snapshot.getTotalDuration(),
                statistics.getAverageModuleDuration(),
                statistics.getCompletionRate(),
                statistics.getDifficultyScore(),
                statistics.getLastCalculated()
        );
    }

}
