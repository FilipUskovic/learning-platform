package com.micro.learningplatform.shared;

import com.micro.learningplatform.models.Course;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdvancedPostgresFeatureService {

    /**
     * - full text pretraga
     * - napredni indexi
     */

    // TODO DODATI TEstove za validaciju kreiranja o ovisnostima i dokumentirati ovisnosti o postgresql znacajkama

    private final EntityManager entityManager;


    //Implementiram full text pretragu PostgreSQL tsvector umjesot LIKE
    public List<Course> fullTextSearch (String searchTerm) {
        String sql = """
                select c.* from courses c
                where to_tsvector('english', c.title || ' ' || c.description || ' ') @@
                plainto_tsquery('english', :searchTerm)
                order by ts_rank(
                to_tsvector('english', c.title || ' ' || c.description),
                plainto_tsquery('english', :searchTerm)) desc
                """;

        return entityManager.createNativeQuery(sql, Course.class)
                .setParameter("searchTerm", searchTerm)
                .getResultList();
    }

    // Korisiti gist indexe za prostorno pretrazivanje
    // korisno ako implementiramo lokacisko-bazitanje znacajke

    //todo: dodati location u courses ili kreirati posebnu tablicu
    @Transactional
    public void createGistIndex() {
        String sql = " CREATE INDEX IF NOT EXISTS idx_course_location " +
                " ON courses USING GIST (location) ";

        entityManager.createNativeQuery(sql).executeUpdate();
    }

}
