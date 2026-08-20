package app.hamdy.spec.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateSpecRequest(
        @NotBlank(message = "Paste a spec first.") String body,
        String note) {
}
