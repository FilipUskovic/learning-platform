package com.micro.learningplatform.models.dto;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Embeddable // mozemo ugraditi u bilo koji jpa entitet
public class ChangeTracker {
    /*
     * Ova klasa ce pratii sve uzmjene na entitetom
     */

    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @Column(name = "modification_reason")
    private String modificationReason;

    /* ElementCollection -> pohrana osnovnih tipova ili ugrađenih klasa u zasebnu tablicu
       * u nasem slucaju se changeHistory pohranjuje kao lista strignova
     */
    @ElementCollection
    @CollectionTable(name = "entity_changes")
    private List<String> changeHistory = new ArrayList<>();

    // omogucjemo pracenje svakog entit-a, dodaje novu stavku u u povjez izmjena
    public void trackChange(String reason, String modifiedBy) {
        this.lastModifiedBy = modifiedBy;
        this.modificationReason = reason;
        this.changeHistory.add(String.format("%s: %s by %s",
                LocalDateTime.now(), reason, modifiedBy));
    }
}
