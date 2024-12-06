package com.micro.learningplatform.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.micro.learningplatform.event.TestEvent;
import com.micro.learningplatform.event.TestEventRequest;
import com.micro.learningplatform.event.kafka.KafkaTopicProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Slf4j
public class KafkaController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopicProperties topicProperties;

    @PostMapping("/test/publish")
    public ResponseEntity<String> publishTestEvent(@RequestBody TestEventRequest request) {
        try {
            // Kreiramo test event
            TestEvent testEvent = new TestEvent(
                    UUID.randomUUID(),
                    request.message(),
                    LocalDateTime.now()
            );

            // Serializiramo u JSON
            String eventJson = objectMapper.writeValueAsString(testEvent);

            // Šaljemo na Kafka topic
            kafkaTemplate.send(topicProperties.getCourseEvents(),
                            testEvent.eventId().toString(),
                            eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send test event", ex);
                        } else {
                            log.info("Test event sent successfully: {}", result.getRecordMetadata());
                        }
                    });

            return ResponseEntity.ok("Event published successfully");
        } catch (Exception e) {
            log.error("Error publishing test event", e);
            return ResponseEntity.internalServerError().body("Failed to publish event: " + e.getMessage());
        }
    }

    @PostMapping("/test/dlq")
    public ResponseEntity<String> testDlq(@RequestBody TestEventRequest request) {
        try {
            // Namjerno kreiramo invalid JSON da izazovemo error
            String invalidJson = "{invalid-json";

            kafkaTemplate.send(topicProperties.getCourseEvents(),
                    UUID.randomUUID().toString(),
                    invalidJson);

            return ResponseEntity.ok("Invalid event sent - check DLQ");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Test failed: " + e.getMessage());
        }
    }

}
