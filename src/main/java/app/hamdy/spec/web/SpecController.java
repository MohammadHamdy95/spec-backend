package app.hamdy.spec.web;

import java.util.List;
import java.util.Map;

import app.hamdy.spec.domain.SpecDoc;
import app.hamdy.spec.domain.SpecVersion;
import app.hamdy.spec.service.SpecDiffResult;
import app.hamdy.spec.service.SpecService;
import app.hamdy.spec.web.dto.CreateSpecRequest;
import app.hamdy.spec.web.dto.PublicUrls;
import app.hamdy.spec.web.dto.SpecResponse;
import app.hamdy.spec.web.dto.UpdateSpecRequest;
import app.hamdy.spec.web.dto.VersionSummary;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Served same-origin under /v1/*.
 *
 * <p>Deliberately not on api.spec.hamdy.app: that is a two-level subdomain,
 * which Cloudflare's free Universal SSL does not cover — the same trap that
 * forced paste onto a path prefix.</p>
 */
@RestController
@RequestMapping("/v1")
public class SpecController {

    /** Ids are fixed-length base62; constrain the path so junk 404s early. */
    private static final String ID = "{id:[0-9A-Za-z]{10}}";

    private final SpecService service;
    private final PublicUrls urls;

    public SpecController(SpecService service, PublicUrls urls) {
        this.service = service;
        this.urls = urls;
    }

    @PostMapping("/specs")
    public ResponseEntity<SpecResponse> create(@Valid @RequestBody CreateSpecRequest request) {
        SpecService.Created created =
                service.create(request.body(), request.expiresInDays(), request.note());
        SpecDoc doc = created.doc();
        SpecVersion version = service.getVersion(doc.getId(), 1);

        // The only response that carries the edit token.
        return ResponseEntity.status(HttpStatus.CREATED).body(
                SpecResponse.of(doc, version, urls.forSpec(doc.getId()), created.editToken()));
    }

    @PutMapping("/specs/" + ID)
    public SpecResponse update(@PathVariable String id,
                               @RequestHeader("X-Edit-Token") String editToken,
                               @Valid @RequestBody UpdateSpecRequest request) {
        SpecDoc doc = service.update(id, editToken, request.body(), request.note());
        return SpecResponse.of(doc, service.getLatest(id), urls.forSpec(id), null);
    }

    @GetMapping("/specs/" + ID)
    public SpecResponse get(@PathVariable String id) {
        SpecDoc doc = service.get(id);
        return SpecResponse.of(doc, service.getLatest(id), urls.forSpec(id), null);
    }

    @GetMapping("/specs/" + ID + "/versions/{version:[0-9]{1,6}}")
    public SpecResponse getVersion(@PathVariable String id, @PathVariable int version) {
        SpecDoc doc = service.get(id);
        return SpecResponse.of(doc, service.getVersion(id, version), urls.forSpec(id), null);
    }

    @GetMapping("/specs/" + ID + "/versions")
    public List<VersionSummary> versions(@PathVariable String id) {
        return service.listVersions(id).stream().map(VersionSummary::of).toList();
    }

    /**
     * Semantic diff. Defaults to "the previous revision against the latest",
     * which is the comparison people almost always want.
     */
    @GetMapping("/specs/" + ID + "/diff")
    public SpecDiffResult diff(@PathVariable String id,
                               @RequestParam(required = false) Integer from,
                               @RequestParam(required = false) Integer to) {
        int latest = service.get(id).getLatestVersion();
        int toVersion = to == null ? latest : to;
        int fromVersion = from == null ? Math.max(1, toVersion - 1) : from;
        return service.diff(id, fromVersion, toVersion);
    }

    /** Raw document, in the format it was submitted. */
    @GetMapping(value = "/specs/" + ID + "/raw", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> raw(@PathVariable String id) {
        return ResponseEntity.ok(service.getLatest(id).getBody());
    }

    /** Same document as JSON, whatever it arrived as. */
    @GetMapping(value = "/specs/" + ID + "/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> json(@PathVariable String id) throws Exception {
        return ResponseEntity.ok(service.asJson(service.getLatest(id).getBody()));
    }

    /** Same document as YAML, whatever it arrived as. */
    @GetMapping(value = "/specs/" + ID + "/yaml", produces = "application/yaml")
    public ResponseEntity<String> yaml(@PathVariable String id) throws Exception {
        return ResponseEntity.ok(service.asYaml(service.getLatest(id).getBody()));
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
