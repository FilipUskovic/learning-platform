package com.micro.learningplatform.repositories;

import com.micro.learningplatform.models.CourseModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ModuleRepositroy extends JpaRepository<CourseModule, UUID> {

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN TRUE ELSE FALSE END " +
            "FROM CourseModule m WHERE UPPER(m.title) IN :titles")
    boolean existsByTitleInIgnoreCase(List<String> titles);
}
