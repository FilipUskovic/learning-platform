package com.micro.learningplatform.models;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@Table(name = "course_statistics_history")
@Slf4j
public class CourseStatisticHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID Id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @ToString.Exclude
    private Course course;

    @Column(name = "total_modules", nullable = false)
    private int totalModules;

    @Column(name = "total_duration", nullable = false)
    private Duration totalDuration;

    @Column(name = "snapshot_timestamp", nullable = false)
    private LocalDateTime snapshotTimestamp;

    protected CourseStatisticHistory() {
    }

    // Factory metoda za kreiranje povijesnog zapisa
    public static CourseStatisticHistory createSnapshot(Course course) {
        log.debug("Creating snapshot for course ID: {}", course.getId());

        CourseStatisticHistory history = new CourseStatisticHistory();
        history.course = course;
        history.totalModules = course.getCourseStatistics().getTotalModules();
        history.totalDuration = course.getCourseStatistics().getTotalDuration();
        history.snapshotTimestamp = LocalDateTime.now();
        log.debug("Snapshot created with totalModules: {}, totalDuration: {}, snapshotTimestamp: {}",
                history.totalModules, history.totalDuration, history.snapshotTimestamp);
        return history;

    }


}

