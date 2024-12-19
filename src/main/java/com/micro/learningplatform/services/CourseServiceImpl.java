package com.micro.learningplatform.services;

import com.micro.learningplatform.models.*;
import com.micro.learningplatform.models.dto.DifficultyLevel;
import com.micro.learningplatform.models.dto.courses.*;
import com.micro.learningplatform.models.dto.module.CreateModuleRequest;
import com.micro.learningplatform.models.dto.module.ModuleDetailResponse;
import com.micro.learningplatform.repositories.*;
import com.micro.learningplatform.shared.CourseMapper;
import com.micro.learningplatform.shared.exceptions.*;
import com.micro.learningplatform.shared.validation.CourseValidator;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

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
    private final CustomCourseRepoImpl customCourseRepo;
    private final ModuleRepositroy moduleRepository;
    private final CourseValidator courseValidator;
    private final CourseStatisticsHistoryRepository historyRepository;

    // TODO dodati update i delete metode, te provjeriti jos jednom svaku metodu i validacije

    @Override
    @Transactional
    public CourseResponse createCourse(CreateCourseRequest createCourseRequest) {
        log.debug("Creating new course with title: {}", createCourseRequest.title());
        courseValidator.validateCourseTitle(createCourseRequest.title());
        Course course = Course.create(createCourseRequest.title(), createCourseRequest.description());
        //  course.assignAuthor(createCourseRequest.authorId());
        course.setDifficultyLevel(createCourseRequest.getDifficultyLevelEnum());
        course = courseRepository.save(course);

        // Event je već registriran kroz Course.create()

        log.info("Successfully created course with ID: {}", course.getId());
        return CourseMapper.toDTO(course);
    }

    @Override
    @Cacheable(cacheNames = "courses", key = "#courseId", unless = "#result == null", condition = "#courseId != null")
    public CourseResponse getCourse(UUID courseId) {
        log.debug("Fetching course by ID: {}", courseId);
        Course course = findCourseById(courseId);
        return CourseMapper.toDTO(course);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"courses", "coursesWithModules"}, key = "#courseId")
    public CourseResponse publishCourse(UUID courseId) {
        Course course = findCourseWithModuleById(courseId);

        log.debug("Modules in course: {}", course.getModules());

        // Poslovna logika unutar domenskog modela
        course.publish();

        log.info("Successfully published course with ID: {}", courseId);

        return CourseMapper.toDTO(course);
    }

    @Override
    @Cacheable(
            cacheNames = "courseSearches",
            key = "T(java.util.Objects).hash(#searchCriteria, #pageable)",
            unless = "#result.content.empty"
    )
    public Page<?> search(CourseSearchCriteria searchCriteria, Pageable pageable) throws RepositoryException {
        log.debug("Izvršavam pretragu s kriterijima: {}", searchCriteria);
        log.debug("Search term: '{}'", searchCriteria.getSearchTerm());

        if (CourseSearchCriteria.SearchType.FULL_TEXT.equals(searchCriteria.getSearchType())) {
            log.debug("Izvršavam full-text pretragu.");
            List<CourseSearchResult> results = customCourseRepo.fullTextSearch(searchCriteria.getSearchTerm());

            if (results.isEmpty()) {
                log.warn("Nema rezultata za pretragu: '{}'", searchCriteria.getSearchTerm());
                throw new NoResultException("No results found for the given search criteria.");
            }

            List<CourseSearchResult> dtos = results.stream()
                    .map(result -> new CourseSearchResult(
                            result.courseId(),
                            result.title(),
                            result.description(),
                            result.difficultyLevel(),
                            result.rank()
                    ))
                    .toList();

            return new PageImpl<>(dtos, pageable, dtos.size());
        }

        Page<CourseResponse> result = courseRepository
                .searchCoursesWithOther(searchCriteria.getSearchTerm(), pageable)
                .map(this::mapToCourseResponse);

        if (result.isEmpty()) {
            log.warn("Nema rezultata za standardnu pretragu: '{}'", searchCriteria.getSearchTerm());
            throw new NoResultException("No results found for the given search criteria.");
        }

        return result;
    }
