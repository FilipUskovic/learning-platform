package com.micro.learningplatform.services;

import com.micro.learningplatform.models.Course;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PartitionMigrationService {

    private final EntityManager entityManager;
    private static final int BATCH_SIZE = 1000;


    //TODO dodati zatvaranje resourca da ne cure podaci
    public void migrateDataToPartitions() {
        // Dohvaćamo podatke u batch-evima da ne preopteretimo memoriju
        ScrollableResults scrollableResults = entityManager
                .createQuery("SELECT c FROM Course c ORDER BY c.createdAt", Course.class)
                .unwrap(Query.class) // Unwrap to Hibernate Query
                .setFetchSize(BATCH_SIZE) // Set fetch size
                .scroll(ScrollMode.FORWARD_ONLY); // Enable forward-only scrolling

        int count = 0;
        while (scrollableResults.next()) {
            Course course = (Course) scrollableResults.get();
            migrateToAppropriatePartition(course);

            if (++count % BATCH_SIZE == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }

    private void migrateToAppropriatePartition(Course course) {
        //TODO za migraciju pojedinačnog kursa
    }
}
