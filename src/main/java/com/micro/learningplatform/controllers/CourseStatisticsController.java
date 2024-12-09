package com.micro.learningplatform.controllers;

import com.micro.learningplatform.models.CourseStatisticHistory;
import com.micro.learningplatform.services.CourseStatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/statistics")
@RequiredArgsConstructor
public class CourseStatisticsController {
    private final CourseStatisticService statisticsService;

    @GetMapping("/history")
    public ResponseEntity<List<CourseStatisticHistory>> getStatisticsHistory(
            @PathVariable UUID courseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate) {

        return ResponseEntity.ok(
                // Create initial snapshot
                statisticsService.getStatisticsHistory(courseId, startDate, endDate));
    }
}
