package com.micro.learningplatform.models.dto.coursestatistic;

import com.micro.learningplatform.models.CourseModule;
import lombok.Getter;
import lombok.Value;

import java.util.List;

@Value
@Getter
public class StatisticsCalculationContext {

    List<CourseModule> modules;
 //   List<CourseCompletion> completions;
}
