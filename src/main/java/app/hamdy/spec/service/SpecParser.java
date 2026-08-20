package app.hamdy.spec.service;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import org.springframework.stereotype.Component;

/**
 * Parses, validates and describes a submitted document.
 *
 * <p>Validation is the point of this class. Anyone can render a spec; telling
 * someone <em>why</em> theirs is wrong, before they share a broken link, is
 * the thing worth building.</p>
 */
@Component
public class SpecParser {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ObjectMapper YAML = new ObjectMapper(
            new YAMLFactory()
                    // Keeps output readable: no "---" banner, no surprise line
                    // wrapping through the middle of long descriptions or URLs.
                    .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                    .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));

    /**
     * Remote $ref resolution is deliberately OFF.
     *
     * <p>Turning it on would make the server fetch arbitrary URLs chosen by
     * whoever pasted the document — a server-side request forgery primitive
     * aimed at everything this host can reach, which includes Pi-hole,
     * BTCPay and the LAN. A spec that needs remote refs must be bundled
     * before upload.</p>
     */
    private ParseOptions parseOptions() {
        ParseOptions options = new ParseOptions();
        options.setResolve(false);
        options.setResolveFully(false);
        return options;
    }

    public ParsedSpec parse(String body) {
        String format = looksLikeJson(body) ? "json" : "yaml";

        SwaggerParseResult result;
        try {
            result = new OpenAPIV3Parser().readContents(body, null, parseOptions());
        } catch (Exception e) {
            return invalid(format, List.of("Could not parse the document: " + e.getMessage()));
        }

        OpenAPI api = result == null ? null : result.getOpenAPI();
        List<String> errors = new ArrayList<>();
        if (result != null && result.getMessages() != null) {
            errors.addAll(result.getMessages());
        }

        if (api == null) {
            if (errors.isEmpty()) {
                errors.add("This does not look like a Swagger or OpenAPI document.");
            }
            return invalid(format, errors);
        }

        // The parser is lenient: it returns a model for documents that are
        // structurally recognisable but still wrong. Treat any message as a
        // failure so a broken spec never gets a share link.
        boolean valid = errors.isEmpty();

        String title = api.getInfo() != null && api.getInfo().getTitle() != null
                ? api.getInfo().getTitle()
                : "Untitled API";
        String apiVersion = api.getInfo() != null && api.getInfo().getVersion() != null
                ? api.getInfo().getVersion()
                : "";

        return new ParsedSpec(valid, errors, title, apiVersion,
                dialect(api, body), format, countOperations(api));
    }

    /** Convert between the two representations without changing the content. */
    public String toJson(String body) throws Exception {
        return JSON.writerWithDefaultPrettyPrinter()
                .writeValueAsString(readAny(body));
    }

    public String toYaml(String body) throws Exception {
        return YAML.writeValueAsString(readAny(body));
    }

    private JsonNode readAny(String body) throws Exception {
        return looksLikeJson(body) ? JSON.readTree(body) : YAML.readTree(body);
    }

    /**
     * JSON and YAML are told apart by the first non-whitespace character:
     * every JSON document starts with '{'. YAML is the fallback because YAML
     * is a superset of JSON, so guessing wrong in that direction still parses.
     */
    private boolean looksLikeJson(String body) {
        String trimmed = body.stripLeading();
        return !trimmed.isEmpty() && trimmed.charAt(0) == '{';
    }

    /**
     * The parser upconverts Swagger 2.0 into an OpenAPI 3 model, so the model
     * alone cannot tell us what was submitted. The raw text can.
     */
    private String dialect(OpenAPI api, String body) {
        if (body.contains("\"swagger\"") || body.matches("(?s).*(^|\\n)\\s*swagger\\s*:.*")) {
            return "swagger_2_0";
        }
        String declared = api.getOpenapi();
        if (declared != null && declared.startsWith("3.1")) {
            return "openapi_3_1";
        }
        return "openapi_3_0";
    }

    private int countOperations(OpenAPI api) {
        if (api.getPaths() == null) {
            return 0;
        }
        return api.getPaths().values().stream()
                .mapToInt(item -> item.readOperations() == null ? 0 : item.readOperations().size())
                .sum();
    }

    private ParsedSpec invalid(String format, List<String> errors) {
        return new ParsedSpec(false, errors, "Untitled API", "", "unknown", format, 0);
    }
}
