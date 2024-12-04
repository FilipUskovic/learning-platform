package com.micro.learningplatform.shared.performace;

import java.util.List;

public record OptimizationRecommendation(
        OptimizationType type,
        String description,
        List<String> affectedObjects,
        OptimizationPriority priority

) {


}
