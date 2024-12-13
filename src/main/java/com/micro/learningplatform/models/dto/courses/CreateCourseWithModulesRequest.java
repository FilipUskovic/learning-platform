package com.micro.learningplatform.models.dto.courses;

import com.micro.learningplatform.models.dto.module.CreateModuleRequest;

import java.util.List;

public record CreateCourseWithModulesRequest(
        String title,
        String description,
        List<CreateModuleRequest> modules
) {
}
