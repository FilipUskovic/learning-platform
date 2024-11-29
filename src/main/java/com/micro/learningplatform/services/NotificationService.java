package com.micro.learningplatform.services;

import com.micro.learningplatform.models.CourseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class NotificationService {

    public void notifyAdministrators(String message) {
        // U pravoj implementaciji, ovo bi slalo email ili drugo obavijest
        // Za sada samo logiramo
        //TODO: dodati mail slanje obavjesti
        log.info("Admin notification: {}", message);
    }

    public void notifyAboutStateChange(UUID courseId, CourseStatus newStatus) {
        log.info("Course {} changed status to: {}", courseId, newStatus);
    }
}
