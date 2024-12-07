package com.micro.learningplatform.services;

import com.micro.learningplatform.models.dto.CourseSearchResult;
import com.micro.learningplatform.models.dto.CourseTrend;
import com.micro.learningplatform.models.dto.SearchRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.Transformers;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class AdvancedCourseSearchService {

    private final EntityManager entityManager;

    /**
     * napredno pretrazivanje courses-a korsiteci postgresql full text seatch
     * 1. znacajno brza od like jer korisi gin indeks
     * 2. implementira rangiranje rezultata
     * 3. podrzava slozene search upite
     *
     */



    public List<CourseSearchResult> searchCourses(SearchRequest request) {
        String sql = """
            SELECT c.*,
                ts_rank(c.search_vector, query) as rank
            FROM
                courses c,
                plainto_tsquery('english', :searchTerm) query
            WHERE
                c.search_vector @@ query
            ORDER BY rank DESC
            LIMIT :limit
            """;

        //todo promjeniti depricated setResultTransformer i Transformers.aliasToBean
        //todo umjesot toga korsiit tuple ili liesttransformer ako se ne varam
        Query query = entityManager.createNativeQuery(sql)
                .setParameter("searchTerm", request.searchTerm())
                .setParameter("limit", request.limit())
                .unwrap(NativeQuery.class)
                .addScalar("rank", StandardBasicTypes.DOUBLE)
                .setResultTransformer(Transformers.aliasToBean(CourseSearchResult.class));

        log.debug("Executing full text search with term: {}", request.searchTerm());
        return query.getResultList();
    }


    // Pratimo promjene u popularnosti kurseva
    // Identificiramo trendove u kategorijama i analiziramo sezonske varijacije

    public List<CourseTrend> analyzeCoursesTrend(){
        String sql = """
            WITH monthly_stats AS (
                SELECT
                    date_trunc('month', created_at) as month,
                    count(*) as course_count,
                    avg(count(*)) OVER
                        (ORDER BY date_trunc('month', created_at)
                         ROWS BETWEEN 3 PRECEDING AND CURRENT ROW)
                    as moving_average
                FROM courses
                GROUP BY date_trunc('month', created_at)
            )
            SELECT
                month,
                course_count,
                moving_average,
                course_count - lag(course_count) OVER (ORDER BY month)
                    as month_over_month_change
            FROM monthly_stats
            ORDER BY month DESC
            """;

        Query query = entityManager.createNativeQuery(sql)
                .unwrap(NativeQuery.class);
        // ovjde korsiti tuple umjesot depricated setResultTransformer

        List<Object[]> results = query.getResultList();
        return results.stream()
                .map(CourseTrend::fromResult)
                .toList();
    }


}
