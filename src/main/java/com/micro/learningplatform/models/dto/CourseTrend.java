package com.micro.learningplatform.models.dto;

public record CourseTrend(
        String month,
        int courseCount,
        double movingAverage,
        int monthOverMonthChange
) {

    public static CourseTrend fromResult(Object[] tuple) {
        return new CourseTrend(
                (String) tuple[0],
                ((Number) tuple[1]).intValue(),
                ((Number) tuple[2]).doubleValue(),
                ((Number) tuple[3]).intValue()
        );
    }
}
