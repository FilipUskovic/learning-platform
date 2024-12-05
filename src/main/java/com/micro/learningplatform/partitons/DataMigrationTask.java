package com.micro.learningplatform.partitons;

import java.util.UUID;

public record DataMigrationTask(
        UUID recordId,
        String sourcePartition,
        String targetPartition
) {
}
