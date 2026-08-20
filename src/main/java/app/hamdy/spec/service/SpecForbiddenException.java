package app.hamdy.spec.service;

/** Wrong or missing edit token on an update. */
public class SpecForbiddenException extends RuntimeException {
    public SpecForbiddenException(String message) {
        super(message);
    }
}
