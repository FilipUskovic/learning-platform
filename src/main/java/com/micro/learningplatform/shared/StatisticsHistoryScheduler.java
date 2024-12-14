package com.micro.learningplatform.shared;

import com.micro.learningplatform.repositories.CourseRepository;
import com.micro.learningplatform.services.CourseStatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StatisticsHistoryScheduler {

    private final CourseStatisticService statisticsService;
    private final CourseRepository courseRepository;
/*
    @Scheduled(cron = "0 0 0 * * *") // Svaki dan u ponoc
    public void statisticsHistory() {
        courseRepository.findAll().forEach(course ->
                statisticsService.createSnapShot(course.getId()));
    }

 */

}
