package com.micro.learningplatform.repositories;

import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.dto.courses.CourseSearchResult;
import com.micro.learningplatform.shared.exceptions.RepositoryException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CustomCourseRepoImpl implements CustomCourseRepo {

    private final EntityManager entityManager;
    private final MeterRegistry meterRegistry;

    @Override
    @Cacheable(cacheNames = "search", key = "#criteria.toString()")
    public Page<Course> searchCourses(CourseSearchCriteria criteria, Pageable pageable) throws RepositoryException {
        return executeWithMetrics("search", () -> {
            // Kreiramo CriteriaBuilder i CriteriaQuery za tipski sigurno pretraživanje
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Course> query = cb.createQuery(Course.class);
            Root<Course> course = query.from(Course.class);

            // Gradimo predikat koristeći kriterije pretrage
            List<Predicate> predicates = buildPredicates(criteria, cb, course);

            // Dodajemo where i order by klauzule
            query.where(predicates.toArray(new Predicate[0]))
                    .orderBy(cb.desc(course.get("createdAt")));

            // Izvršavamo query s paginacijom
            TypedQuery<Course> typedQuery = entityManager.createQuery(query)
                    .setFirstResult((int) pageable.getOffset())
                    .setMaxResults(pageable.getPageSize());

            // Brojimo ukupne rezultate za paginaciju
            long total = countResults(criteria);

            return new PageImpl<>(typedQuery.getResultList(), pageable, total);
        });
    }

    /**
     * Gradi predikate za pretraživanje bazirane na kriterijima
     */
    private List<Predicate> buildPredicates(
            CourseSearchCriteria criteria,
            CriteriaBuilder cb,
            Root<Course> course) {

        List<Predicate> predicates = new ArrayList<>();

        // Dodajemo predikate samo za ne-null vrijednosti
        if (criteria.getTitle() != null) {
            predicates.add(cb.like(
                    cb.lower(course.get("title")),
                    "%" + criteria.getTitle().toLowerCase() + "%"
            ));
        }

        if (criteria.getStatus() != null) {
            predicates.add(cb.equal(course.get("courseStatus"), criteria.getStatus()));
        }

        if (criteria.getCategory() != null) {
            predicates.add(cb.equal(course.get("category"), criteria.getCategory()));
        }

        return predicates;
    }

    /**
     * Broji ukupne rezultate koristeći count query
     */
    private long countResults(CourseSearchCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Course> course = countQuery.from(Course.class);

        // Postavljamo count query
        countQuery.select(cb.count(course));

        // Dodajemo iste predikate kao i u glavnom upitu
        List<Predicate> predicates = buildPredicates(criteria, cb, course);
        countQuery.where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(countQuery).getSingleResult();
    }

    /**
     * Batch spremanje kurseva s optimalnom veličinom batcha
     */
    @Override
    @Transactional
    public void batchSave(List<Course> courses) throws RepositoryException {
        executeWithMetrics("batchSave", () -> {
            int batchSize = 50;
            for (int i = 0; i < courses.size(); i++) {
                entityManager.persist(courses.get(i));

                if (i % batchSize == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
            return null;
        });
    }


     // * Full-text pretraživanje koristeći PostgreSQL tsvector
    // malo popravio metodu jer korisitm typedquery - koje greske u tipovima i kolonama vraca hvata tijekom kompalacije, te sigurniiji
    // bolje performance jer ne moramo pretvari iz objekta i mapirati, tj hibernate zna unaprijed strukutru
    // korsiti tuple api da imam tipiziranotsi i dinamicnost
    public List<CourseSearchResult> fullTextSearch(String searchTerm) throws RepositoryException {
        return executeWithMetrics("fullTextSearch", () -> {
            // SQL ostaje isti, ali dodajemo aliase koji se podudaraju s metodama u projekciji
            String sql = """
             SELECT c.id as id, c.title as title, c.difficulty_level as difficultyLevel, c.description as description,
              ts_rank(to_tsvector('english', c.title || ' ' || c.description),
              plainto_tsquery('english', :searchTerm)) as rank
              FROM courses c
               WHERE to_tsvector('english', c.title || ' ' || c.description) @@
               plainto_tsquery('english', :searchTerm)
               ORDER BY rank DESC
            """;

            // Koristimo TypedQuery s našom projekcijom
            TypedQuery<Tuple> query = (TypedQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class)
                    .setParameter("searchTerm", searchTerm);

            // Mapiranje Tuple rezultata na CourseSearchResult
            return query.getResultList().stream()
                    .map(tuple -> new CourseSearchResult(
                            tuple.get("id", UUID.class).toString(),
                            tuple.get("title", String.class),
                            tuple.get("description", String.class),
                            tuple.get("difficultyLevel", String.class),
                            tuple.get("rank", Float.class)
                    ))
                    .toList();
        });
    }


    /**
     * Generička metoda za izvršavanje operacija s metrikama i error handlingom
     */
    private <T> T executeWithMetrics(String operation, Supplier<T> action) throws RepositoryException {
        Timer.Sample timer = Timer.start(meterRegistry);

        try {
            T result = action.get();
            timer.stop(Timer.builder("repository.operation")
                    .tag("operation", operation)
                    .register(meterRegistry));
            return result;
        } catch (Exception e) {
            meterRegistry.counter("repository.error",
                    "operation", operation,
                    "exception", e.getClass().getSimpleName()
            ).increment();
            log.error("Repository error during {}: {}", operation, e.getMessage());
            throw new RepositoryException("Failed to execute " + operation, e);
        }
    }


}


