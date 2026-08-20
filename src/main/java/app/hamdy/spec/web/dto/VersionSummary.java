package app.hamdy.spec.web.dto;

import java.time.Instant;

import app.hamdy.spec.domain.SpecVersion;

/** A row in the history list — no body, so listing stays cheap. */
public record VersionSummary(
        int version,
        int sizeBytes,
        int operationCount,
        String format,
        String note,
        Instant createdAt) {

    public static VersionSummary of(SpecVersion v) {
        return new VersionSummary(v.getVersion(), v.getSizeBytes(),
                v.getOperationCount(), v.getFormat(), v.getNote(), v.getCreatedAt());
    }
}
