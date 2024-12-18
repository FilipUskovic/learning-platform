package com.micro.learningplatform.shared.validation;

import com.micro.learningplatform.models.dto.courses.CreateCourseRequest;
import com.micro.learningplatform.models.dto.module.CreateModuleRequest;
import com.micro.learningplatform.repositories.CourseRepository;
import com.micro.learningplatform.repositories.ModuleRepositroy;
import com.micro.learningplatform.shared.exceptions.CourseAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CourseValidator {

    private final CourseRepository courseRepository;
    private final ModuleRepositroy moduleRepository;
    private static final Logger log = LogManager.getLogger(CourseValidator.class);



    public void validateBatchCourseTitles(List<CreateCourseRequest> requests) {
        var titles = requests.stream()
                .map(CreateCourseRequest::title)
                .filter(title -> title != null && !title.isBlank())
                .map(String::trim)
                .map(String::toUpperCase)
                .toList();

        Set<String> uniqueTitles = new HashSet<>();
        List<String> duplicates = titles.stream()
                .filter(title -> !uniqueTitles.add(title))
                .distinct()
                .toList();

        if (!duplicates.isEmpty()) {
            log.error("Duplicate titles found within request: {}", duplicates);
            throw new CourseAlreadyExistsException("Duplicate titles found in the request: " + duplicates);
        }

        boolean existsInDatabase = courseRepository.existsByTitleInIgnoreCase(titles);
        if (existsInDatabase) {
            log.error("Courses with these titles already exist in the database: {}", titles);
            throw new CourseAlreadyExistsException("One or more courses already exist with the provided titles.");
        }
    }


    public void validateDuplicateModuleTitlesInRequest(List<CreateModuleRequest> modules) {
        List<String> titles = modules.stream()
                .map(CreateModuleRequest::title)
                .filter(title -> title != null && !title.isBlank())
                .map(String::trim)
                .map(String::toUpperCase)
                .toList();

        Set<String> uniqueTitles = new HashSet<>();
        List<String> duplicates = titles.stream()
                .filter(title -> !uniqueTitles.add(title))
                .toList();

        if (!duplicates.isEmpty()) {
            log.error("Duplicate module titles found in request: {}", duplicates);
            throw new IllegalArgumentException("Duplicate module titles found: " + duplicates);
        }

        boolean duplicatesExist = moduleRepository.existsByTitleInIgnoreCase(titles);
        if (duplicatesExist) {
            log.error("Modules with the same titles already exist in the database: {}", titles);
            throw new IllegalArgumentException("One or more modules already exist with the provided titles.");
        }
    }


    public void validateCourseTitle(String title) {
        if (courseRepository.existsByTitleIgnoreCase(title)) {
            log.warn("Attempt to create course with existing title: {}", title);
            throw new CourseAlreadyExistsException(
                    String.format("Course with title '%s' already exists", title)
            );
        }
    }

}
