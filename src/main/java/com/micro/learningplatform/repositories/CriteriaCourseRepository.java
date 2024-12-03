package com.micro.learningplatform.repositories;

import com.micro.learningplatform.models.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CriteriaCourseRepository{
    // korisitm dinamciki querije
    List<Course> findByDynamicCriteria(CourseCriteria criteria);

}
