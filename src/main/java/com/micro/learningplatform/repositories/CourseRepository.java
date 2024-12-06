package com.micro.learningplatform.repositories;

import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.CourseStatus;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {


    boolean existsByTitleIgnoreCase(String title);

    @Query("SELECT c FROM Course c WHERE c.courseStatus = :status ORDER BY c.createdAt DESC")
    Page<Course> findByStatus(@Param("status") CourseStatus status, Pageable pageable);

    @Query("""
        SELECT c FROM Course c
        WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        """)
    Page<Course> searchCourses(@Param("searchTerm") String searchTerm, Pageable pageable);

    @EntityGraph(attributePaths = {"modules"})
    Optional<Course> findWithModulesById(UUID id);

    @Query("""
        SELECT c FROM Course c
        WHERE c.courseStatus = :status
        ORDER BY c.createdAt DESC
        """)
    @QueryHints({
            @QueryHint(name = "org.hibernate.cacheable", value = "true"),
            @QueryHint(name = "org.hibernate.cacheRegion", value = "course.search")
    })
    List<Course> findByStatus(@Param("status") CourseStatus status);


    @Query("""
        SELECT c FROM Course c
        WHERE (:status IS NULL OR c.courseStatus = :status)
        AND (:searchTerm IS NULL
             OR LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
             OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        """)
    Page<Course> advanceSearchCourses(@Param("searchTerm") String searchTerm,
                                      @Param("status") CourseStatus status,
                                      Pageable pageable);


    // dodajem indexke 
    @Query(value = """
        SELECT c FROM Course c
        WHERE c.courseStatus = :status
        ORDER BY c.createdAt DESC
        """)
    @QueryHints(@QueryHint(name = "org.hibernate.comment",
            value = "Using idx_courses_status_created"))
    List<Course> findByStatusOrderByCreatedAt(@Param("status") CourseStatus status);

    // Dodajemo podršku za idx_course_category_level
    @Query(value = """
        SELECT c FROM Course c
        WHERE c.category = :category
        AND c.difficultyLevel = :level
        """)
    @QueryHints(@QueryHint(name = "org.hibernate.comment",
            value = "Using idx_course_category_level"))
    List<Course> findByCategoryAndDifficultyLevel(
            @Param("category") String category,
            @Param("level") String level);

}
