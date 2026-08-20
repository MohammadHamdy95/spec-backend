package app.hamdy.spec.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import app.hamdy.spec.domain.SpecDoc;
import app.hamdy.spec.domain.SpecVersion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.query.Criteria;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.stereotype.Service;

/**
 * Create, read, update and compare shared specs.
 *
 * <p>Updating is what separates this from a pastebin: the share link stays
 * fixed while the content moves forward, and every previous state stays
 * reachable so a link someone sent last month still shows what they meant.</p>
 */
@Service
public class SpecService {

    private final CassandraTemplate cassandra;
    private final SpecParser parser;
    private final SpecDiffService diffService;
    private final int maxContentBytes;

    public SpecService(CassandraTemplate cassandra,
                       SpecParser parser,
                       SpecDiffService diffService,
                       @Value("${spec.max-content-bytes:20971520}") int maxContentBytes) {
        this.cassandra = cassandra;
        this.parser = parser;
        this.diffService = diffService;
        this.maxContentBytes = maxContentBytes;
    }

    /** @return the created doc plus the edit token, which is shown exactly once. */
    public Created create(String body, Integer expiresInDays, String note) {
        ParsedSpec parsed = validate(body);

        String token = SpecIds.generateEditToken();
        Instant now = Instant.now();

        SpecDoc doc = new SpecDoc();
        doc.setId(SpecIds.generateId());
        doc.setTitle(parsed.title());
        doc.setApiVersion(parsed.apiVersion());
        doc.setSpecVersion(parsed.specVersion());
        doc.setLatestVersion(1);
        doc.setEditTokenHash(SpecIds.hashToken(token));
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);
        doc.setExpiresAt(expiresAt(expiresInDays, now));

        cassandra.insert(doc);
        cassandra.insert(version(doc.getId(), 1, body, parsed, note, now));

        return new Created(doc, token);
    }

    /**
     * Adds a revision. The id and share link are unchanged; only
     * {@code latestVersion} moves.
     */
    public SpecDoc update(String id, String editToken, String body, String note) {
        SpecDoc doc = require(id);
        if (!SpecIds.tokenMatches(editToken, doc.getEditTokenHash())) {
            throw new SpecForbiddenException("Wrong or missing edit token.");
        }

        ParsedSpec parsed = validate(body);
        Instant now = Instant.now();
        int next = doc.getLatestVersion() + 1;

        cassandra.insert(version(id, next, body, parsed, note, now));

        // Metadata follows the newest revision: a spec that gets renamed or
        // rev'd should show the current title, not the one it launched with.
        doc.setTitle(parsed.title());
        doc.setApiVersion(parsed.apiVersion());
        doc.setSpecVersion(parsed.specVersion());
        doc.setLatestVersion(next);
        doc.setUpdatedAt(now);
        cassandra.update(doc);

        return doc;
    }

    public SpecDoc get(String id) {
        SpecDoc doc = require(id);
        if (doc.getExpiresAt() != null && doc.getExpiresAt().isBefore(Instant.now())) {
            throw new SpecNotFoundException("This spec has expired.");
        }
        return doc;
    }

    public SpecVersion getVersion(String id, int version) {
        SpecVersion found = cassandra.selectOne(
                Query.query(Criteria.where("spec_id").is(id))
                        .and(Criteria.where("version").is(version)),
                SpecVersion.class);
        if (found == null) {
            throw new SpecNotFoundException("Version " + version + " does not exist.");
        }
        return found;
    }

    public SpecVersion getLatest(String id) {
        return getVersion(id, get(id).getLatestVersion());
    }

    /** Newest first — the clustering order does the sorting. */
    public List<SpecVersion> listVersions(String id) {
        require(id);
        return cassandra.select(
                Query.query(Criteria.where("spec_id").is(id)), SpecVersion.class);
    }

    public SpecDiffResult diff(String id, int fromVersion, int toVersion) {
        get(id);
        return diffService.diff(
                fromVersion, getVersion(id, fromVersion).getBody(),
                toVersion, getVersion(id, toVersion).getBody());
    }

    public String asJson(String body) throws Exception {
        return parser.toJson(body);
    }

    public String asYaml(String body) throws Exception {
        return parser.toYaml(body);
    }

    // ── internals ────────────────────────────────────────────────────

    private ParsedSpec validate(String body) {
        int bytes = body.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > maxContentBytes) {
            throw new SpecTooLargeException(
                    "Spec is " + bytes + " bytes; the limit is " + maxContentBytes + ".");
        }
        ParsedSpec parsed = parser.parse(body);
        if (!parsed.valid()) {
            // Refuse rather than publish something broken: a share link that
            // renders an error is worse than being told at paste time.
            throw new SpecInvalidException(parsed.errors());
        }
        return parsed;
    }

    private SpecVersion version(String specId, int number, String body,
                                ParsedSpec parsed, String note, Instant at) {
        SpecVersion v = new SpecVersion();
        v.setSpecId(specId);
        v.setVersion(number);
        v.setBody(body);
        v.setFormat(parsed.format());
        v.setSizeBytes(body.getBytes(StandardCharsets.UTF_8).length);
        v.setOperationCount(parsed.operationCount());
        v.setNote(note);
        v.setCreatedAt(at);
        return v;
    }

    private SpecDoc require(String id) {
        SpecDoc doc = cassandra.selectOneById(id, SpecDoc.class);
        if (doc == null) {
            throw new SpecNotFoundException("No spec with that id.");
        }
        return doc;
    }

    private Instant expiresAt(Integer days, Instant from) {
        // null means never — specs are reference documents people bookmark,
        // so an unbounded default is the useful one here.
        return days == null || days <= 0 ? null : from.plus(Duration.ofDays(days));
    }

    public record Created(SpecDoc doc, String editToken) {
    }
}
