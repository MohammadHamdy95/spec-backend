package app.hamdy.spec.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.openapitools.openapidiff.core.OpenApiCompare;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.openapitools.openapidiff.core.model.ChangedOperation;
import org.openapitools.openapidiff.core.model.Endpoint;

import org.springframework.stereotype.Component;

/**
 * Compares two revisions and says what changed — and, more usefully, what
 * changed in a way that breaks existing callers.
 *
 * <p>This is a semantic comparison of two parsed API models, not a text diff.
 * Reordering paths or reflowing YAML produces no changes here, while removing
 * a required field produces one and flags it as breaking. That distinction is
 * the whole reason the feature is worth having: a text diff of a large spec
 * is noise.</p>
 */
@Component
public class SpecDiffService {

    public SpecDiffResult diff(int fromVersion, String fromBody, int toVersion, String toBody) {
        ChangedOpenApi changed = OpenApiCompare.fromContents(fromBody, toBody);

        List<SpecDiffResult.Change> changes = new ArrayList<>();

        for (Endpoint e : nullSafe(changed.getNewEndpoints())) {
            changes.add(new SpecDiffResult.Change(
                    "ADDED", "endpoint", label(e),
                    "New operation added.", false));
        }

        // A removed endpoint always breaks somebody: anyone still calling it.
        for (Endpoint e : nullSafe(changed.getMissingEndpoints())) {
            changes.add(new SpecDiffResult.Change(
                    "REMOVED", "endpoint", label(e),
                    "Operation no longer exists.", true));
        }

        for (ChangedOperation op : nullSafe(changed.getChangedOperations())) {
            String subject = subject(String.valueOf(op.getHttpMethod()), op.getPathUrl());
            boolean breaking = op.isIncompatible();
            changes.add(new SpecDiffResult.Change(
                    "MODIFIED", "operation", subject,
                    describe(op), breaking));
        }

        // Breaking first: the reason anyone opens a diff is to find these.
        changes.sort(Comparator.comparing(SpecDiffResult.Change::breaking).reversed());

        int breakingCount = (int) changes.stream()
                .filter(SpecDiffResult.Change::breaking).count();

        return new SpecDiffResult(
                fromVersion, toVersion, breakingCount == 0, breakingCount, changes);
    }

    /**
     * Turns the library's change model into something a human reads. It
     * exposes far more detail than is useful at a glance, so this summarises
     * which facets moved and leaves the specifics to the rendered docs.
     */
    private String describe(ChangedOperation op) {
        List<String> parts = new ArrayList<>();
        if (op.getParameters() != null && !op.getParameters().isUnchanged()) {
            parts.add("parameters");
        }
        if (op.getRequestBody() != null && !op.getRequestBody().isUnchanged()) {
            parts.add("request body");
        }
        if (op.getApiResponses() != null && !op.getApiResponses().isUnchanged()) {
            parts.add("responses");
        }
        if (op.getSecurityRequirements() != null && !op.getSecurityRequirements().isUnchanged()) {
            parts.add("security");
        }
        if (parts.isEmpty()) {
            return "Description or metadata changed.";
        }
        return "Changed: " + String.join(", ", parts) + ".";
    }

    private String label(Endpoint e) {
        return subject(String.valueOf(e.getMethod()), e.getPathUrl());
    }

    /**
     * "GET /orders/{id}" — method upper-cased, path left exactly as written.
     * URL paths are case-sensitive, so upper-casing the whole label would
     * misreport the very thing the reader is trying to identify.
     */
    private String subject(String method, String path) {
        return method.toUpperCase() + " " + path;
    }

    private <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
