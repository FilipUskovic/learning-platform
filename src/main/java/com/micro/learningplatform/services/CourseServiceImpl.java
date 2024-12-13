package com.micro.learningplatform.services;

import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.CourseModule;
import com.micro.learningplatform.models.CourseStatus;
import com.micro.learningplatform.models.dto.courses.*;
import com.micro.learningplatform.models.dto.module.CreateModuleRequest;
import com.micro.learningplatform.repositories.CourseRepository;
import com.micro.learningplatform.repositories.CustomCourseRepoImpl;
import com.micro.learningplatform.shared.CourseMapper;
import com.micro.learningplatform.shared.exceptions.CourseAlreadyExistsException;
import com.micro.learningplatform.shared.exceptions.CourseNotFoundException;
import com.micro.learningplatform.shared.exceptions.RepositoryException;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.micro.learningplatform.models.Course.create;
import static com.micro.learningplatform.shared.CourseModuleMapper.toResponseWithModules;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private static final Logger log = LogManager.getLogger(CourseServiceImpl.class);
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
        Course course = create(createCourseRequest);
        course = courseRepository.save(course);
        eventPublisher.publishEvent(new CourseCreatedEvent(course.getId()));
        return CourseMapper.toDTO(course);
    }


    @Override
    @Cacheable(cacheNames = "courses", key = "#courseId")
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

        if (courseRepository.existsByTitleIgnoreCase(titles.getFirst())) {
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

    @Override
    @Transactional(readOnly = true)
    public CourseStatisticsDTO getStatistics(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        return new CourseStatisticsDTO(
                course.getId(),
                course.getTitle(),
                course.getStatistics().getTotalModules(),
                course.getStatistics().getTotalDuration(),
                course.getStatistics().getLastCalculated()
        );
    }


    @Override
    @Transactional(readOnly = true)
    public CourseResponseWithModules getCourseWithModulesAndStatistics(UUID courseId) {
        Course course = courseRepository.findWithModulesById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        // Osigurajte da se statistika ažurira prije vraćanja
        course.getStatistics().recalculate(course.getModules());

        return CourseMapper.toDTOAdvance(course);
    }

    // todo razmisliti zelim li ne dosusiti duplikate
    @Override
    @Transactional
    public void addModuleToCourse(UUID courseId, CreateModuleRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        log.info("Fetched course: {}", course.getId());

        CourseModule module = CourseModule.create(request);
        log.info("Created module: {}", module);

        // Dodajte modul direktno u postojeću kolekciju
        if (course.getModules().stream().noneMatch(existingModule ->
                existingModule.getTitle().equalsIgnoreCase(module.getTitle()))) {
            course.addModule(module);
        } else {
            log.warn("Module with the same title already exists: {}", module.getTitle());
        }

        // Perzistiraj kolegij
        courseRepository.save(course);
        log.info("Course saved successfully with modules.");
    }

    @Override
    @Transactional
    public CourseResponseWithModules createWithModule(
            CreateCourseRequest createCourseRequest,
            List<CreateModuleRequest> moduleRequests) {
        if (courseRepository.existsByTitleIgnoreCase(createCourseRequest.title())) {
            throw new CourseAlreadyExistsException(
                    "Course with title '" + createCourseRequest.title() + "' already exists"
            );
        }
        Course newCourse = create(createCourseRequest);
        List<CourseModule> modules = moduleRequests.stream()
                .map(request -> {
                    CourseModule module = CourseModule.create(request);
                    module.setCourse(newCourse); // Postavi kolegij za modul
                    return module;
                })
                .toList();

        newCourse.getModules().addAll(modules);

        Course savedCourse = courseRepository.save(newCourse);

        return toResponseWithModules(savedCourse);
    }

    @Override
    @Transactional
    public void batchAddCourseWithModules(CreateCourseWithModulesRequest request) {
        log.info("Creating course with title: {}", request.title());

        Course course = Course.create(new CreateCourseRequest(request.title(), request.description()));

        request.modules().forEach(moduleRequest -> {
            CourseModule module = CourseModule.create(moduleRequest);
            course.addModule(module);
            log.info("Added module: {} to course: {}", module.getTitle(), course.getTitle());
        });

        courseRepository.save(course);
        log.info("Course with modules saved successfully.");
    }


}
