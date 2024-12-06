package com.micro.learningplatform.partitons;

import com.micro.learningplatform.models.dto.BatchProcessingResult;
import com.micro.learningplatform.services.BatchProcessor;
import com.micro.learningplatform.services.BatchProcessorService;
import com.micro.learningplatform.shared.exceptions.BatchProcessingException;
import com.micro.learningplatform.shared.exceptions.PartitionMaintenanceException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PartitionMigrationService {

    private final EntityManager entityManager;
    private final MeterRegistry meterRegistry;
    private final BatchProcessorService batchProcessor;

    private static final Duration RETENTION_PERIOD = Duration.ofDays(120);

    public void ensureRequiredTablesExist() {
        List<String> requiredTables = List.of("migration_candidates", "archived_table");
        for (String tableName : requiredTables) {
            try {
                // Provjera postoji li tablica
                Query query = entityManager.createNativeQuery(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_name = :tableName
                        """
                );
                query.setParameter("tableName", tableName);
                Long count = ((Number) query.getSingleResult()).longValue();

                // Ako tablica ne postoji, kreiraj ju
                if (count == 0) {
                    createTableIfNotExists(tableName);
                }
            } catch (Exception e) {
                log.error("Failed to ensure table exists: {}", tableName, e);
            }
        }
    }

    private void createTableIfNotExists(String tableName) {
        try {
            String createTableSql = switch (tableName) {
                case "migration_candidates" -> """
            CREATE TABLE migration_candidates (
                id UUID PRIMARY KEY,
                source_partition TEXT NOT NULL,
                target_partition TEXT NOT NULL
            )
            """;
                case "archived_table" -> """
            CREATE TABLE archived_table (
                id UUID PRIMARY KEY,
                title TEXT,
                description TEXT,
                created_at TIMESTAMP,
                updated_at TIMESTAMP,
                status TEXT
            )
            """;
                default -> throw new IllegalArgumentException("Unknown table: " + tableName);
            };

            entityManager.createNativeQuery(createTableSql).executeUpdate();
            log.info("Successfully created table: {}", tableName);

        } catch (Exception e) {
            log.error("Failed to create table: {}", tableName, e);
            throw e;
        }
    }

    @Scheduled(cron = "0 0 1 * * *") // Izvršava se prvog dana u mjesecu
    @Transactional
    public void maintainPartitions() throws PartitionMaintenanceException {
        initializeMissingPartitions();

        Timer.Sample timer = Timer.start(meterRegistry);

        try {
            log.info("Starting partition maintenance.");
            ensureBasePartitionTableExists();
            ensureRequiredTablesExist();
            createUpcomingPartitions();
            migrateData();
            archiveOldPartitions();
            recordMaintenanceMetrics(timer);
            log.info("Partition maintenance completed successfully.");

        } catch (Exception | BatchProcessingException e) {
          //  handleMaintenanceError(e);
            assert e instanceof Exception;
            throw new PartitionMaintenanceException("Partition maintenance failed", (Exception) e);
        }
    }


    /**
     * Kreira particije za nadolazeće razdoblje s optimalnom strukturom i indeksima.
     */
    public void createUpcomingPartitions() {
        if (!ensureBasePartitionTableExists()) {
            log.error("Base partition table 'courses_partitioned' does not exist. Cannot create upcoming partitions.");
            throw new IllegalStateException("Base partition table does not exist.");
        }

        LocalDate nextMonth = LocalDate.now().plusMonths(1);
        String partitionName = generatePartitionName(nextMonth);

        String createPartitionSql = """
        CREATE TABLE IF NOT EXISTS %s
        PARTITION OF courses_partitioned
        FOR VALUES FROM ('%s') TO ('%s')
        WITH (
            parallel_workers = 4,
            autovacuum_enabled = true,
            fillfactor = 90
        )
        """.formatted(
                partitionName,
                nextMonth.atStartOfDay().toString(),
                nextMonth.plusMonths(1).atStartOfDay().toString()
        );

        try {
            // Korištenje unwrap za direktnu manipulaciju konekcijom
            entityManager.unwrap(Session.class).doWork(connection -> {
                try (PreparedStatement ps = connection.prepareStatement(createPartitionSql)) {
                    ps.executeUpdate();
                    log.info("Successfully created partition: {}", partitionName);
                }
            });

            // Kreiranje indeksa za particiju
            createPartitionIndexes(partitionName);

            recordPartitionCreation(partitionName);
        } catch (Exception e) {
            log.error("Failed to create partition: {}", partitionName, e);
            recordPartitionError(partitionName);
            throw e;
        }
    }


    public void migrateData() throws BatchProcessingException {
        List<DataMigrationTask> migrationTasks = identifyDataForMigration();

        if (migrationTasks.isEmpty()) {
            log.warn("No migration tasks found. Skipping data migration.");
            return;
        }
        BatchProcessor<DataMigrationTask> processor = batch -> {
            for (DataMigrationTask task : batch) {
                try {
                    if (!doesPartitionExist(task.sourcePartition())) {
                        log.warn("Source partition {} does not exist. Skipping task for recordId {}.", task.sourcePartition(), task.recordId());
                        continue;
                    }

                    if (!doesPartitionExist(task.targetPartition())) {
                        log.warn("Target partition {} does not exist. Skipping task for recordId {}.", task.targetPartition(), task.recordId());
                        continue;
                    }

                    String migrationSql = String.format(
                            """
                            WITH moved_rows AS (
                                DELETE FROM %s
                                WHERE id = '%s'
                                RETURNING *
                            )
                            INSERT INTO %s
                            SELECT * FROM moved_rows
                            """,
                            task.sourcePartition(),
                            task.recordId(),
                            task.targetPartition()
                    );

                    entityManager.createNativeQuery(migrationSql).executeUpdate();
                    log.info("Successfully migrated record {} from {} to {}", task.recordId(), task.sourcePartition(), task.targetPartition());
                } catch (Exception e) {
                    log.error("Error migrating record {} from {} to {}: {}", task.recordId(), task.sourcePartition(), task.targetPartition(), e.getMessage());
                }
            }
        };

        BatchProcessingResult result = batchProcessor.processBatch(
                migrationTasks,
                processor,
                BatchProcessorService.BatchProcessingOptions.getDefault()
        );

        log.info("Data migration completed: {}", result);
    }

    private void initializeMissingPartitions() {
        List<String> requiredPartitions = List.of("courses_partitioned_2023_12", "courses_partitioned_2024_01");
        for (String partitionName : requiredPartitions) {
            if (!doesPartitionExist(partitionName)) {
                log.info("Partition {} does not exist. Creating it.", partitionName);
                createPartition(partitionName); // Implementirajte ovu metodu za stvaranje particija.
            }
        }
    }

    private void createPartition(String partitionName) {
        String createPartitionSql = String.format(
                """
                CREATE TABLE IF NOT EXISTS %s PARTITION OF courses_partitioned
                FOR VALUES FROM ('%s') TO ('%s')
                """,
                partitionName
        );

        entityManager.createNativeQuery(createPartitionSql).executeUpdate();
        log.info("Partition {} created successfully.", partitionName);
    }

    public void archiveOldPartitions() {
        LocalDate archiveThreshold = LocalDate.now().minus(RETENTION_PERIOD);
        List<String> oldPartitions = findPartitionsOlderThan(archiveThreshold);

        for (String partition : oldPartitions) {
            if (isPartitionInUse(partition)) {
                log.warn("Partition {} is in use, skipping archival", partition);
                continue;
            }
            archivePartition(partition);
        }
    }

    private void createPartitionIndexes(String partitionName) {
        List<String> indexDefinitions = List.of(
                """
                CREATE INDEX idx_%s_search ON %s
                USING gin(to_tsvector('english', title || ' ' || description))
                """,
                """
                CREATE INDEX idx_%s_temporal ON %s (created_at, updated_at)
                """,
                """
                CREATE INDEX idx_%s_status ON %s (status, created_at DESC)
                """
        );

        for (String indexSql : indexDefinitions) {
            String formatted = indexSql.formatted(
                    partitionName.replace(".", "_"),
                    partitionName
            );
            String indexName = extractIndexName(formatted);
            if (doesIndexExist(indexName)) {
                log.warn("Index {} already exists. Skipping creation.", indexName);
                continue;
            }

            try {
                Query query = entityManager.createNativeQuery(formatted);
                query.executeUpdate();
                log.info("Created index on partition: {}", partitionName);
            } catch (Exception e) {
                log.error("Failed to create index on partition: {}", partitionName, e);
            }
        }
    }


    private boolean doesIndexExist(String indexName) {
        Query query = entityManager.createNativeQuery(
                """
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE indexname = :indexName
                """
        );
        query.setParameter("indexName", indexName);
        Long count = ((Number) query.getSingleResult()).longValue();
        return count > 0;
    }

    private boolean doesPartitionExist(String partitionName) {
        Query query = entityManager.createNativeQuery(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_name = :partitionName
                """
        );
        query.setParameter("partitionName", partitionName);
        Long count = ((Number) query.getSingleResult()).longValue();
        return count > 0;
    }

    private String extractIndexName(String sql) {
        String[] parts = sql.split(" ");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equalsIgnoreCase("INDEX")) {
                return parts[i + 1];
            }
        }
        throw new IllegalArgumentException("Invalid SQL for extracting index name: " + sql);
    }



    private boolean isPartitionInUse(String partition) {
        Query query = entityManager.createNativeQuery(
                """
                SELECT COUNT(*)
                FROM pg_stat_activity
                WHERE query LIKE CONCAT('%', :partition, '%')
                """
        );
        query.setParameter("partition", "%" + partition + "%");
        long count = ((Number) query.getSingleResult()).longValue();
        return count > 0;
    }

    private List<String> findPartitionsOlderThan(LocalDate archiveThreshold) {
        String threshold = "courses_partitioned_" + archiveThreshold.toString().replace("-", "_");
        Query query = entityManager.createNativeQuery(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_name LIKE 'courses_partitioned_%'
                AND table_name < :threshold
                """
        );
        query.setParameter("threshold", threshold);
        return query.getResultList();
    }

    private List<DataMigrationTask> identifyDataForMigration() {
        Query query = entityManager.createNativeQuery(
                """
                SELECT id, source_partition, target_partition
                FROM migration_candidates
                """
        );

        List<Object[]> results = query.getResultList();
        return results.stream()
                .map(row -> new DataMigrationTask(
                        (UUID) row[0], // Direkto kastanje u UUID
                        (String) row[1],
                        (String) row[2]
                ))
                .toList();
    }

    private void archivePartition(String partition) {
        try {
            String archiveSql = String.format(
                    """
                    WITH archived_rows AS (
                        DELETE FROM %s
                        RETURNING *
                    )
                    INSERT INTO archived_table
                    SELECT * FROM archived_rows
                    """,
                    partition
            );

            entityManager.createNativeQuery(archiveSql).executeUpdate();

            log.info("Successfully archived partition: {}", partition);
            meterRegistry.counter("partition.archived", "partition", partition).increment();
        } catch (Exception e) {
            log.error("Failed to archive partition: {}", partition, e);
            meterRegistry.counter("partition.error", "operation", "archive", "partition", partition).increment();
            throw e;
        }
    }




    private boolean ensureBasePartitionTableExists() {
        try {
            Query query = entityManager.createNativeQuery(
                    """
                    SELECT COUNT(*)
                    FROM information_schema.tables
                    WHERE table_name = 'courses_partitioned'
                    """
            );
            Long count = ((Number) query.getSingleResult()).longValue();

            if (count == 0) {
                entityManager.createNativeQuery(
                        """
                        CREATE TABLE courses_partitioned (
                            id UUID,
                            title TEXT,
                            description TEXT,
                            created_at TIMESTAMP,
                            updated_at TIMESTAMP,
                            status TEXT,
                            PRIMARY KEY (id, created_at)
                        ) PARTITION BY RANGE (created_at)
                        """
                ).executeUpdate();
                log.info("Base partition table 'courses_partitioned' created successfully.");
                return true;
            } else {
                log.info("Base partition table 'courses_partitioned' already exists.");
                return true;
            }
        } catch (Exception e) {
            log.error("Error creating base partition table 'courses_partitioned'.", e);
            return false;

        }

    }



    private String generatePartitionName(LocalDate date) {
        return "courses_partitioned_" + date.toString().replace("-", "_");
    }

    private void recordMaintenanceMetrics(Timer.Sample timer) {
        timer.stop(Timer.builder("partition.maintenance")
                .register(meterRegistry));
    }

    private void recordPartitionCreation(String partitionName) {
        meterRegistry.counter("partition.created",
                "partition", partitionName).increment();
    }

    private void recordPartitionError(String partitionName) {
        meterRegistry.counter("partition.error",
                "operation", "creation",
                "partition", partitionName).increment();
    }
}
