package app.hamdy.spec.service;

import java.util.List;

/**
 * What we learn from a submitted document: whether it is usable, and the
 * metadata worth showing without re-parsing (title, counts, dialect).
 *
 * @param valid         whether the document parsed into a usable API model
 * @param errors        human-readable problems, empty when valid
 * @param title         info.title, or a placeholder when absent
 * @param apiVersion    info.version — the API's own version, not our revision
 * @param specVersion   dialect: swagger_2_0 / openapi_3_0 / openapi_3_1
 * @param format        json or yaml, as submitted
 * @param operationCount number of operations across all paths
 */
public record ParsedSpec(
        boolean valid,
        List<String> errors,
        String title,
        String apiVersion,
        String specVersion,
        String format,
        int operationCount) {
}
