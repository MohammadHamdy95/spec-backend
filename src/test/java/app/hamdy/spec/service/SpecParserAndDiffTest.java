package app.hamdy.spec.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Plain unit tests — no Spring context, no Cassandra. These cover the two
 * pieces that carry the product: whether a document is judged valid, and
 * whether a change is judged breaking.
 */
class SpecParserAndDiffTest {

    private final SpecParser parser = new SpecParser();
    private final SpecDiffService diff = new SpecDiffService();

    private static final String V1 = """
            openapi: 3.0.3
            info:
              title: Orders API
              version: "1.0.0"
            paths:
              /orders:
                get:
                  responses:
                    "200":
                      description: ok
              /orders/{id}:
                get:
                  parameters:
                    - name: id
                      in: path
                      required: true
                      schema:
                        type: string
                  responses:
                    "200":
                      description: ok
            """;

    /** v2 removes /orders/{id} and adds /customers. */
    private static final String V2 = """
            openapi: 3.0.3
            info:
              title: Orders API
              version: "2.0.0"
            paths:
              /orders:
                get:
                  responses:
                    "200":
                      description: ok
              /customers:
                get:
                  responses:
                    "200":
                      description: ok
            """;

    @Test
    void parsesValidYamlSpecAndExtractsMetadata() {
        ParsedSpec parsed = parser.parse(V1);

        assertTrue(parsed.valid(), "expected valid, errors: " + parsed.errors());
        assertEquals("Orders API", parsed.title());
        assertEquals("1.0.0", parsed.apiVersion());
        assertEquals("openapi_3_0", parsed.specVersion());
        assertEquals("yaml", parsed.format());
        assertEquals(2, parsed.operationCount());
    }

    @Test
    void detectsJsonFormat() {
        String json = """
                {"openapi":"3.0.3","info":{"title":"T","version":"1"},"paths":{}}
                """;
        ParsedSpec parsed = parser.parse(json);
        assertTrue(parsed.valid(), "errors: " + parsed.errors());
        assertEquals("json", parsed.format());
    }

    @Test
    void rejectsSomethingThatIsNotASpec() {
        ParsedSpec parsed = parser.parse("just: some: yaml\nnot: a spec\n");
        assertFalse(parsed.valid());
        assertFalse(parsed.errors().isEmpty(), "should say what is wrong");
    }

    @Test
    void convertsYamlToJsonAndBack() throws Exception {
        String json = parser.toJson(V1);
        assertTrue(json.stripLeading().startsWith("{"));
        assertTrue(json.contains("Orders API"));

        String yaml = parser.toYaml(json);
        assertTrue(yaml.contains("title:"));
        assertFalse(yaml.stripLeading().startsWith("{"));
    }

    /** The feature that makes this a review tool: a removed endpoint is breaking. */
    @Test
    void flagsRemovedEndpointAsBreaking() {
        SpecDiffResult result = diff.diff(1, V1, 2, V2);

        assertFalse(result.compatible(), "removing an endpoint must not be compatible");
        assertTrue(result.breakingCount() >= 1);

        boolean removedFlagged = result.changes().stream()
                .anyMatch(c -> c.kind().equals("REMOVED")
                        && c.subject().contains("/orders/{id}")
                        && c.breaking());
        assertTrue(removedFlagged, "expected the removed endpoint flagged breaking: " + result.changes());
    }

    /** Additions alone must not be reported as breaking. */
    @Test
    void addedEndpointIsNotBreaking() {
        SpecDiffResult result = diff.diff(1, V1, 2, V2);

        boolean addedNonBreaking = result.changes().stream()
                .anyMatch(c -> c.kind().equals("ADDED")
                        && c.subject().contains("/customers")
                        && !c.breaking());
        assertTrue(addedNonBreaking, "adding an endpoint should be compatible: " + result.changes());
    }

    /** Identical documents must produce no changes at all. */
    @Test
    void identicalSpecsProduceNoChanges() {
        SpecDiffResult result = diff.diff(1, V1, 2, V1);
        assertTrue(result.compatible());
        assertEquals(0, result.breakingCount());
        assertTrue(result.changes().isEmpty(), "unexpected: " + result.changes());
    }
}
