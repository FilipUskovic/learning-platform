package com.micro.learningplatform.event.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaErrorHandler implements CommonErrorHandler {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;



    @Override
    public void handleBatch(Exception thrownException, ConsumerRecords<?, ?> data, Consumer<?, ?> consumer, MessageListenerContainer container, Runnable invokeListener) {
        log.error("Error processing Kafka batch: {}", data, thrownException);
        data.forEach(record -> {
            // Slanje svakog zapisa iz batch-a na DLQ
            sendToDlq(record, thrownException);
        });

        recordError(thrownException);
        // Poziv izvornog listenera ako želite nastaviti s obradom
        invokeListener.run();
    }

    @Override
    public boolean handleOne(Exception thrownException, ConsumerRecord<?, ?> record,
                             Consumer<?, ?> consumer, MessageListenerContainer container) {
        log.error("Error processing Kafka message: {}", record, thrownException);
        sendToDlq(record, thrownException);
        recordError(thrownException);
        return true;
    }



    private void sendToDlq(ConsumerRecord<?, ?> data, Exception e) {
        try {
            DeadLetterMessage dlqMessage = new DeadLetterMessage(
                    data.value(),
                    e.getMessage(),
                    LocalDateTime.now()
            );

            String deadLetterJson = objectMapper.writeValueAsString(dlqMessage);


            kafkaTemplate.send(
                    topicProperties.getDeadLetter(),
                    data.key() != null ? data.key().toString() : null,
                    deadLetterJson
            );
        } catch (Exception ex) {
            log.error("Failed to send to DLQ", ex);
        }
    }

    private void recordError(Exception e) {
        meterRegistry.counter("kafka.error",
                "type", e.getClass().getSimpleName()
        ).increment();
    }


}

