package com.micro.learningplatform.services;

import com.micro.learningplatform.event.course.CourseStatusChangedEvent;
import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.CourseModule;
import com.micro.learningplatform.models.CourseStatisticHistory;
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
import org.hibernate.service.spi.ServiceException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public CourseResponse createCourse(CreateCourseRequest createCourseRequest) {
     /// Validacija poslovnih pravila
        validateNewCourseTitle(createCourseRequest.title());

        // Kreiranje kursa kroz factory metodu koja već postavlja
        // kategoriju i inicijalizira statistike
        Course course = Course.create(
                createCourseRequest.title(),
                createCourseRequest.description()
        );

        course = courseRepository.save(course);

        // Eventi se automatski registriraju kroz domenski model
        // Ne trebamo eksplicitno publishati event jer je već registriran u Course.create()

        return CourseMapper.toDTO(course);
    }

    @Override
    public CourseResponse getCourse(UUID courseId) {
        Course course = getCourseById(courseId);
        return CourseMapper.toDTO(course);
    }

    @Override
    public CourseResponse publishCourse(UUID courseId) {
        Course course = getCourseById(courseId);

        // Course.publish će:
        // 1. Validirati može li se kurs objaviti
        // 2. Promijeniti status
        // 3. Registrirati događaj
        course.publish();

        course = courseRepository.save(course);

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
        // Koristimo istu logiku kao i osnovna pretraga, ali s direktnim parametrima
        return courseRepository
                .advanceSearchCourses(searchTerm, status, pageable)
                .map(CourseMapper::toDTO);
    }

    @Override
    public List<CourseSearchResult> fullTextSearch(String searchTerm) throws RepositoryException {
        // Koristimo custom repository za full-text pretragu
        try {
            return customCourseRepo.fullTextSearch(searchTerm);
        } catch (Exception e) {
            log.error("Error during full text search: {}", e.getMessage());
            throw new RepositoryException("Full text search failed", e);
        }
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
    public void batchSaveCourses(List<CreateCourseRequest> requests) throws RepositoryException {
        validateBatchCourseTitles(requests);

        try {
            // Kreiramo Course objekte koristeći factory metodu
            List<Course> courses = requests.stream()
                    .map(request -> Course.create(
                            request.title(),
                            request.description()
                    ))
                    .toList();

            // Koristimo custom repository za batch save
            customCourseRepo.batchSave(courses);

            // Eventi su već registrirani kroz Course.create()
            log.info("Successfully saved {} courses in batch", courses.size());
        } catch (Exception e) {
            log.error("Error during batch save: {}", e.getMessage());
            throw new RepositoryException("Batch save failed", e);
        }
    }

    @Override
    public CourseStatisticsDTO getStatistics(UUID courseId) {
        Course course = getCourseById(courseId);

        // Koristimo i snapshot i detaljne statistike
        return new CourseStatisticsDTO(
                course.getId(),
                course.getTitle(),
                course.getStatisticsSnapshot().getTotalModules(),
                course.getStatisticsSnapshot().getTotalDuration(),
                course.getCourseStatistics().getLastCalculated(),
                course.getCourseStatistics().getCompletionRate(),
                course.getCourseStatistics().getDifficultyScore()
        );
    }

    @Override
    public CourseResponseWithModules getCourseWithModulesAndStatistics(UUID courseId) {
        Course course = courseRepository.findWithModulesById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        // Statistike se automatski računaju kroz domenski model
        return CourseMapper.toDTOWithModules(course);
    }

    @Override
    public void addModuleToCourse(UUID courseId, CreateModuleRequest request) {
        Course course = getCourseById(courseId);
        log.info("Adding module to course: {}", courseId);

        // CourseModule.create već validira zahtjev i registrira potrebne evente
        CourseModule module = CourseModule.create(request);

        // Course.addModule će:
        // 1. Validirati može li se modul dodati
        // 2. Ažurirati statistike (snapshot i detaljne)
        // 3. Kreirati povijesni zapis
        // 4. Registrirati događaj
        course.addModule(module);

        courseRepository.save(course);
        log.info("Module added successfully to course: {}", courseId);

    }

    @Override
    public CourseResponseWithModules createWithModule(CreateCourseRequest courseRequest, List<CreateModuleRequest> moduleRequests) {
        validateNewCourseTitle(courseRequest.title());

        // Kreiramo kurs
        Course course = Course.create(
                courseRequest.title(),
                courseRequest.description()
        );

        // Dodajemo module koristeći domensku logiku
        moduleRequests.forEach(moduleRequest -> {
            CourseModule module = CourseModule.create(moduleRequest);
            course.addModule(module);
        });

        Course savedCourse = courseRepository.save(course);
        return CourseMapper.toDTOWithModules(savedCourse);
    }

    @Override
    public void batchAddCourseWithModules(CreateCourseWithModulesRequest request) {
        log.info("Creating course with title: {} and {} modules",
                request.title(), request.modules().size());

        try {
            // Kreiramo kurs
            Course course = Course.create(
                    request.title(),
                    request.description()
            );

            // Dodajemo module
            request.modules().forEach(moduleRequest -> {
                CourseModule module = CourseModule.create(moduleRequest);
                course.addModule(module);
            });

            courseRepository.save(course);
            log.info("Successfully saved course with {} modules", request.modules().size());

        } catch (Exception e) {
            log.error("Error during batch course creation: {}", e.getMessage());
            throw new ServiceException("Failed to create course with modules", e);
        }

    }

    @Override
    public List<CourseStatisticHistory> getCourseHistory(UUID courseId, LocalDateTime startDate, LocalDateTime endDate) {
        Course course = getCourseById(courseId);
        return course.getStatisticHistory(startDate, endDate);
    }


    private Course getCourseById(UUID courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
    }

    private void validateNewCourseTitle(String title) {
        if (courseRepository.existsByTitleIgnoreCase(title)) {
            throw new CourseAlreadyExistsException(
                    "Course with title '" + title + "' already exists"
            );
        }
    }

    private void validateBatchCourseTitles(List<CreateCourseRequest> requests) {
        Set<String> titles = requests.stream()
                .map(CreateCourseRequest::title)
                .collect(Collectors.toSet());

        if (courseRepository.existsByTitleIgnoreCase(titles.toString())) {
            throw new CourseAlreadyExistsException(
                    "One or more courses already exist with the provided titles");
        }
    }


/*
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

 */


}
