package com.micro.learningplatform.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.Duration;

@Embeddable
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CourseStatisticsSnapshot {

    @Column(name = "total_modules")
    private int totalModules;

    @Column(name = "total_duration")
    private Duration totalDuration;

    // zaštitu za konkurentne operacije:
    public synchronized void  incrementModuleCount() {
        totalModules++;
    }

    public void decrementModuleCount() {
        totalModules--;
    }

    public void addDuration(Duration duration) {
        totalDuration = totalDuration.plus(duration);
    }

    public void subtractDuration(Duration duration) {
        totalDuration = totalDuration.minus(duration);
    }
}
