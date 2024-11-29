package com.micro.learningplatform.services;

import com.micro.learningplatform.models.dto.CourseResponse;
import com.micro.learningplatform.models.dto.CourseSearchRequest;
import com.micro.learningplatform.models.dto.CreateCourseRequest;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface CourseService {

    CourseResponse createCourse(CreateCourseRequest createCourseRequest);

    CourseResponse getCourse(UUID courseId);

    CourseResponse publishCourse(UUID courseId);

    Page<CourseResponse> search(CourseSearchRequest searchRequest);

}