/*
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
    public Page<CourseResponse> searchByTerm(String searchTerm, Pageable pageable) {
        return courseRepository.searchCourses(searchTerm, pageable)
                .map(CourseMapper::toDTO);
    }


 */

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
    @Cacheable(
            cacheNames = "coursesWithModules",
            key = "#courseId",
            unless = "#result == null"
    )
    public CourseResponseWithModules getCourseWithModules(UUID courseId) {
        log.debug("Fetching course with modules for ID: {}", courseId);
        Course course = courseRepository.findWithModulesById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        return CourseMapper.toCourseWithModulesResponse(course);
    }

    @Override
    @Transactional(
    rollbackFor = {CourseException.class, DataIntegrityViolationException.class},
    timeout = 30,
    propagation = Propagation.REQUIRED,
    isolation = Isolation.READ_COMMITTED
    )
    public void batchSaveCourses(List<CreateCourseRequest> requests) throws RepositoryException {
        log.debug("Batch saving {} courses", requests.size());
        courseValidator.validateBatchCourseTitles(requests);

        try {
            var courses = requests.stream()
                    .map(request -> {
                        Course course = Course.create(request.title(), request.description());
                        course.setDifficultyLevel(request.getDifficultyLevelEnum());
                        log.debug("Saving course with title: {}", course.getTitle());
                        return course;
                    })
                    .toList();

            customCourseRepo.batchSave(courses);
            log.info("Successfully batch saved {} courses", courses.size());

        } catch (DataIntegrityViolationException ex) {
            log.error("Duplicate title detected during batch save", ex);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new CourseAlreadyExistsException("One or more courses already exist with the provided titles.");
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
    @CacheEvict(cacheNames = {"courses", "coursesWithModules"}, key = "#courseId")
    public void addModuleToCourse(UUID courseId, CreateModuleRequest request) {
        log.debug("Adding module to course: {}", courseId);
        // Dohvati tečaj
        Course course = findCourseWithModuleById(courseId);
        log.debug("Course state after fetch: {}", course.getCourseStatus());
        log.debug("Modules in course after fetch: {}", course.getModules());

        // Kreiraj modul
        CourseModule module = CourseModule.create(request);
        log.debug("New module created: {}", module);

        checkForDuplicateTitle(course, module);
        setDifficultyLevelIfNotSpecified(course, module);
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
    @Transactional(
            rollbackFor = {CourseException.class, DataIntegrityViolationException.class},
            timeout = 60,  // Duži timeout za batch operacije
            propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED
    )
    public void batchAddCourseWithModules(CreateCourseWithModulesRequest request) {
        log.debug("Creating course with title: {} and {} modules",
                request.title(), request.modules().size());
        courseValidator.validateDuplicateModuleTitlesInRequest(request.modules());

        Course course = Course.create(request.title(), request.description());
        course.setDifficultyLevel(DifficultyLevel.valueOf(request.difficultyLevel()));

        List<CourseModule> modules = request.modules().stream()
                .map(moduleRequest -> {
                    CourseModule module = CourseModule.create(moduleRequest);
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

    // todo dodati controller za ovo
    public List<CourseStatisticHistory> getStatisticsHistory(
            UUID courseId,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        log.debug("Fetching statistics history for course ID: {}, startDate: {}, endDate: {}", courseId, startDate, endDate);

        List<CourseStatisticHistory> results = historyRepository.findByDateRange(courseId, startDate, endDate);

        if (results.isEmpty()) {
            log.warn("No statistics found for course ID: {} within date range {} - {}", courseId, startDate, endDate);
        } else {
            log.debug("Statistics fetched: {}", results);
        }

        return results;
        //  return statisticRepo.findByDateRange(courseId, startDate, endDate);

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


    // pomocne bussneuess metode

    private Course findCourseById(UUID courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
    }

    private Course findCourseWithModuleById(UUID courseId) {
        return courseRepository.findByIdWithModules(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
    }

    // pomocne bussniess metode
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
        Set<CourseModule> existingModules = course.getModules();
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

    private CourseResponse mapToCourseResponse(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCourseStatus(),
                course.getCategory(),
                course.getDifficultyLevel(),
                CourseMapper.toStatisticsDTO(
                        course.getStatisticsSnapshot(),
                        course.getCourseStatistics()
                ),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }

}

    // TODO smanjti broj slicni metoda npr dodati opcionalne parametere s bolje razrađenimk reopstirojem



