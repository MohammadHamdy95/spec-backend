package app.hamdy.spec.service;

import java.util.List;

/**
 * A semantic comparison of two revisions.
 *
 * @param fromVersion    revision compared against
 * @param toVersion      revision compared
 * @param compatible     true when nothing here can break an existing client
 * @param breakingCount  number of changes classified as breaking
 * @param changes        every change, breaking ones first
 */
public record SpecDiffResult(
        int fromVersion,
        int toVersion,
        boolean compatible,
        int breakingCount,
        List<Change> changes) {

    /**
     * @param kind     ADDED / REMOVED / MODIFIED
     * @param area     what changed: endpoint, parameter, response, schema...
     * @param subject  which one, e.g. "GET /orders/{id}"
     * @param detail   what specifically changed
     * @param breaking whether this can break an existing caller
     */
    public record Change(
            String kind,
            String area,
            String subject,
            String detail,
            boolean breaking) {
    }
}
