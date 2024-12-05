package com.micro.learningplatform.controllers;

import com.micro.learningplatform.models.dto.BatchProcessingResult;
import com.micro.learningplatform.services.BatchProcessor;
import com.micro.learningplatform.services.BatchProcessorService;
import com.micro.learningplatform.shared.exceptions.BatchProcessingException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
public class BatchTestController {

    private final BatchProcessorService batchProcessorService;

    @PostMapping("/process")
    public BatchProcessingResult processBatch(@RequestBody BatchRequest request) throws BatchProcessingException {
        BatchProcessor<Object> processor = batch -> {
            for (Object item : batch) {
                if (item.toString().equalsIgnoreCase("errorItem")) {
                    throw new RuntimeException("Error processing item: " + item);
                }
                System.out.println("Processed item: " + item);
            }
        };

        BatchProcessorService.BatchProcessingOptions options = BatchProcessorService.BatchProcessingOptions.builder()
                .batchSize(request.getBatchSize())
                .clearEntityManagerAfterBatch(false)
                .progressInterval(2)
                .retryPolicy(BatchProcessorService.RetryPolicy.getDefault())
                .build();

        return batchProcessorService.processBatch(request.getItems(), processor, options);
    }


    @Setter
    @Getter
    public static class BatchRequest<T> {
        private List<T> items;
        private int batchSize;
        private BatchProcessor<T> processor;

    }


}
