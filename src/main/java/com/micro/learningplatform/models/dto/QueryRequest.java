package com.micro.learningplatform.models.dto;

import java.util.Map;

public record QueryRequest(String query, Map<String, Object> params) {
}
