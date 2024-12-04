package com.micro.learningplatform.repositories;

import com.micro.learningplatform.models.dto.CourseBatchDTO;
import com.micro.learningplatform.shared.SearchCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.jpa.SpecHints;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class PerformanceOptimizedCourseRepository {
    @PersistenceContext
    private EntityManager entityManager;

    /*
      Dinamicki generira sql upite pomocu uvjeta kojih sam definirao u searchcriteriju
      optimiziran za performance kroz grpuby i orderby metode
      -> flexibilnost kroz dodadvanje dinamickih querija
      -> povecava perofrmance korsiteci queryHints
     */

    // Metoda koja demonstrira optimizirani pristup pretraživanju
    public List<CourseBatchDTO> findCoursesByOptimizedCriteria(SearchCriteria criteria) {
        // Gradimo SQL upit koji će maksimalno iskoristiti indekse
        StringBuilder sql = new StringBuilder("""
            SELECT
                c.id,
                c.title,
                c.status,
                COUNT(m.id) as module_count
            FROM courses c
            LEFT JOIN course_modules m ON c.id = m.course_id
            WHERE 1=1
            """);

        Map<String, Object> params = new HashMap<>();

        // Dinamički dodajemo uvjete pretrage, pazeći na performanse
        if (criteria.hasStatus()) {
            sql.append(" AND c.status = :status");
            params.put("status", criteria.getStatus().name());
        }

        if (criteria.hasSearchTerm()) {
            // Koristimo postojeći indeks za pretragu po naslovu
            sql.append(" AND c.title ILIKE :searchTerm");
            params.put("searchTerm", "%" + criteria.getSearchTerm() + "%");
        }

        // Grupiramo rezultate za module_count
        sql.append(" GROUP BY c.id, c.title, c.status");

        // Dodajemo sortiranje koje može koristiti indekse
        sql.append(" ORDER BY c.created_at DESC");

        // Kreiramo i konfiguriramo upit
        Query query = entityManager.createNativeQuery(sql.toString());

        // Postavljamo parametre
        params.forEach(query::setParameter);

        // Postavljamo hint-ove za optimizaciju
        query.setHint(AvailableSettings.STATEMENT_FETCH_SIZE, 50);
        query.setHint(HibernateHints.HINT_CACHEABLE, true);

        // Dodajemo query timeout za sigurnost
        query.setHint(SpecHints.HINT_SPEC_QUERY_TIMEOUT, 5000); // 5 sekundi

        // Pratimo vrijeme izvršavanja upita
        long startTime = System.nanoTime();

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        long endTime = System.nanoTime();

        // Logiramo performanse upita za monitoring
        log.debug("Query executed in {}ms with {} results",
                (endTime - startTime) / 1_000_000,
                results.size());

        // Mapiramo rezultate u DTO objekte
        return results.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private CourseBatchDTO mapToDTO(Object[] result) {
        return new CourseBatchDTO(
                (UUID) result[0],       // id
                (String) result[1],     // title
                (String) result[2],     // status
                ((Number) result[3]).intValue()  // module_count
        );
    }
}
