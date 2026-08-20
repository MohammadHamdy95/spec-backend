package app.hamdy.spec.web.dto;

import java.time.Instant;

import app.hamdy.spec.domain.SpecDoc;
import app.hamdy.spec.domain.SpecVersion;

/**
 * What a viewer gets. {@code editToken} is populated only in the response to
 * a create — never on a read, or anyone with the link could edit.
 */
public record SpecResponse(
        String id,
        String url,
        String title,
        String apiVersion,
        String specVersion,
        int version,
        int latestVersion,
        String format,
        int sizeBytes,
        int operationCount,
        String note,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt,
        String body,
        String editToken) {

    public static SpecResponse of(SpecDoc doc, SpecVersion version, String url, String editToken) {
        return new SpecResponse(
                doc.getId(), url, doc.getTitle(), doc.getApiVersion(), doc.getSpecVersion(),
                version.getVersion(), doc.getLatestVersion(), version.getFormat(),
                version.getSizeBytes(), version.getOperationCount(), version.getNote(),
                doc.getCreatedAt(), doc.getUpdatedAt(), doc.getExpiresAt(),
                version.getBody(), editToken);
    }
}
