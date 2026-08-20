package app.hamdy.spec.web;

import java.net.URI;

import app.hamdy.spec.service.SpecForbiddenException;
import app.hamdy.spec.service.SpecInvalidException;
import app.hamdy.spec.service.SpecNotFoundException;
import app.hamdy.spec.service.SpecTooLargeException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** RFC 7807 problem responses, matching paste-backend's shape. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(SpecNotFoundException.class)
    public ProblemDetail notFound(SpecNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Not found", e.getMessage(), "not-found");
    }

    @ExceptionHandler(SpecForbiddenException.class)
    public ProblemDetail forbidden(SpecForbiddenException e) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", e.getMessage(), "forbidden");
    }

    @ExceptionHandler(SpecTooLargeException.class)
    public ProblemDetail tooLarge(SpecTooLargeException e) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "Spec too large", e.getMessage(), "too-large");
    }

    /**
     * Validation failures carry the parser's messages, so the UI can point at
     * what is actually wrong instead of saying "invalid spec".
     */
    @ExceptionHandler(SpecInvalidException.class)
    public ProblemDetail invalid(SpecInvalidException e) {
        ProblemDetail detail = problem(
                HttpStatus.UNPROCESSABLE_ENTITY, "Invalid spec", e.getMessage(), "invalid-spec");
        detail.setProperty("errors", e.getErrors());
        return detail;
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String slug) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://spec.hamdy.app/errors/" + slug));
        return problem;
    }
}
