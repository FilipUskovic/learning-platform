package com.micro.learningplatform.services;

import com.google.common.collect.Lists;
import com.micro.learningplatform.batch.BatchItemError;
import com.micro.learningplatform.models.dto.BatchProcessingResult;
import com.micro.learningplatform.shared.exceptions.BatchProcessingException;
import com.micro.learningplatform.shared.exceptions.BatchProcessingInterruptedException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.StaleObjectStateException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchProcessorService {

    // pruža centralizirano i optimizirano rukovanje batch operacijama

    private final EntityManager entityManager;
    private final MeterRegistry meterRegistry;

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    private int defaultBatchSize;

    @Value("${spring.task.execution.pool.queue-capacity}")
    private int queueCapacity;

    private final Set<String> knownErrors = new HashSet<>();


    // metoda za batch procesiranje koja osigurava optimalne performanse i pravilno upravljanje resursima.
    @Transactional
    public <T> BatchProcessingResult processBatch(List<T> items, BatchProcessor<T> processor, BatchProcessingOptions processingOptions) throws BatchProcessingException {

        validateBatchParameters(items);
        Timer.Sample timer = Timer.start(meterRegistry);

        BatchProcessingResult result = new BatchProcessingResult();

        try {
            List<List<T>> batches = partitionItems(items, processingOptions);
            log.info("Partitioned items into {} batches", batches.size());

            BatchProgressTracker batchProgressTracker = new BatchProgressTracker(
                    items.size(),
                    processingOptions.getProgressInterval()
            );

            for (List<T> batch : batches) {
                processSingleBatch(batch, processor, result, batchProgressTracker);
                log.info("Processing batch of {} items", batch.size());

                clearEntityManagerIfNeeded(processingOptions);
            }

            recordBatchMetrics(timer, items.size(), result);
            result.complete();
            log.info("Batch processing completed successfully: {}", result);

            return result;

        }catch (Exception e) {
            handleBatchError(e, result);
            throw new BatchProcessingException("Batch processing failed", e);
        }


    }

    /**
     * Optimalno particionira items za batch processing
     */
    private <T> List<List<T>> partitionItems(
            List<T> items,
            BatchProcessingOptions options) {

        int batchSize = options.getBatchSize() != null ?
                options.getBatchSize() : defaultBatchSize;

        return Lists.partition(items, batchSize);
    }

    private void clearEntityManagerIfNeeded(BatchProcessingOptions options) {
        if (options.isClearEntityManagerAfterBatch()) {
            entityManager.flush();
            entityManager.clear();
        }
    }

    private <T> void processSingleBatch(List<T> batch, BatchProcessor<T> processor, BatchProcessingResult result, BatchProgressTracker batchProgressTracker) {
        Timer.Sample batchTimer = Timer.start(meterRegistry);

        try {
            // Obrada batch-a
            processor.process(batch);
            result.incrementSuccessCount(batch.size());
            batchProgressTracker.updateProgress(batch.size());

            recordSingleBatchMetrics(batchTimer, batch.size(), result);

        } catch (Exception e) {
            log.error("Error processing batch of {} items. Failed items: {}. Error: {}",
                    batch.size(),
                    batch,
                    e.getMessage(), e);


            handleBatchItemError(e, batch, result);

            // Pokušaj pojedinačne obrade, ako je konfigurirano
            if (shouldProcessIndividually(e)) {
                processItemsIndividually(batch, processor, result);
            }

            // Opcionalno: nastavak bez bacanja RuntimeException
            log.warn("Skipping failed batch. Continuing with remaining batches...");

        }
    }

    /**
     * Bilježi metrike za batch processing
     */
    private void recordBatchMetrics(
            Timer.Sample timer,
            int totalItems,
            BatchProcessingResult result) {

       // Završavamo mjerenje vremena
        timer.stop(Timer.builder("batch.processing")
                .tag("status", result.isSuccessful() ? "success" : "partial_failure")
                .tag("operation", "processBatch")
                .register(meterRegistry));

        meterRegistry.gauge("batch.success_rate",
                Tags.of("operation", "processBatch"),
                result.getSuccessRate());

        // Brojači za uspješne i neuspješne unose
        meterRegistry.counter("batch.items",
                        "status", "success",
                        "operation", "processBatch")
                .increment(result.getSuccessCount());

        meterRegistry.counter("batch.items",
                        "status", "failure",
                        "operation", "processBatch")
                .increment(result.getFailureCount());
    }

    /**
     * Procesira pojedinačne iteme kad batch processing ne uspije
     */
    private <T> void processItemsIndividually(
            List<T> batch,
            BatchProcessor<T> processor,
            BatchProcessingResult result) {

        for (T item : batch) {
            try {
                processor.process(Collections.singletonList(item));
                result.incrementSuccessCount(1);
            } catch (Exception e) {
                log.error("Failed to process individual item: {}", item, e);
                result.incrementFailureCount(1);
                result.addError(new BatchItemError(item, e));
            }
        }
    }




    /**
     * Centralizirano rukovanje greškama za cijeli batch
     * Bilježi greške i ažurira metrike
     */
    private void handleBatchError(Exception e, BatchProcessingResult result) {
        log.error("Batch processing failed: {}", e.getMessage(), e);

        meterRegistry.counter("batch.error",
                "type", e.getClass().getSimpleName(),
                "message", e.getMessage()
        ).increment();

        result.addError(new BatchItemError(null, e));
        result.complete();
    }



   /* Rukovanje greškama za pojedinačni batch
    * Odlučuje treba li pokušati pojedinačnu obradu
    */
    private void handleBatchItemError(Exception e, List<?> batch, BatchProcessingResult result) {
        String errorType = e.getClass().getSimpleName();
        if (!knownErrors.contains(errorType)) {
            knownErrors.add(errorType);

            // Logiraj novu vrstu greške
            log.error("Encountered new error type: {}", errorType);
        }
        log.error("Error processing batch of {} items. Failed items: {}. Error: {}",
                batch.size(),
                batch,
                e.getMessage(), e);

        // Ažuriraj rezultat obrade
        result.incrementFailureCount(batch.size());
        result.addError(new BatchItemError(batch, e));

        // Bilježenje metričkih podataka s dosljednim oznakama
        meterRegistry.counter(
                "batch.error",
                "type", e.getClass().getSimpleName(),
                "batchSize", String.valueOf(batch.size())
        ).increment();
    }

    private void validateBatchParameters(List<?> items) {
        if (items == null) {
            throw new IllegalArgumentException("Items list cannot be null");
        }
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Items list cannot be empty");
        }
        if (items.size() > queueCapacity) {
            throw new IllegalArgumentException(
                    String.format("Batch size %d exceeds maximum queue capacity %d",
                            items.size(), queueCapacity)
            );
        }
    }

    /**
     * Određuje treba li pokušati pojedinačnu obradu nakon batch greške
     * Bazira odluku na tipu greške i retry politici
     */
    private boolean shouldProcessIndividually(Exception e) {
        // Provjeravamo je li greška vezana za batch procesiranje
        return e instanceof OptimisticLockException || e instanceof StaleObjectStateException;
    }


    private void recordSingleBatchMetrics(Timer.Sample timer, int batchSize, BatchProcessingResult result) {
        timer.stop(Timer.builder("batch.single")
                .tag("size", String.valueOf(batchSize))
                .register(meterRegistry));

        meterRegistry.counter("batch.processed",
                "size", String.valueOf(batchSize)
        ).increment();
    }



    @Getter
    @Builder
    public static class BatchProcessingOptions {
        private final Integer batchSize;
        private final boolean clearEntityManagerAfterBatch;
        private final int progressInterval;
        private final RetryPolicy retryPolicy;

        public static BatchProcessingOptions getDefault() {
            return BatchProcessingOptions.builder()
                    .clearEntityManagerAfterBatch(true)
                    .progressInterval(1000)
                    .retryPolicy(RetryPolicy.getDefault())
                    .build();
        }
    }

    @Builder
    @Getter
    public static class RetryPolicy {
        private final int maxRetries;
        private final Duration delay;
        private final List<Class<? extends Exception>> retryableExceptions;

        public static RetryPolicy getDefault() {
            return RetryPolicy.builder()
                    .maxRetries(3)
                    .delay(Duration.ofSeconds(1))
                    .retryableExceptions(Arrays.asList(
                            OptimisticLockException.class,
                            StaleObjectStateException.class))
                    .build();
        }

        public boolean shouldRetry(Exception e, int attempts) {
            return attempts < maxRetries &&
                    retryableExceptions.stream()
                            .anyMatch(ex -> ex.isInstance(e));
        }
    }

    /**
     * Prati napredak batch processinga
     */
    @Getter
    private static class BatchProgressTracker {
        private final int totalItems;
        private final int progressInterval;
        private int processedItems = 0;
        private final LocalDateTime startTime = LocalDateTime.now();
        private volatile boolean shouldStop = false;


        public BatchProgressTracker(int totalItems, int progressInterval) {
            this.totalItems = totalItems;
            this.progressInterval = progressInterval;
        }

        public void updateProgress(int itemsProcessed) {
            processedItems += itemsProcessed;
            if (shouldReportProgress()) {
                logProgress();
            }
        }

        public void stopProcessing() {
            this.shouldStop = true;
        }

        public void checkShouldContinue() throws BatchProcessingInterruptedException {
            if (shouldStop) {
                throw new BatchProcessingInterruptedException("Processing was interrupted");
            }
        }

        private boolean shouldReportProgress() {
            return (processedItems % progressInterval) == 0 ||
                    processedItems == totalItems;
        }

        private void logProgress() {
            double percentage = (double) processedItems / totalItems * 100;
            Duration elapsed = Duration.between(startTime, LocalDateTime.now());

            // Procjena preostalog vremena
            Duration estimated = elapsed.multipliedBy(totalItems)
                    .dividedBy(processedItems);
            Duration remaining = estimated.minus(elapsed);

            log.info("Batch progress: {}% ({}/{} items) - Time elapsed: {}s - Estimated remaining: {}s",
                    String.format("%.2f", percentage),
                    processedItems,
                    totalItems,
                    elapsed.getSeconds(),
                    remaining.getSeconds());
        }
    }
}

