package com.micro.learningplatform.services;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j

public class CoursePartitionManager {

    /* - automatsko particioniraje po vremenu
        - jednostavno arhiviranje starih pidataka
        - lakse odrzavanje i backup-a starih podatka

     */

    private final EntityManager entityManager;

    public void createPartitionForNextMonth() {
        // Izračunamo datum za sljedeći mjesec
        LocalDate nextMonth = LocalDate.now().plusMonths(1);
        LocalDate followingMonth = nextMonth.plusMonths(1);

        String partitionName = String.format("courses_y%04dm%02d",
                nextMonth.getYear(),
                nextMonth.getMonthValue());

        // SQL za kreiranje nove particije
        String createPartitionSQL = """
            CREATE TABLE IF NOT EXISTS %s
            PARTITION OF courses_partitioned
            FOR VALUES FROM ('%s') TO ('%s')
            """;

        // Izvršavamo kreiranje particije
        entityManager.createNativeQuery(String.format(
                createPartitionSQL,
                partitionName,
                nextMonth.atStartOfDay(),
                followingMonth.atStartOfDay()
        )).executeUpdate();

        // Kreiramo indekse na novoj particiji
        createIndexesForPartition(partitionName);

        log.info("Created new partition {} for period {} to {}",
                partitionName, nextMonth, followingMonth);

    }

    private void createIndexesForPartition(String partitionName) {
        String createIndexSQL = """
            CREATE INDEX idx_%s_created
            ON %s (created_at)
            """;

        entityManager.createNativeQuery(String.format(
                createIndexSQL,
                partitionName,
                partitionName
        )).executeUpdate();
    }

    // Metoda za automatsko održavanje particija
    @Scheduled(cron = "0 0 1 * * *") // Izvršava se prvi dan u mjesecu
    public void maintainPartitions() {
        // Kreiramo particiju za sljedeći mjesec
        createPartitionForNextMonth();

        // Arhiviramo stare particije
        archiveOldPartitions();
    }

    private void archiveOldPartitions() {
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);

        //TODO: provjeriti radi li kako treba
        // Pronalazimo stare particije
        String findOldPartitionsSQL = """
            SELECT courses_partitioned FROM pg_tables
            WHERE courses_partitioned LIKE 'courses_y%'
            AND courses_partitioned < 'courses_y%04dm%02d'
            """;

        List<String> oldPartitions = entityManager
                .createNativeQuery(String.format(
                        findOldPartitionsSQL,
                        sixMonthsAgo.getYear(),
                        sixMonthsAgo.getMonthValue()
                ))
                .getResultList();

        // Arhiviramo svaku staru particiju
        for (String partition : oldPartitions) {
            archivePartition(partition);
        }
    }

    private void archivePartition(String partition) {
        //TODO: dodati logiku za arhiviranje podataka

        log.info("Archiving partition: {}", partition);
    }

}
