package com.micro.learningplatform.controllers;

import com.micro.learningplatform.api.CourseResource;
import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.dto.CourseResponse;
import com.micro.learningplatform.models.dto.CourseSearchRequest;
import com.micro.learningplatform.models.dto.CreateCourseRequest;
import com.micro.learningplatform.services.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
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



}
