package com.micro.learningplatform.services;

import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.dto.CourseCreatedEvent;
import com.micro.learningplatform.models.dto.CourseResponse;
import com.micro.learningplatform.models.dto.CreateCourseRequest;
import com.micro.learningplatform.repositories.CourseRepository;
import com.micro.learningplatform.shared.CourseMapper;
import com.micro.learningplatform.shared.exceptions.CourseAlreadyExistsException;
import com.micro.learningplatform.shared.exceptions.CourseNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final ApplicationEventPublisher eventPublisher;


    public CourseServiceImpl(CourseRepository courseRepository, ApplicationEventPublisher eventPublisher) {
        this.courseRepository = courseRepository;
        this.eventPublisher = eventPublisher;
    }


    @Override
    @Transactional  // Override read-only jer modificiramo podatke
    public CourseResponse createCourse(CreateCourseRequest createCourseRequest) {
        // Provjera poslovnih pravila
        if (courseRepository.existsByTitleIgnoreCase(createCourseRequest.title())) {
            throw new CourseAlreadyExistsException(
                    "Course with title '" + createCourseRequest.title() + "' already exists"
            );
        }
        Course course = Course.create(createCourseRequest);
        course = courseRepository.save(course);
        eventPublisher.publishEvent(new CourseCreatedEvent(course.getId()));
        return CourseMapper.toDTO(course);
    }


    @Override
    public CourseResponse getCourse(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        return CourseMapper.toDTO(course);
    }

    @Override
    @Transactional
    public CourseResponse publishCourse(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        course.publish();
        course = courseRepository.save(course);
        //:TODO promjeniti CourseCreatedEvent u recod CoursePublishEvenet
        eventPublisher.publishEvent(new CourseCreatedEvent(courseId));

        return CourseMapper.toDTO(course);
    }
}
