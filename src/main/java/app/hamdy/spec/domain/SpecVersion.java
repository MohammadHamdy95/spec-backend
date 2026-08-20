package app.hamdy.spec.domain;

import java.time.Instant;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 * One revision of a spec. Immutable once written.
 *
 * <p>Partitioned by spec id and clustered by version DESC, so "give me the
 * latest" and "list the history" are both a single partition read, and
 * versions arrive newest-first without sorting.</p>
 */
@Table("spec_versions")
public class SpecVersion {

    @PrimaryKeyColumn(name = "spec_id", type = PrimaryKeyType.PARTITIONED)
    private String specId;

    @PrimaryKeyColumn(name = "version", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private int version;

    /**
     * The document exactly as the user supplied it, byte for byte.
     *
     * <p>Deliberately not normalised or re-serialised: people share specs to
     * be read, and reformatting someone's YAML — losing their comments, key
     * order and anchors — makes the shared copy differ from the file they
     * maintain. Rendering and diffing parse this on demand instead.</p>
     */
    private String body;

    /** {@code json} or {@code yaml} — the format it arrived in. */
    private String format;

    @Column("size_bytes")
    private int sizeBytes;

    @Column("operation_count")
    private int operationCount;

    /** Free-text note the editor can attach to a revision, e.g. "added /orders". */
    private String note;

    @Column("created_at")
    private Instant createdAt;

    public String getSpecId() { return specId; }
    public void setSpecId(String specId) { this.specId = specId; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public int getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(int sizeBytes) { this.sizeBytes = sizeBytes; }

    public int getOperationCount() { return operationCount; }
    public void setOperationCount(int operationCount) { this.operationCount = operationCount; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
