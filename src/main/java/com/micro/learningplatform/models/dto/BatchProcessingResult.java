package com.micro.learningplatform.models.dto;

import com.micro.learningplatform.batch.BatchItemError;
import com.micro.learningplatform.batch.BatchProcessingSummary;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
public class BatchProcessingResult{
    private int successCount = 0;
    private int failureCount = 0;
    private final List<BatchItemError> errors = new ArrayList<>();
    private final LocalDateTime startTime = LocalDateTime.now();
    private LocalDateTime endTime;

    public void incrementSuccessCount(int count) {
        this.successCount += count;
    }

    public void incrementFailureCount(int count) {
        this.failureCount += count;
    }

    public void addError(BatchItemError error) {
        this.errors.add(error);
    }

    public void complete() {
        this.endTime = LocalDateTime.now();
    }

    public Duration getDuration() {
        return Duration.between(startTime,
                endTime != null ? endTime : LocalDateTime.now());
    }

    public double getSuccessRate() {
        int total = successCount + failureCount;
        return total == 0 ? 0 : (double) successCount / total;
    }

    public boolean isSuccessful() {
        return failureCount == 0;
    }

    public BatchProcessingSummary getSummary() {
        return new BatchProcessingSummary(
                successCount,
                failureCount,
                getDuration(),
                getSuccessRate(),
                errors.size()
        );
    }

}
