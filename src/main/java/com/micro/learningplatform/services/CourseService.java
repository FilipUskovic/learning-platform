package com.micro.learningplatform.services;

import com.micro.learningplatform.models.CourseStatisticHistory;
import com.micro.learningplatform.models.CourseStatus;
import com.micro.learningplatform.models.dto.courses.*;
import com.micro.learningplatform.models.dto.module.CreateModuleRequest;
import com.micro.learningplatform.models.dto.module.ModuleDetailResponse;
import com.micro.learningplatform.shared.exceptions.RepositoryException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CourseService {

    CourseResponse createCourse(CreateCourseRequest createCourseRequest);

    CourseResponse getCourse(UUID courseId);

    CourseResponse publishCourse(UUID courseId);

    Page<CourseResponse> search(CourseSearchRequest searchRequest);

    Page<CourseResponse> advancedSearch(String searchTerm, CourseStatus status, Pageable pageable);

    List<CourseSearchResult> fullTextSearch(String searchTerm) throws RepositoryException;

    Page<CourseResponse> findByStatus(CourseStatus status, Pageable pageable);

    CourseResponseWithModules getCourseWithModules(UUID id);

   void batchSaveCourses(List<CreateCourseRequest> requests) throws RepositoryException;

    CourseStatisticsDTO getStatistics(UUID courseId);

    CourseResponseWithModules getCourseWithModulesAndStatistics(UUID courseId);

    void addModuleToCourse(UUID courseId, CreateModuleRequest request);

    CourseResponseWithModules createWithModule(CreateCourseRequest createCourseRequest, CreateModuleRequest moduleRequests);

    void batchAddCourseWithModules(CreateCourseWithModulesRequest request);

    List<CourseStatisticHistory> getCourseHistory(UUID courseId, LocalDateTime startDate, LocalDateTime endDate);

    ModuleDetailResponse getModuleDetails(UUID moduleId);

    List<CourseResponse> getRecentCoursesByStatus(CourseStatus status); // S @QueryHints za caching
    // S @QueryHints za caching
    List<CourseResponse> findByCategoryAndDifficultyLevel(String category, String level); // S indeks optimizacijom

    Page<CourseResponse> searchByTerm(String searchTerm, Pageable pageable);


}
