package com.micro.learningplatform.repositories;

import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.dto.CourseSearchResult;
import com.micro.learningplatform.shared.exceptions.RepositoryException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomCourseRepo {

    Page<Course> searchCourses(CourseSearchCriteria criteria, Pageable pageable) throws RepositoryException;

    void batchSave(List<Course> courses) throws RepositoryException;

    List<CourseSearchResult> fullTextSearch(String searchTerm) throws RepositoryException;
}
