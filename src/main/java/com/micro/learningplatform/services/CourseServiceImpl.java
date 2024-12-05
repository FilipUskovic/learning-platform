package com.micro.learningplatform.services;

import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.CourseStatus;
import com.micro.learningplatform.models.dto.*;
import com.micro.learningplatform.repositories.CourseRepository;
import com.micro.learningplatform.repositories.CustomCourseRepoImpl;
import com.micro.learningplatform.shared.CourseMapper;
import com.micro.learningplatform.shared.exceptions.CourseAlreadyExistsException;
import com.micro.learningplatform.shared.exceptions.CourseNotFoundException;
import com.micro.learningplatform.shared.exceptions.RepositoryException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CustomCourseRepoImpl customCourseRepo;



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
        eventPublisher.publishEvent(new CoursePublishedEvent(courseId));

        return CourseMapper.toDTO(course);
    }

    @Override
    public Page<CourseResponse> search(CourseSearchRequest searchRequest) {
        return courseRepository
                .advanceSearchCourses(
                        searchRequest.searchTerm(),
                        searchRequest.status(),
                        searchRequest.getPageable()
                )
                .map(CourseMapper::toDTO);
    }

    @Override
    public Page<CourseResponse> advancedSearch(String searchTerm, CourseStatus status, Pageable pageable) {
        return courseRepository
                .advanceSearchCourses(searchTerm, status, pageable)
                .map(CourseMapper::toDTO);
    }

    @Override
    public List<CourseSearchResult> fullTextSearch(String searchTerm) throws RepositoryException {
        return customCourseRepo.fullTextSearch(searchTerm);
    }

    @Override
    public Page<CourseResponse> findByStatus(CourseStatus status, Pageable pageable) {
        return courseRepository
                .findByStatus(status, pageable)
                .map(CourseMapper::toDTO);
    }

    @Override
    public CourseResponse getCourseWithModules(UUID id) {
        Course course = courseRepository.findWithModulesById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
        return CourseMapper.toDTO(course);
    }

    @Override
    @Transactional

    public void batchSaveCourses(List<CreateCourseRequest> requests) throws RepositoryException {
        // Prvo provjeravamo postoje li duplikati
        List<String> titles = requests.stream()
                .map(CreateCourseRequest::title)
                .toList();

        if (courseRepository.existsByTitleIgnoreCase(titles.get(0))) {
            throw new CourseAlreadyExistsException(
                    "One or more courses already exist with the provided titles");
        }

        // Kreiramo Course objekte
        List<Course> courses = requests.stream()
                .map(Course::create)
                .toList();

        // Koristimo custom repository za batch save
        customCourseRepo.batchSave(courses);

        // Objavljujemo evente za svaki kreirani kurs
        courses.forEach(course ->
                eventPublisher.publishEvent(new CourseCreatedEvent(course.getId()))
        );
    }


}
