package com.micro.learningplatform.controllers;

import com.micro.learningplatform.partitons.PartitionMigrationService;
import com.micro.learningplatform.shared.exceptions.BatchProcessingException;
import com.micro.learningplatform.shared.exceptions.PartitionMaintenanceException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/partitions")
@RequiredArgsConstructor
public class PartitionMigrationController {

    private final PartitionMigrationService partitionMigrationService;

    @PostMapping("/maintain")
    public ResponseEntity<String> maintainPartitions() {
        try {
            partitionMigrationService.maintainPartitions();
            return ResponseEntity.ok("Partition maintenance completed successfully.");
        } catch (PartitionMaintenanceException e) {
            return ResponseEntity.status(500).body("Error during partition maintenance: " + e.getMessage());
        }
    }

    /**
     * Endpoint za kreiranje particija za sljedeći mjesec.
     */
    @PostMapping("/create-upcoming")
    public ResponseEntity<String> createUpcomingPartitions() {
        try {
            partitionMigrationService.createUpcomingPartitions();
            return ResponseEntity.ok("Upcoming partitions created successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating upcoming partitions: " + e.getMessage());
        }
    }

    /**
     * Endpoint za migraciju podataka.
     */
    @PostMapping("/migrate")
    public ResponseEntity<String> migrateData() {
        try {
            partitionMigrationService.migrateData();
            return ResponseEntity.ok("Data migration completed successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error during data migration: " + e.getMessage());
        } catch (BatchProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Endpoint za arhiviranje starih particija.
     */
    @PostMapping("/archive")
    public ResponseEntity<String> archiveOldPartitions() {
        try {
            partitionMigrationService.archiveOldPartitions();
            return ResponseEntity.ok("Old partitions archived successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error during partition archival: " + e.getMessage());
        }
    }

    /**
     * Endpoint za provjeru i kreiranje potrebnih tablica.
     */
    @PostMapping("/ensure-tables")
    public ResponseEntity<String> ensureRequiredTablesExist() {
        try {
            partitionMigrationService.ensureRequiredTablesExist();
            return ResponseEntity.ok("Required tables ensured successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error ensuring required tables: " + e.getMessage());
        }
    }

}
