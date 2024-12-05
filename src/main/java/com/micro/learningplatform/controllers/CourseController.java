package com.micro.learningplatform.controllers;

import com.micro.learningplatform.api.CourseResource;
import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.CourseStatus;
import com.micro.learningplatform.models.dto.*;
import com.micro.learningplatform.services.CourseService;
import com.micro.learningplatform.shared.ValidationUtils;
import com.micro.learningplatform.shared.exceptions.RepositoryException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Parameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
//TODO: DODATI OPEN API swggrr 3@Tag(name = "Course Management", description = "Operations for managing courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

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
    public ResponseEntity<CourseResponse> publishCourse(@PathVariable UUID id) {
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
    public ResponseEntity<CourseResponse> getCourseWithModules(
            @PathVariable String id) {   // Primamo kao String

        return ResponseEntity.ok(
                courseService.getCourseWithModules(ValidationUtils.parseUUID(id))
        );
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

}
