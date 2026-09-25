package com.marlus.financas.dataio.domain;

import com.marlus.financas.common.UserOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "import_job")
public class ImportJob extends UserOwnedEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 255)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ImportMode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportJobStatus status;

    @Column(name = "summary_json", nullable = false, columnDefinition = "TEXT")
    private String summaryJson;

    protected ImportJob() {
    }

    public ImportJob(UUID id, UUID userId, String filename, ImportMode mode, ImportJobStatus status, String summaryJson) {
        super(userId);
        this.id = id;
        this.filename = filename;
        this.mode = mode;
        this.status = status;
        this.summaryJson = summaryJson;
    }

    public UUID getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public ImportMode getMode() {
        return mode;
    }

    public ImportJobStatus getStatus() {
        return status;
    }

    public String getSummaryJson() {
        return summaryJson;
    }
}
