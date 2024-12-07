package com.micro.learningplatform.event.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;
    private final KafkaTopicProperties topicProperties;


    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory());
        template.setDefaultTopic(topicProperties.getCourseEvents());
        return template;
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(
                kafkaProperties.buildProducerProperties(null)
        );
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(null));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            KafkaErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD); // Dodano za potvrdu po zapisu

        // Konfiguracija rukovanja greškama i potvrde offseta
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD); // Potvrda po zapisu
        factory.getContainerProperties().setDeliveryAttemptHeader(true); // Dodavanje broja pokušaja isporuke
        return factory;
    }



}

