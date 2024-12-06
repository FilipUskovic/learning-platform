package com.micro.learningplatform.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.micro.learningplatform.event.kafka.KafkaTopicProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventHanlder {

    // koristimo funkcionalni pristup rukovanju događajim  i optimizirano upravljanje greškama
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopicProperties topicProperties;



    private static final String COURSE_EVENTS_TOPIC = "course-events";
    private static final String DLQ_TOPIC = "course-events-dlq";


    @Async
    @EventListener
    public void handleDomainEvent(DomainEvent event) {
        processEvent(event)
                .ifSuccess(this::publishToKafka)
                .ifPresent(this::recordSuccess);
    }

    private Result<EventResult, EventError> processEvent(DomainEvent event) {
        return Result.of(() -> {
            try {
                String eventJson = objectMapper.writeValueAsString(event);
                return new EventResult(
                        event.getEventId(),
                        eventJson,
                        LocalDateTime.now()
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void publishToKafka(EventResult result) {
        kafkaTemplate.send(
                topicProperties.getCourseEvents(),
                result.eventId().toString(),
                result.eventJson()
        );
    }

    private void recordSuccess(EventResult result) {
        meterRegistry.counter("kafka.events",
                "status", "success",
                "topic", topicProperties.getCourseEvents()
        ).increment();
        log.debug("Event processed: {}", result);
    }

}
