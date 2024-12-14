package com.micro.learningplatform.repositories;

import com.micro.learningplatform.models.CourseModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface ModuleRepositroy extends JpaRepository<CourseModule, UUID> {

    List<CourseModule> findByCourseId(UUID courseId);
}
