package com.micro.learningplatform.shared.exceptions;

public class PartitionMaintenanceException extends Throwable {
    public PartitionMaintenanceException(String partitionMaintenanceFailed, Exception e) {
        super(partitionMaintenanceFailed, e);
    }
}
