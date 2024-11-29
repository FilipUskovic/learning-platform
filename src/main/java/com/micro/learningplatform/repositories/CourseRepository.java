package com.micro.learningplatform.repositories;

import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {

   /* @Query("SELECT case when count (c) > 0 then true else false end " +
            "from Course c where lower(c.title) = lower(:title) ")
           @Param("title")
    */
    boolean existsByTitleIgnoreCase(String title);

    @Query("SELECT c FROM Course c WHERE c.courseStatus = :status  ORDER BY c.createdAt DESC ")
    Page<Course> findByStatus(
            @Param("status") CourseStatus status, Pageable pageable
    );

    @Query("""
        SELECT c FROM Course c\s
        WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\s
        OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
       \s""")
    Page<Course> searchCourses(
            @Param("searchTerm") String searchTerm, Pageable pageable
    );
}
