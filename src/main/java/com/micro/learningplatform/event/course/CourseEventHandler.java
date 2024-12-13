package com.micro.learningplatform.event.course;

import com.micro.learningplatform.models.CourseStatus;
import com.micro.learningplatform.models.dto.courses.CourseCreatedEvent;
import com.micro.learningplatform.models.dto.courses.CoursePublishedEvent;
import com.micro.learningplatform.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseEventHandler {

    private final NotificationService notificationService;


    /* pruza pbradu dogadaja uz entiete CourseCreatedEvent i CoursePublishedEvent sto sa ih napravio dto jel
       Pomaze decopuling radzdavaj logiku dogadaja od osnovne funkcionalsnoti

     */

    @EventListener
    @Async
    public void handleCourseCreated(CourseCreatedEvent event) {
        log.info("Processing course creation event for course: {}",
                event.courseId());

        notificationService.notifyAdministrators(
                String.format("New course created with ID: %s",
                        event.courseId())
        );
    }

    @EventListener
    @Async
    public void handleCoursePublished(CoursePublishedEvent event) {
        log.info("Processing course published event for course: {}",
                event.courseId());

        notificationService.notifyAboutStateChange(
                event.courseId(),
                CourseStatus.PUBLISHED
        );
    }
}
