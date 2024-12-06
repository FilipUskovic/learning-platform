package com.micro.learningplatform.event.kafka;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.kafka.topics")
@Getter
@Setter
public class KafkaTopicProperties {
    private static final Logger log = LogManager.getLogger(KafkaTopicProperties.class);

    private String courseEvents = "course-events";
    private String deadLetter = "course-events-dlq";
    private int retryCount = 3;
    private long retryBackoffMs = 1000;

    @PostConstruct
    public void logProperties() {
        log.info("Loaded kafka topics properties: courseEvents={}, deadLetter={}, retryCount={}, retryBackoffMs={}",
                courseEvents, deadLetter, retryCount, retryBackoffMs);
    }
}
