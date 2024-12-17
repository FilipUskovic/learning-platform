package com.micro.learningplatform.services;

import com.micro.learningplatform.models.*;
import com.micro.learningplatform.models.dto.DifficultyLevel;
import com.micro.learningplatform.models.dto.courses.*;
import com.micro.learningplatform.models.dto.module.CreateModuleRequest;
import com.micro.learningplatform.models.dto.module.ModuleDetailResponse;
import com.micro.learningplatform.repositories.CourseRepository;
import com.micro.learningplatform.repositories.CustomCourseRepoImpl;
import com.micro.learningplatform.repositories.ModuleRepositroy;
import com.micro.learningplatform.shared.CourseMapper;
import com.micro.learningplatform.shared.exceptions.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;



@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private static final Logger log = LogManager.getLogger(CourseServiceImpl.class);
    private final CourseRepository courseRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;
    private final CustomCourseRepoImpl customCourseRepo;
    private final ModuleRepositroy moduleRepository;

    // TODO dodati update i delete metode, te provjeriti jos jednom svaku metodu i validacije

    @Override
    @Transactional
    public CourseResponse createCourse(CreateCourseRequest createCourseRequest) {
        log.debug("Creating new course with title: {}", createCourseRequest.title());

        if (courseRepository.existsByTitleIgnoreCase(createCourseRequest.title())) {
            log.warn("Attempt to create course with existing title: {}", createCourseRequest.title());
            throw new CourseAlreadyExistsException(
                    "Course with title '" + createCourseRequest.title() + "' already exists"
            );
        }
        Course course = Course.create(createCourseRequest.title(), createCourseRequest.description());
        //  course.assignAuthor(createCourseRequest.authorId());
        course.setDifficultyLevel(createCourseRequest.getDifficultyLevelEnum());
        course = courseRepository.save(course);

        // Event je već registriran kroz Course.create()

        log.info("Successfully created course with ID: {}", course.getId());
        return CourseMapper.toDTO(course);
    }

    @Override
    @Cacheable(cacheNames = "courses", key = "#courseId")
    public CourseResponse getCourse(UUID courseId) {
        log.debug("Fetching course by ID: {}", courseId);
        Course course = findCourseById(courseId);
        return CourseMapper.toDTO(course);
    }

    @Override
    @Transactional
    public CourseResponse publishCourse(UUID courseId) {
        Course course = findCourseWithModuleById(courseId);

        log.debug("Modules in course: {}", course.getModules());

        // Poslovna logika unutar domenskog modela
        course.publish();

        log.info("Successfully published course with ID: {}", courseId);

        return CourseMapper.toDTO(course);
    }

    @Override
    public Page<CourseResponse> search(CourseSearchRequest searchRequest) {
        log.debug("Performing search with request: {}", searchRequest);
        Page<CourseResponse> result = courseRepository
                .advanceSearchCourses(searchRequest.searchTerm(),
                                      searchRequest.status(),
                                      searchRequest.getPageable())
                .map(CourseMapper::toDTO);

        if (result.isEmpty()) {
            throw new ResourceNotFoundException("No resources found for the given search criteria.");
        }

        return result;
    }

    // todo vidjeli treba li mi ovaj serach isto posto imam dva ista prakticiki
    // te ovo ispraviti     "detail": "Failed to convert 'status' with value: 'draft'",
    @Override
    public Page<CourseResponse> advancedSearch(String searchTerm, CourseStatus status, Pageable pageable) {

        Page<CourseResponse> result = courseRepository
                .advanceSearchCourses(searchTerm, status, pageable)
                .map(CourseMapper::toDTO);

        if (result.isEmpty()) {
            throw new ResourceNotFoundException("No resources found for the given search criteria.");
        }

        return result;
    }

    //todo vijedti zasto mi vraca za course name id
    @Override
    public List<CourseSearchResult> fullTextSearch(String searchTerm) throws RepositoryException {
        log.debug("Performing full text search with term: {}", searchTerm);
        log.debug("Performing full text search with term: {}", searchTerm);

        List<CourseSearchResult> results = customCourseRepo.fullTextSearch(searchTerm);

        if (results.isEmpty()) {
            throw new NoResultException("No results found for the provided search term.");
        }
        return results;
    }

    @Override
    public Page<CourseResponse> findByStatus(CourseStatus status, Pageable pageable) {
        Page<CourseResponse> result = courseRepository
                .findByStatus(status, pageable)
                .map(CourseMapper::toDTO);

        if (result.isEmpty()) {
            throw new NoResultException("No courses found for the provided status: " + status);
        }
        return result;
    }


    @Override
    public CourseResponseWithModules getCourseWithModules(UUID courseId) {
        log.debug("Fetching course with modules for ID: {}", courseId);
        Course course = courseRepository.findWithModulesById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        return CourseMapper.toCourseWithModulesResponse(course);
    }

    @Override
    @Transactional
    public void batchSaveCourses(List<CreateCourseRequest> requests) throws RepositoryException {
        log.debug("Batch saving {} courses", requests.size());
        validateBatchCourseTitles(requests);

        try {
            List<Course> courses = requests.stream()
                    .map(request -> {
                        Course course = Course.create(request.title(), request.description());
                        course.setDifficultyLevel(request.getDifficultyLevelEnum());
                        return course;
                    })
                    .toList();
            customCourseRepo.batchSave(courses);
            log.info("Successfully batch saved {} courses", courses.size());

        } catch (Exception e) {
            log.error("Batch save failed", e);
            throw new RepositoryException("Failed to batch save courses", e);
        }

    }

    @Override
    public CourseStatisticsDTO getStatistics(UUID courseId) {
        log.debug("Fetching statistics for course: {}", courseId);
        Course course = findCourseById(courseId);

        return new CourseStatisticsDTO(
                course.getStatisticsSnapshot().getTotalModules(),
                course.getStatisticsSnapshot().getTotalDuration(),
                course.getCourseStatistics().getAverageModuleDuration(),
                course.getCourseStatistics().getCompletionRate(),
                course.getCourseStatistics().getDifficultyScore(),
                course.getCourseStatistics().getLastCalculated()
        );

    }

    @Override
    public CourseResponseWithModules getCourseWithModulesAndStatistics(UUID courseId) {
        log.debug("Fetching course with modules and statistics for ID: {}", courseId);
        Course course = courseRepository.findWithModulesById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        // Osiguravamo da su statistike ažurne
        course.getCourseStatistics().recalculate(course.getModules());

        return CourseMapper.toCourseWithModulesResponse(course);
    }

    //TODO prequsitide spremati u tablicu
    @Override
    @Transactional
    public void addModuleToCourse(UUID courseId, CreateModuleRequest request) {
        log.debug("Adding module to course: {}", courseId);

        // Dohvati tečaj
        Course course = findCourseWithModuleById(courseId);
        log.debug("Course state after fetch: {}", course.getCourseStatus());
        log.debug("Modules in course after fetch: {}", course.getModules());

        // Kreiraj modul
        CourseModule module = CourseModule.create(request);
        log.debug("New module created: {}", module);

        // Provjeri duplikat naslova
        boolean duplicateTitleExists = course.getModules().stream()
                .anyMatch(existingModule -> existingModule.getTitle().equalsIgnoreCase(module.getTitle()));
        if (duplicateTitleExists) {
            log.error("Module with the same title already exists: {}", module.getTitle());
            throw new IllegalArgumentException("Module with the same title already exists");
        }

        // Postavi `difficultyLevel` ako nije specificiran
        if (module.getDifficultyLevel() == null) {
            module.setDifficultyLevel(course.getDifficultyLevel());
            log.debug("Fallback to course difficulty level: {}", module.getDifficultyLevel());
        }

        // Postavi sequence number i validiraj
        assignSequenceNumber(course, module);

        // Dodaj modul u tečaj (koristi domensku metodu)
        course.addModule(module);
        log.debug("Module added to course: {}", module);

        // Spremi tečaj
        courseRepository.saveAndFlush(course); // Koristi saveAndFlush za sinkronizaciju
        log.info("Successfully added module to course: {}", courseId);
    }


    @Override
    @Transactional
    public CourseResponseWithModules createWithModule(CreateCourseRequest courseRequest, CreateModuleRequest moduleRequest) {
        log.debug("Creating course with {} modules", moduleRequest);

        if (courseRepository.existsByTitleIgnoreCase(courseRequest.title())) {
            throw new CourseAlreadyExistsException(
                    "Course with title '" + courseRequest.title() + "' already exists"
            );
        }

        Course course = Course.create(courseRequest.title(), courseRequest.description());
        course.setDifficultyLevel(courseRequest.getDifficultyLevelEnum());
        log.debug("Course created: {}", course);
       // course.assignAuthor(courseRequest.authorId());

        CourseModule module = CourseModule.create(moduleRequest);
        checkForDuplicateTitle(course, module);
        setDifficultyLevelIfNotSpecified(course, module);
        assignSequenceNumber(course, module);

        course.addModule(module);

        Course savedCourse = courseRepository.save(course);
        log.info("Successfully created course with module, ID: {}", savedCourse.getId());

        return CourseMapper.toCourseWithModulesResponse(savedCourse);
    }

    @Override
    @Transactional
    public void batchAddCourseWithModules(CreateCourseWithModulesRequest request) {
        log.debug("Creating course with title: {} and {} modules",
                request.title(), request.modules().size());

        Course course = Course.create(request.title(), request.description());
        course.setDifficultyLevel(DifficultyLevel.valueOf(request.difficultyLevel()));

        List<CourseModule> modules = request.modules().stream()
                .map(moduleRequest -> {
                    CourseModule module = CourseModule.create(moduleRequest);
                    checkForDuplicateTitle(course, module);
                    setDifficultyLevelIfNotSpecified(course, module);
                    assignSequenceNumber(course, module);
                    return module;
                }).toList();

        log.debug("Modules in course after saving: {}", course.getModules().size());
        moduleRepository.saveAll(modules);
        modules.forEach(course::addModule);

        courseRepository.save(course);
        log.info("Successfully created course with {} modules", request.modules().size());

    }

    @Override
    public List<CourseStatisticHistory> getCourseHistory(UUID courseId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching course history for course: {} between {} and {}",
                courseId, startDate, endDate);
        Course course = findCourseById(courseId);
        return course.getStatisticHistory(startDate, endDate);
    }

    @Override
    public ModuleDetailResponse getModuleDetails(UUID moduleId) {
        CourseModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ModuleNotFoundException(moduleId));

        // Optimizirano učitavanje svih prerequisita u jednom upitu
        Map<UUID, CourseModule> prerequisiteModules =
                moduleRepository.findAllById(module.getPrerequisites())
                        .stream()
                        .collect(Collectors.toMap(
                                CourseModule::getId,
                                Function.identity()
                        ));

        return CourseMapper.toModuleDetailResponse(module, prerequisiteModules);
    }

    @Override
    public List<CourseResponse> getRecentCoursesByStatus(CourseStatus status) {
        List<CourseResponse> statues = courseRepository.findByStatusOrderByCreatedAt(status)
                .stream()
                .map(CourseMapper::toDTO)
                .toList();
        if(statues.isEmpty()){
            throw new NoResultException("No results found for with status: " + status);
        }
        return statues;

    }


    // pretraga po kategoriji i težini koristeći kompozitni indeks
    @Override
    public List<CourseResponse> findByCategoryAndDifficultyLevel(String category, String level) {
        return courseRepository.findByCategoryAndDifficultyLevel(category, level)
                .stream()
                .map(CourseMapper::toDTO)
                .toList();
    }

    @Override
    public Page<CourseResponse> searchByTerm(String searchTerm, Pageable pageable) {
        return courseRepository.searchCourses(searchTerm, pageable)
                .map(CourseMapper::toDTO);
    }


    private Course findCourseById(UUID courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
    }

    private Course findCourseWithModuleById(UUID courseId) {
        return courseRepository.findByIdWithModules(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
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

    private void checkForDuplicateTitle(Course course, CourseModule module) {
        boolean duplicateTitleExists = course.getModules().stream()
                .anyMatch(existingModule -> existingModule.getTitle().equalsIgnoreCase(module.getTitle()));
        if (duplicateTitleExists) {
            log.error("Module with the same title already exists: {}", module.getTitle());
            throw new IllegalArgumentException("Module with the same title already exists");
        }
    }

    private void setDifficultyLevelIfNotSpecified(Course course, CourseModule module) {
        if (module.getDifficultyLevel() == null) {
            module.setDifficultyLevel(course.getDifficultyLevel());
            log.debug("Fallback to course difficulty level: {}", module.getDifficultyLevel());
        }
    }

    private void assignSequenceNumber(Course course, CourseModule module) {
        Set<CourseModule> existingModules = course.getModules(); // Koristi module iz entiteta
        log.info("Existing modules from course entity: {}", existingModules);

        if (module.getSequenceNumber() == null) {
            int maxSequenceNumber = existingModules.stream()
                    .mapToInt(CourseModule::getSequenceNumber)
                    .max()
                    .orElse(0);
            module.setSequenceNumber(maxSequenceNumber + 1);
            log.info("Assigned new sequenceNumber: {}", module.getSequenceNumber());
        }

        boolean exists = existingModules.stream()
                .anyMatch(existingModule -> existingModule.getSequenceNumber().equals(module.getSequenceNumber()));
        if (exists) {
            log.error("Duplicate sequenceNumber {} for module {}", module.getSequenceNumber(), module.getTitle());
            throw new IllegalArgumentException("Module with the same sequence number already exists");
        }
    }



    // TODO smanjti broj slicni metoda npr dodati opcionalne parametere s bolje razrađenimk reopstirojem
/*
    private Course fetchCourse(UUID id, boolean includeModules, boolean includeStatistics) {
        if (includeModules && includeStatistics) {
            return courseRepository.findWithModulesById(id)
                    .orElseThrow(() -> new CourseNotFoundException(id));
        } else if (includeStatistics) {
            return courseRepository.findByIdWithStatistics(id)
                    .orElseThrow(() -> new CourseNotFoundException(id));
        }
        return findCourseById(id);
    }

 */

/*
    // Servis za inicijalizaciju detaljnih statistika
    @Transactional
    public void recalculateStatistics(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        if (course.getModules().isEmpty()) {
            throw new CourseValidationException("Cannot calculate statistics for a course without modules");
        }
        course.getCourseStatistics().recalculate(course.getModules());
        courseRepository.save(course);
    }

 */

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
