package com.micro.learningplatform.repositories;

import com.micro.learningplatform.models.Course;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class CriteraCourseRepositoryImpl implements CriteriaCourseRepository {

    @PersistenceContext
    private EntityManager entityManager;


    //TODO razmotiriti o data specificationu

    /**
     * Koristi criteria api za dinamicko krierianje SQL upita za fleksibilno filtriranje.
     * Izbjegavamo generiranje velikog broja sličnih metoda.
     */

    @Override
    public List<Course> findByDynamicCriteria(CourseCriteria criteria) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Course> criteriaQuery = builder.createQuery(Course.class);
        Root<Course> root = criteriaQuery.from(Course.class);

        List<Predicate> predicates = new ArrayList<>();

        if (criteria.hasStatus()) {
            predicates.add(builder.equal(root.get("status"), criteria.getStatus()));
        }

        if (criteria.hasCategory()) {
            predicates.add(builder.equal(root.get("category"), criteria.getCategory()));
        }

        criteriaQuery.where(predicates.toArray(new Predicate[0]));
        return entityManager.createQuery(criteriaQuery).getResultList();

    }
}
