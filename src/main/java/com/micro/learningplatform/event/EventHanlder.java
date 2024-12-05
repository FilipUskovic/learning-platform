package com.micro.learningplatform.event;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventHanlder {

    // koristimo funkcionalni pristup rukovanju događajim  i optimizirano upravljanje greškama
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;
   // private final DeadLetterQueue deadLetterQueue;



   // Procesira događaj koristeći Result pattern za upravljanje greškam
/*
    public void handleDomainEvent(DomainEvent event) {
        processEvent(event)
                .ifSuccess(this::publishSuccessMetrics)
                .ifError(this::handleEventError)
                .ifPresent(result -> log.debug("Event processed: {}", result));
    }

 */
}
