package com.micro.learningplatform.models.dto;

import java.util.List;

public record CreateCourseWithModulesRequest(
        String title,
        String description,
        List<CreateModuleRequest> modules
) {
}
