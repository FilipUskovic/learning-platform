package com.micro.learningplatform.services;

import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.dto.CreateModuleRequest;
import com.micro.learningplatform.models.dto.ModuleData;
import com.micro.learningplatform.models.dto.ModuleResponse;
import com.micro.learningplatform.repositories.CourseRepository;
import com.micro.learningplatform.shared.exceptions.CourseNotFoundException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.hibernate.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CourseModuleService {

    /* Koristimo batch processing za efikasno kreiranje većeg broja modula
        Izbjegavamo učitavanje cijelog course objekta u memoriju kada radimo s modulima
        Koristimo native queries za optimalne performanse kod čitanja
        Implementiramo pravilno upravljanje persistence kontekstom da izbjegnemo memory leaks
     */

    private final CourseRepository courseRepository;
    private final EntityManager entityManager;
    private static final int BATCH_SIZE = 50;

    // Metoda za efikasno procesiranje velikog broja modula
    @Transactional
    public void processModulesInBatches(UUID courseId, List<ModuleData> modules){
        // Dohvaćamo kurs koristeći EntityManager za bolju kontrolu nad sesijom
        Course course = entityManager.find(Course.class, courseId);
        if (course == null) {
            throw new CourseNotFoundException(courseId);
        }

        // Dijelimo module u manje skupine za efikasnije procesiranje
        List<List<ModuleData>> batches = ListUtils.partition(modules, BATCH_SIZE);
        for(List<ModuleData> batch : batches){
            processBatch(course, batch);
            // Čistimo persistence context nakon svakog batcha da spriječimo memory leak
            entityManager.flush();
            entityManager.clear();
        }

    }

    private void processBatch(Course course, List<ModuleData> moduleBatch)throws SQLException {
        // Koristimo SQL batch insert za bolje performanse
        String insertSql = """
            INSERT INTO course_modules
            (id, course_id, title, sequence_number, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = entityManager.unwrap(Session.class)
                .doReturningWork(connection ->
                        connection.prepareStatement(insertSql))) {

            for (ModuleData data : moduleBatch) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, course.getId());
                ps.setString(3, data.title());
                ps.setInt(4, data.sequenceNumber());
                ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    // Metoda za efikasno dohvaćanje modula s paginacijom
    public Page<ModuleResponse> getModulesForCourse(UUID courseId, Pageable pageable) {
        // Koristim native query za optimalne performans
        String countQuery = """
            SELECT COUNT(*)
            FROM course_modules
            WHERE course_id = :courseId
            """;

        String selectQuery = """
            SELECT m.*
            FROM course_modules m
            WHERE m.course_id = :courseId
            ORDER BY m.sequence_number
            LIMIT :limit OFFSET :offset
            """;

        // Prvo dohvaćamo ukupan broj za paginaciju
        Long total = (Long) entityManager.createNativeQuery(countQuery)
                .setParameter("courseId", courseId)
                .getSingleResult();

        // Zatim dohvaćamo trenutnu stranicu
        List<Object[]> results = entityManager.createNativeQuery(selectQuery)
                .setParameter("courseId", courseId)
                .setParameter("limit", pageable.getPageSize())
                .setParameter("offset", pageable.getOffset())
                .getResultList();


        List<ModuleResponse> modules = results.stream()
                .map(this::mapToModuleResponse)
                .toList();

        return new PageImpl<>(modules, pageable, total);

    }

    private ModuleResponse mapToModuleResponse(Object[] tuple) {
        return new ModuleResponse(
                (String) tuple[0],          // id
                (String) tuple[1],        // title
                (String) tuple[2],        // description
                (Integer) tuple[3],       // sequence_number
                (Integer) tuple[4]        // duration
        );
    }
 }


