package app.hamdy.spec.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param body          the spec, JSON or YAML — format is detected, not declared
 * @param expiresInDays null or 0 means never expire
 * @param note          optional label for this revision
 */
public record CreateSpecRequest(
        @NotBlank(message = "Paste a spec first.") String body,
        Integer expiresInDays,
        String note) {
}
