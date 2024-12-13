package com.micro.learningplatform.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter(AccessLevel.PACKAGE)
@AllArgsConstructor
@Entity
@ToString
@NoArgsConstructor
@Table(name = "course_statistics")
public class CourseStatistics {

    /**
     *  Ova klasa pruza detaljnu statisku
     *  Prati kopletan korsinicki napredak kroz tecajeve (courses)

     */

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "course_id")
    private UUID courseId;

    /**
     *  MapsId je kompozitni primrany kljuc tecaja -> dijeli siti primarny kljuc kao Course (CourseStatistics i couse) isti priarmn kljuc
     *  relacija 1 na 1
     *  join colum povezuje course_id iz CourseStatistics s Course.
     */

    @MapsId
    @OneToOne
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "average_module_duration")
    private Duration averageModuleDuration;

    @Column(name = "completion_rate")
    private BigDecimal completionRate;

    @Column(name = "difficulty_score")
    private BigDecimal difficultyScore;

    @Column(name = "last_calculated")
    private LocalDateTime lastCalculated;


    /*
       Povezujemo statiskiku s odgovarajucim tecajem
     */
    protected CourseStatistics(Course course) {
        this.courseId = course.getId();
        this.course = course;
    }


    public void recalculate(Set<CourseModule> modules) {
        calculateAverageModuleDuration(modules);
        calculateCompletionRate(modules);
        calculateDifficultyScore(modules);
        this.lastCalculated = LocalDateTime.now();
    }

    private void calculateAverageModuleDuration(Set<CourseModule> modules) {
        if (modules.isEmpty()) {
            this.averageModuleDuration = Duration.ZERO;
            return;
        }

        Duration totalDuration = modules.stream()
                .map(CourseModule::getDuration)
                .reduce(Duration.ZERO, Duration::plus);
        this.averageModuleDuration = totalDuration.dividedBy(modules.size());
    }

    private void calculateCompletionRate(Set<CourseModule> modules) {
        if (modules.isEmpty()) {
            this.completionRate = BigDecimal.ZERO;
            return;
        }

        long completedModules = modules.stream()
                .filter(module -> module.getStatus() == ModuleStatus.COMPLETED)
                .count();

        this.completionRate = BigDecimal.valueOf(completedModules)
                .divide(BigDecimal.valueOf(modules.size()), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private void calculateDifficultyScore(Set<CourseModule> modules) {
        if (modules.isEmpty()) {
            this.difficultyScore = BigDecimal.ZERO;
            return;
        }

        int totalDifficultyScore = modules.stream()
                .mapToInt(module -> module.getDifficultyLevel().getScore())
                .sum();

        this.difficultyScore = BigDecimal.valueOf(totalDifficultyScore)
                .divide(BigDecimal.valueOf(modules.size()), 2, RoundingMode.HALF_UP);
    }
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

