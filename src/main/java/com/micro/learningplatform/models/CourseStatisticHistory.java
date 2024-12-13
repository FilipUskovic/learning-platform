package com.micro.learningplatform.models;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
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

    @Column(name = "average_module_duration", nullable = false)
    private Duration averageModuleDuration;

    @Column(name = "completion_rate", nullable = false)
    private BigDecimal completionRate;

    @Column(name = "difficulty_score", nullable = false)
    private BigDecimal difficultyScore;

    @Column(name = "snapshot_timestamp", nullable = false)
    private LocalDateTime snapshotTimestamp;

    protected CourseStatisticHistory() {
    }

    // Factory metoda za kreiranje povijesnog zapisa
    public static CourseStatisticHistory createSnapshot(CourseStatistics statistics) {
        return new CourseStatisticHistory(
                null,
                statistics.getCourse(),
                statistics.getCourse().getStatisticsSnapshot().getTotalModules(),
                statistics.getCourse().getStatisticsSnapshot().getTotalDuration(),
                statistics.getAverageModuleDuration(),
                statistics.getCompletionRate(),
                statistics.getDifficultyScore(),
                LocalDateTime.now()
        );
    }


}

