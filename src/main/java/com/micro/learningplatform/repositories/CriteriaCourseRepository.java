package com.micro.learningplatform.repositories;

import com.micro.learningplatform.models.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

public interface CriteriaCourseRepository{
    // korisitm dinamciki querije
    List<Course> findByDynamicCriteria(CourseCriteria criteria);

}
