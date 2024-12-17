package com.micro.learningplatform.controllers;

import com.micro.learningplatform.models.CourseStatus;
import com.micro.learningplatform.models.dto.*;
import com.micro.learningplatform.models.dto.courses.*;
import com.micro.learningplatform.models.dto.module.CreateModuleRequest;
import com.micro.learningplatform.services.CourseServiceImpl;
import com.micro.learningplatform.shared.exceptions.RepositoryException;
import com.micro.learningplatform.shared.validation.ValidationErrorResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.InvalidParameterException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses")
@Tag(name = "Course Management", description = "Operations for managing courses")
@RequiredArgsConstructor
public class CourseController {

    private static final Logger log = LogManager.getLogger(CourseController.class);
    private final CourseServiceImpl courseService;

    // radi sve ispravno
    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(
            @Valid @RequestBody CreateCourseRequest request) {
            CourseResponse response = courseService.createCourse(request);

        return ResponseEntity
                .created(URI.create("/api/v1/courses/" + response.id()))
                .body(response);
    }

    //radi sve ispravno
    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable UUID id) {
        return ResponseEntity.ok(courseService.getCourse(id));
    }


    // radi
    @PostMapping("/{id}/publish")
    public ResponseEntity<CourseResponse> publishCourse(@PathVariable UUID id){
        return ResponseEntity.ok(courseService.publishCourse(id));
    }

    // radi sipravno
    @GetMapping("/search")
    public ResponseEntity<Page<CourseResponse>> searchCourses(
            @Valid CourseSearchRequest request) {
        return ResponseEntity.ok(courseService.search(request));
    }

    // radi osim konvertije draft-a status
    @GetMapping("/advanced-search")
    public ResponseEntity<Page<CourseResponse>> advancedSearch(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) CourseStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(
                courseService.advancedSearch(searchTerm, status, pageable));
    }

    //radi ispravno
    @GetMapping("/full-text-search")
    public ResponseEntity<List<CourseSearchResult>> fullTextSearch(
            @RequestParam String searchTerm) throws RepositoryException {
        List<CourseSearchResult> results = courseService.fullTextSearch(searchTerm);
        return ResponseEntity.ok(results);
    }

    // radi
    @GetMapping("/by-status/{status}")
    public ResponseEntity<Page<CourseResponse>> getCoursesByStatus(
            @Parameter(description = "Course status draft or published")
            @PathVariable @NotNull(message = "Status cannot be null") String status,
            @RequestParam @Min(value = 0, message = "Page number cannot be negative.") int page,
            @RequestParam @Min(value = 1, message = "Page size must be greater than 0.") int size,
            Pageable pageable) {
        return ResponseEntity.ok(
                Optional.ofNullable(status)
                        .map(String::toUpperCase)
                        .map(validStatus -> courseService.findByStatus(CourseStatus.valueOf(validStatus), pageable))
                        .orElseThrow(() -> new InvalidParameterException("Status cannot be null"))
        );
    }


    // radi
    @GetMapping("/{id}/with-modules")
    public ResponseEntity<CourseResponseWithModules> getCourseWithModules(@PathVariable UUID id) {
        return ResponseEntity.ok(courseService.getCourseWithModules(id));
    }


    // radi
    @PostMapping("/batch")
    public ResponseEntity<?> batchSaveCourses(
             @RequestBody @Valid List<@Valid CreateCourseRequest> requests) throws RepositoryException {
        courseService.batchSaveCourses(requests);
        return ResponseEntity.ok(new BatchResponse(
                requests.size() + " courses successfully created",
                LocalDateTime.now()
        ));

    }

    @GetMapping("/{id}/statistics")
    public ResponseEntity<CourseStatisticsDTO> getCourseStatistics(@PathVariable UUID id) {
        return ResponseEntity.ok(courseService.getStatistics(id));
    }

    // radi
    @GetMapping("/{id}/with-modules-and-statistics")
    public ResponseEntity<CourseResponseWithModules> getCourseWithModulesAndStatistics(@PathVariable UUID id) {
        return ResponseEntity.ok(courseService.getCourseWithModulesAndStatistics(id));
    }

    // radi
    @PostMapping("/{id}/modules")
    public ResponseEntity<CourseResponseWithModules> addModuleToCourse(
            @PathVariable UUID id, @Valid @RequestBody CreateModuleRequest request) {
        courseService.addModuleToCourse(id, request);
        return ResponseEntity.ok(courseService.getCourseWithModules(id));
    }

    // radi
    @PostMapping("/with-module")
    public ResponseEntity<CourseResponseWithModules> createCourseWithModule(
            @Valid @RequestBody CreateCourseWithModulesRequest request) {
        CourseResponseWithModules response = courseService.createWithModule(
                new CreateCourseRequest(request.title(), request.description(), request.difficultyLevel()),
                request.modules().getFirst());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }



    // treba rijesiti dupliciranj module titlova
    @PostMapping("/batch-with-modules")
    public ResponseEntity<String> batchAddCoursesWithModules(@RequestBody @Valid CreateCourseWithModulesRequest request) {
        courseService.batchAddCourseWithModules(request);
        return ResponseEntity.ok("Courses with modules created successfully.");
    }

    @GetMapping("/filter")
    public ResponseEntity<List<CourseResponse>> findByCategoryAndDifficulty(
            @RequestParam String category,
            @RequestParam String difficultyLevel) {
        return ResponseEntity.ok(courseService.findByCategoryAndDifficultyLevel(category, difficultyLevel));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<CourseResponse>> getRecentCourses(
            @RequestParam CourseStatus status) {
        return ResponseEntity.ok(courseService.getRecentCoursesByStatus(status));
    }
}
