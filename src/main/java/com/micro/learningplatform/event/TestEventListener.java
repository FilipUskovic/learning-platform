package com.micro.learningplatform.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TestEventListener {

    @KafkaListener(topics = "${spring.kafka.topics.course-events}")
    public void handleTestEvent(String message) {
        log.info("Received test event: {}", message);
    }

    @KafkaListener(topics = "${spring.kafka.topics.dead-letter}")
    public void handleDlqEvent(String message) {
        log.info("Received DLQ event: {}", message);
    }
}
