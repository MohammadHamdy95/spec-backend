package app.hamdy.spec.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification;

/**
 * Cassandra bootstrap, same shape as paste-backend. Extends the Spring Data
 * base configuration rather than relying on Boot auto-config purely for
 * keyspace creation: getKeyspaceCreations() runs against the system session
 * before the keyspace session opens, so a fresh node needs zero manual DDL.
 * Tables come from the entity model via CREATE_IF_NOT_EXISTS.
 */
@Configuration
public class CassandraConfig extends AbstractCassandraConfiguration {

    @Value("${spring.cassandra.contact-points:localhost}")
    private String contactPoints;

    @Value("${spring.cassandra.port:9042}")
    private int port;

    @Value("${spring.cassandra.local-datacenter:datacenter1}")
    private String localDataCenter;

    @Value("${spring.cassandra.keyspace-name:spec}")
    private String keyspaceName;

    @Override
    protected String getKeyspaceName() {
        return keyspaceName;
    }

    @Override
    protected String getContactPoints() {
        return contactPoints;
    }

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    protected String getLocalDataCenter() {
        return localDataCenter;
    }

    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.CREATE_IF_NOT_EXISTS;
    }

    @Override
    protected List<CreateKeyspaceSpecification> getKeyspaceCreations() {
        // SimpleStrategy RF=1 matches the single-node deployment, as with paste.
        return List.of(CreateKeyspaceSpecification.createKeyspace(keyspaceName)
                .ifNotExists()
                .withSimpleReplication(1));
    }

    @Override
    public String[] getEntityBasePackages() {
        return new String[] { "app.hamdy.spec" };
    }
}
