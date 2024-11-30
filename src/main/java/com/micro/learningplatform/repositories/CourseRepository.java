package com.micro.learningplatform.repositories;

import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.CourseStatus;
import jakarta.persistence.Entity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
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

 @Query("""
        SELECT c FROM Course c\s
        WHERE (:status IS NULL OR c.courseStatus = :status)\s
        AND (:searchTerm IS NULL\s
             OR LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
             OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
       \s""")
    Page<Course> advanceSearchCourses(@Param("searchTerm") String searchTerm,
                                      @Param("status") CourseStatus status,
                                      Pageable pageable);

   //TODO provjeriti dali mi trebaju ove dolje metode posto koristi batch s entetymanagerom

    @EntityGraph(attributePaths = {"modules"})
    Optional<Course> findWithModulesById(UUID id);

    @Query("select c from Course c left join fetch CourseModule m " +
            "where c.courseStatus = :status order by c.createdAt desc, m.sequenceNumber")
    List<Course> findAllWithModulesByStatus(
            @Param("status") CourseStatus status,
            Pageable pageable
    );

    @Query(value = """
        SELECT c.* FROM courses c
        WHERE c.course_status = :status
        AND EXISTS (
            SELECT 1 FROM course_modules m
            WHERE m.course_id = c.id
            AND m.duration <= :maxDuration
        )
        """, nativeQuery = true)
    List<Course> findCoursesWithModulesUnderDuration(
            @Param("status") String status,
            @Param("maxDuration") Duration maxDuration
    );

}