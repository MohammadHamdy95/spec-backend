package app.hamdy.spec.domain;

import java.time.Instant;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 * The shareable document: a stable id and link, plus a pointer to whichever
 * version is current.
 *
 * <p>The body does not live here. A spec is edited repeatedly and every prior
 * state has to stay reachable — someone shared v2 with a colleague and that
 * link must keep working after v3 lands — so bodies live in
 * {@link SpecVersion} and this row only tracks which one is latest.</p>
 */
@Table("specs")
public class SpecDoc {

    @PrimaryKey
    private String id;

    /** Title pulled from the document's {@code info.title}, for listings and OG cards. */
    private String title;

    /** The API's own version, from {@code info.version} — not our revision number. */
    @Column("api_version")
    private String apiVersion;

    /** Which spec dialect: {@code swagger_2_0}, {@code openapi_3_0}, {@code openapi_3_1}. */
    @Column("spec_version")
    private String specVersion;

    /** Revision number of the current version; {@link SpecVersion} rows count 1..n. */
    @Column("latest_version")
    private int latestVersion;

    /**
     * SHA-256 of the edit token. Only the hash is stored: the token is shown
     * once at creation and cannot be recovered, so a database read does not
     * hand over the ability to rewrite everyone's specs.
     */
    @Column("edit_token_hash")
    private String editTokenHash;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Column("expires_at")
    private Instant expiresAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }

    public String getSpecVersion() { return specVersion; }
    public void setSpecVersion(String specVersion) { this.specVersion = specVersion; }

    public int getLatestVersion() { return latestVersion; }
    public void setLatestVersion(int latestVersion) { this.latestVersion = latestVersion; }

    public String getEditTokenHash() { return editTokenHash; }
    public void setEditTokenHash(String editTokenHash) { this.editTokenHash = editTokenHash; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
