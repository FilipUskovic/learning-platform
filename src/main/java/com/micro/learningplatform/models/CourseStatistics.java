package com.micro.learningplatform.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Getter
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
}
