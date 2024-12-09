package com.micro.learningplatform.services;

import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.CourseStatisticHistory;
import com.micro.learningplatform.repositories.CourseRepository;
import com.micro.learningplatform.repositories.CourseStatisticsHistoryRepository;
import com.micro.learningplatform.shared.exceptions.CourseNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class CourseStatisticService {

    private final CourseRepository courseRepository;
    private final CourseStatisticsHistoryRepository statisticRepo;

    @Transactional
    public void createSnapShot(UUID courseId) {
        log.debug("Attempting to create snapshot for course ID: {}", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        CourseStatisticHistory snapshot = course.createSnap();
        log.debug("Snapshot created: {}", snapshot);

        statisticRepo.save(snapshot);
        log.debug("Snapshot saved to database.");


    }

    public List<CourseStatisticHistory> getStatisticsHistory(
            UUID courseId,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        log.debug("Fetching statistics history for course ID: {}, startDate: {}, endDate: {}", courseId, startDate, endDate);

        List<CourseStatisticHistory> results = statisticRepo.findByDateRange(courseId, startDate, endDate);

        if (results.isEmpty()) {
            log.warn("No statistics found for course ID: {} within date range {} - {}", courseId, startDate, endDate);
        } else {
            log.debug("Statistics fetched: {}", results);
        }

        return results;
      //  return statisticRepo.findByDateRange(courseId, startDate, endDate);

    }



}
