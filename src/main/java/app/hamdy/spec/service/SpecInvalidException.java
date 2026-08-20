package app.hamdy.spec.service;

import java.util.List;

/**
 * Thrown when a document will not parse into a usable API model. Carries the
 * parser's messages so the caller can show what is wrong and where, rather
 * than a bare "invalid".
 */
public class SpecInvalidException extends RuntimeException {

    private final List<String> errors;

    public SpecInvalidException(List<String> errors) {
        super("The document is not a valid OpenAPI or Swagger spec.");
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
