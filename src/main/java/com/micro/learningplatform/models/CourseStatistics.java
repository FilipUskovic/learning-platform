package com.micro.learningplatform.models;

import com.micro.learningplatform.models.dto.coursestatistic.StatisticsCalculationContext;
import com.micro.learningplatform.models.dto.coursestatistic.StatisticsSummary;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Getter(AccessLevel.PACKAGE)
@Setter
@AllArgsConstructor
@Embeddable
public class CourseStatistics {

    @Column(name = "total_modules")
    private int totalModules;

    @Column(name = "total_duration")
    private Duration totalDuration;

    @Column(name = "last_calculated")
    private LocalDateTime lastCalculated;

    @Column(name = "average_module_duration")
    private Duration averageModuleDuration;

    @Column(name = "completion_rate")
    private BigDecimal completionRate;

    @Column(name = "difficulty_score")
    private BigDecimal difficultyScore;

    protected CourseStatistics() {
        this.totalModules = 0;
        this.totalDuration = Duration.ZERO;
        this.lastCalculated = LocalDateTime.now();
        this.completionRate = BigDecimal.ZERO;
        this.difficultyScore = BigDecimal.ZERO;
    }

    public void recalculate(List<CourseModule> modules) {
        this.totalModules = modules.size();
        this.totalDuration = modules.stream()
                .map(CourseModule::getDuration)
                .reduce(Duration.ZERO, Duration::plus);
        this.lastCalculated = LocalDateTime.now();

        if (!modules.isEmpty()) {
            this.averageModuleDuration = this.totalDuration.dividedBy(modules.size());
        } else {
            this.averageModuleDuration = Duration.ZERO;
        }
    }


    private void calculateBasicMetrics(StatisticsCalculationContext context) {
        this.totalModules = context.getModules().size();

        this.totalDuration = context.getModules().stream()
                .map(CourseModule::getDuration)
                .filter(Objects::nonNull)
                .reduce(Duration.ZERO, Duration::plus);

        if (!context.getModules().isEmpty()) {
            this.averageModuleDuration = this.totalDuration.dividedBy(this.totalModules);
        } else {
            this.averageModuleDuration = Duration.ZERO;
        }
    }

    private void calculateAdvancedMetrics(StatisticsCalculationContext context) {
        calculateCompletionRate(context);
        calculateDifficultyScore(context);
    }

    /*
    private void calculateCompletionRate(StatisticsCalculationContext context) {
        if (context.getCompletions().isEmpty()) {
            this.completionRate = BigDecimal.ZERO;
            return;
        }

        long completedCount = context.getCompletions().stream()
                .filter(CourseCompletion::isCompleted)
                .count();

        this.completionRate = BigDecimal.valueOf(completedCount)
                .divide(BigDecimal.valueOf(context.getCompletions().size()), 2, RoundingMode.HALF_UP);
    }



    private void calculateDifficultyScore(StatisticsCalculationContext context) {
        if (context.getCompletions().isEmpty()) {
            this.difficultyScore = BigDecimal.ZERO;
            return;
        }

        double averageAttempts = context.getCompletions().stream()
                .mapToInt(CourseCompletion::getAttemptCount)
                .average()
                .orElse(0.0);

        this.difficultyScore = BigDecimal.valueOf(averageAttempts)
                .multiply(BigDecimal.valueOf(this.totalModules))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

     */


    public StatisticsSummary createSummary() {
        return new StatisticsSummary(
                this.totalModules,
                this.totalDuration,
                this.averageModuleDuration,
                this.completionRate,
                this.difficultyScore,
                this.lastCalculated
        );
    }



    @Override
    public String toString() {
        return String.format(
                "CourseStatistics[modules=%d, duration=%s, avgDuration=%s, completionRate=%s%%, difficultyScore=%s]",
                totalModules,
                totalDuration,
                averageModuleDuration,
                completionRate.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP),
                difficultyScore.setScale(1, RoundingMode.HALF_UP)
        );
    }



    /*
    @Column(name = "total_modules")
    private int totalModules;

    @Column(name = "total_duration")
    private Duration totalDuration;

    @Column(name = "last_calculated")
    private LocalDateTime lastCalculated;

    @Column(name = "average_module_duration")
    private Duration averageModuleDuration;



    protected CourseStatistics() {
        this.totalModules = 0;
        this.totalDuration = Duration.ZERO;
        this.lastCalculated = LocalDateTime.now();
    }

    public void recalculate(List<CourseModule> modules) {
        this.totalModules = modules.size();
        this.totalDuration = modules.stream()
                .map(CourseModule::getDuration)
                .reduce(Duration.ZERO, Duration::plus);
        this.lastCalculated = LocalDateTime.now();

        if (!modules.isEmpty()) {
            this.averageModuleDuration = this.totalDuration.dividedBy(modules.size());
        } else {
            this.averageModuleDuration = Duration.ZERO;
        }

        this.lastCalculated = LocalDateTime.now();
    }

     */
}
