package com.micro.learningplatform.controllers;

import com.micro.learningplatform.models.CourseStatus;
import com.micro.learningplatform.models.dto.*;
import com.micro.learningplatform.services.CourseServiceImpl;
import com.micro.learningplatform.shared.utils.ValidationUtils;
import com.micro.learningplatform.shared.exceptions.RepositoryException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private final CourseServiceImpl courseService;

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(
            @Valid @RequestBody CreateCourseRequest request) {
            CourseResponse response = courseService.createCourse(request);

        return ResponseEntity
                .created(URI.create("/api/v1/courses/" + response.publicId()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable UUID id) {
        return ResponseEntity.ok(courseService.getCourse(id));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<CourseResponse> publishCourse(@PathVariable UUID id){
        return ResponseEntity.ok(courseService.publishCourse(id));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<CourseResponse>> searchCourses(
            @Valid CourseSearchRequest request) {
        return ResponseEntity.ok(courseService.search(request));
    }

    @GetMapping("/advanced-search")
    public ResponseEntity<Page<CourseResponse>> advancedSearch(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) CourseStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(
                courseService.advancedSearch(searchTerm, status, pageable));
    }

    //todo vraca mi id ne string
    @GetMapping("/full-text-search")
    public ResponseEntity<List<CourseSearchResult>> fullTextSearch(
            @RequestParam String searchTerm) throws RepositoryException {
        return ResponseEntity.ok(courseService.fullTextSearch(searchTerm));
    }

    @GetMapping("/by-status/{status}")
    public ResponseEntity<Page<CourseResponse>> getCoursesByStatus(
           // @Parameter(description = "Course status")
            @PathVariable String status,
            Pageable pageable) {
        return ResponseEntity.ok(
                Optional.ofNullable(status)
                        .map(String::toUpperCase)
                        .map(validStatus -> courseService.findByStatus(CourseStatus.valueOf(validStatus), pageable))
                        .orElseThrow(() -> new InvalidParameterException("Status cannot be null"))
        );
    }

    @GetMapping("/{id}/with-modules")
    public ResponseEntity<CourseResponse> getCourseWithModules(@PathVariable String id) {
        return ResponseEntity.ok(courseService.getCourseWithModules(ValidationUtils.parseUUID(id)));
    }


    @PostMapping("/batch")
    public ResponseEntity<BatchResponse> batchSaveCourses(
            @Valid @RequestBody List<CreateCourseRequest> requests) throws RepositoryException {
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

    @GetMapping("/{id}/with-modules-and-statistics")
    public ResponseEntity<CourseResponseWithModules> getCourseWithModulesAndStatistics(@PathVariable UUID id) {
        return ResponseEntity.ok(courseService.getCourseWithModulesAndStatistics(id));
    }

    @PostMapping("/{id}/modules")
    public ResponseEntity<CourseResponse> addModuleToCourse(
            @PathVariable UUID id, @RequestBody CreateModuleRequest request) {
        courseService.addModuleToCourse(id, request);
        return ResponseEntity.ok(courseService.getCourseWithModules(id));
    }

    @PostMapping("/with-modules")
    public ResponseEntity<CourseResponseWithModules> createCourseWithModules(
            @RequestBody CreateCourseWithModulesRequest request) {
        CourseResponseWithModules response = courseService.createWithModule(
                new CreateCourseRequest(request.title(), request.description()),
                request.modules()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/batch-with-module")
    public ResponseEntity<String> addCourseWithModules(@RequestBody CreateCourseWithModulesRequest request) {
        courseService.batchAddCourseWithModules(request);
        return ResponseEntity.ok("Course with modules created successfully.");
    }
}
