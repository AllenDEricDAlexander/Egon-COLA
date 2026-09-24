package top.egon.cola.component.outbox.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlManifestBO;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlResult;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlTargetBO;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlTargetBO.RoleEnum;
import top.egon.cola.component.common.mybatis.ddl.EgonColaPostgreDdlRunner;
import top.egon.cola.component.common.mybatis.routing.EgonColaPhysicalTargetBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.PartitionKeyBO;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.ConfigStyleEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.DataSourceRoleEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.ModeEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.PhysicalDataSourceProperties;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingTopologyValidator;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingTopologyValidator.TopologyBO;
import top.egon.cola.component.outbox.autoconfigure.OutboxMpStorageProperties;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Initializes the fixed outbox schema through the shared managed-DDL runner. */
@Slf4j
@Validated
@RequiredArgsConstructor
public class OutboxManagedDdlInitializer {

    private static final String OUTBOX_SCHEMA = "egon_outbox";
    private static final List<String> REQUIRED_TABLES = List.of("egon_cola_outbox_message", "ddl_history");

    @Qualifier("egonColaPostgreDdlRunner")
    private final EgonColaPostgreDdlRunner ddlRunner;

    @Qualifier("egonColaShardingTopologyValidator")
    private final EgonColaShardingTopologyValidator topologyValidator;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Qualifier("egon.cola.component.mybatis-plus.sharding-top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties")
    private final EgonColaShardingProperties shardingProperties;

    @Qualifier("outboxMpStorageProperties")
    private final OutboxMpStorageProperties storageProperties;

    @Qualifier("outboxStateMachineObjectMapper")
    private final ObjectMapper objectMapper;

    private final ResourcePatternResolver resources = new PathMatchingResourcePatternResolver();

    private volatile ReadinessBO readiness;

    public synchronized void initialize(@NotEmpty Map<@NotBlank String, @NotNull DataSource> physicalSources,
                                        @NotEmpty byte[] yaml) {
        readiness = null;
        if (physicalSources == null || physicalSources.isEmpty() || yaml == null || yaml.length == 0) {
            throw new IllegalArgumentException("OUTBOX_DDL_INPUT_REQUIRED");
        }
        validationUtils.validate(shardingProperties);
        validationUtils.validate(storageProperties);
        validateStorageTopology();

        PhysicalDataSourceProperties primary = primaryDataSource();
        DataSource primaryDataSource = physicalSources.get(primary.name());
        if (physicalSources.size() != 1 || primaryDataSource == null) {
            throw new IllegalStateException("OUTBOX_SINGLE_PRIMARY_REQUIRED");
        }

        TopologyBO topology = topologyValidator.validate(shardingProperties, yaml);
        validateTransactionAndSingleNodes(topology, primary.name());
        EgonColaDdlManifestBO manifest = loadManifest();
        DataSource scopedDataSource = new SchemaScopedDataSource(primaryDataSource, OUTBOX_SCHEMA);
        List<EgonColaDdlResult> results = ddlRunner.run(List.of(new EgonColaDdlTargetBO(
                primary.name(), OUTBOX_SCHEMA, RoleEnum.MASTER_DATA, scopedDataSource, manifest, topology.fingerprint())));
        validateRunnerResults(results, manifest, primary.name());
        readiness = new ReadinessBO(primaryDataSource, topology.fingerprint());
        log.info("outbox managed DDL ready: schema={} scripts={} routeFingerprint={}",
                OUTBOX_SCHEMA, results.size(), topology.fingerprint());
    }

    public boolean isReadyFor(@NotNull DataSource primaryDataSource, @NotBlank String routeFingerprint) {
        ReadinessBO current = readiness;
        return current != null && current.primaryDataSource() == primaryDataSource
                && current.routeFingerprint().equals(routeFingerprint);
    }

    private void validateStorageTopology() {
        if (shardingProperties.getMode() != ModeEnum.SHARDING
                || shardingProperties.getConfigStyle() != ConfigStyleEnum.NATIVE
                || !"LOCAL".equals(shardingProperties.getTransactionDefaultType())) {
            throw new IllegalStateException("OUTBOX_NATIVE_LOCAL_SHARDING_REQUIRED");
        }
        if (shardingProperties.getDataSources() == null || shardingProperties.getDataSources().size() != 1
                || shardingProperties.getDataSources().getFirst().role() != DataSourceRoleEnum.PRIMARY) {
            throw new IllegalStateException("OUTBOX_SINGLE_PRIMARY_REQUIRED");
        }
        if (!"db/egon-outbox-mp/manifest.json".equals(storageProperties.getManifestResource())) {
            throw new IllegalStateException("OUTBOX_DDL_MANIFEST_RESOURCE_FIXED");
        }
    }

    private PhysicalDataSourceProperties primaryDataSource() {
        List<PhysicalDataSourceProperties> dataSources = shardingProperties.getDataSources();
        if (dataSources.size() != 1 || dataSources.getFirst().role() != DataSourceRoleEnum.PRIMARY) {
            throw new IllegalStateException("OUTBOX_SINGLE_PRIMARY_REQUIRED");
        }
        return dataSources.getFirst();
    }

    private static void validateTransactionAndSingleNodes(TopologyBO topology, String primaryName) {
        org.apache.shardingsphere.driver.yaml.YamlJDBCConfiguration configuration =
                org.apache.shardingsphere.infra.util.yaml.YamlEngine.unmarshal(
                        new String(topology.yaml(), java.nio.charset.StandardCharsets.UTF_8),
                        org.apache.shardingsphere.driver.yaml.YamlJDBCConfiguration.class);
        if (configuration.getTransaction() == null
                || !"LOCAL".equals(configuration.getTransaction().getDefaultType())) {
            throw new IllegalStateException("OUTBOX_LOCAL_TRANSACTION_REQUIRED");
        }
        for (String table : REQUIRED_TABLES) {
            EgonColaRoutingProfileBO profile = topology.profiles().get(table);
            if (profile == null || profile.kind() != EgonColaRoutingProfileBO.TableKindEnum.SINGLE
                    || profile.actualNodes().size() != 1) {
                throw new IllegalStateException("OUTBOX_SINGLE_NODE_REQUIRED: " + table);
            }
            List<EgonColaPhysicalTargetBO> nodes = profile.actualNodes().get(new PartitionKeyBO(0, 0));
            if (nodes == null || nodes.size() != 1) {
                throw new IllegalStateException("OUTBOX_SINGLE_NODE_REQUIRED: " + table);
            }
            EgonColaPhysicalTargetBO node = nodes.getFirst();
            if (!primaryName.equals(node.group()) || !OUTBOX_SCHEMA.equals(node.schema()) || !table.equals(node.table())) {
                throw new IllegalStateException("OUTBOX_SINGLE_NODE_MISMATCH: " + table);
            }
        }
    }

    private EgonColaDdlManifestBO loadManifest() {
        String resourcePath = "classpath*:" + storageProperties.getManifestResource();
        try {
            var matches = resources.getResources(resourcePath);
            if (matches.length != 1) {
                throw new IllegalStateException("OUTBOX_DDL_MANIFEST_RESOURCE_AMBIGUOUS");
            }
            try (var input = matches[0].getInputStream()) {
                EgonColaDdlManifestBO manifest = objectMapper.readValue(input, EgonColaDdlManifestBO.class);
                validationUtils.validate(manifest);
                if (!"component-outbox".equals(manifest.family())) {
                    throw new IllegalStateException("OUTBOX_DDL_MANIFEST_FAMILY_INVALID");
                }
                return manifest;
            }
        } catch (IOException failure) {
            throw new IllegalStateException("OUTBOX_DDL_MANIFEST_UNREADABLE", failure);
        }
    }

    private void validateRunnerResults(List<EgonColaDdlResult> results, EgonColaDdlManifestBO manifest,
                                       String primaryName) {
        if (results == null || results.size() != manifest.scripts().size()) {
            throw new IllegalStateException("OUTBOX_DDL_MANIFEST_INCOMPLETE");
        }
        for (int index = 0; index < results.size(); index++) {
            EgonColaDdlResult result = validationUtils.validate(results.get(index));
            EgonColaDdlManifestBO.ScriptBO script = manifest.scripts().get(index);
            if (!primaryName.equals(result.alias()) || !OUTBOX_SCHEMA.equals(result.schema())
                    || !script.version().equals(result.version()) || !script.sha256().equals(result.checksum())) {
                throw new IllegalStateException("OUTBOX_DDL_RESULT_MISMATCH");
            }
        }
    }

    private record ReadinessBO(DataSource primaryDataSource, String routeFingerprint) {
    }

    private static final class SchemaScopedDataSource extends AbstractDataSource {

        private final DataSource delegate;
        private final String schema;

        private SchemaScopedDataSource(DataSource delegate, String schema) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.schema = Objects.requireNonNull(schema, "schema");
        }

        @Override
        public Connection getConnection() throws SQLException {
            return scope(delegate.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return scope(delegate.getConnection(username, password));
        }

        private Connection scope(Connection connection) throws SQLException {
            String originalSchema;
            try {
                originalSchema = connection.getSchema();
                if (originalSchema == null || originalSchema.isBlank()) {
                    throw new SQLException("OUTBOX_DDL_ORIGINAL_SCHEMA_UNKNOWN");
                }
                connection.setSchema(schema);
            } catch (SQLException failure) {
                try {
                    connection.close();
                } catch (SQLException closeFailure) {
                    failure.addSuppressed(closeFailure);
                }
                throw failure;
            }

            AtomicBoolean closed = new AtomicBoolean();
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, arguments) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return switch (method.getName()) {
                                case "toString" -> "OutboxSchemaScopedConnection[" + schema + ']';
                                case "hashCode" -> System.identityHashCode(proxy);
                                case "equals" -> proxy == arguments[0];
                                default -> throw new UnsupportedOperationException(method.getName());
                            };
                        }
                        if ("close".equals(method.getName()) && method.getParameterCount() == 0) {
                            closeScoped(connection, originalSchema, closed);
                            return null;
                        }
                        if ("unwrap".equals(method.getName()) && method.getParameterCount() == 1) {
                            Class<?> type = (Class<?>) arguments[0];
                            if (type.isInstance(proxy)) {
                                return proxy;
                            }
                            throw new SQLException("OUTBOX_DDL_CONNECTION_UNWRAP_UNSUPPORTED");
                        }
                        if ("isWrapperFor".equals(method.getName()) && method.getParameterCount() == 1) {
                            return ((Class<?>) arguments[0]).isInstance(proxy);
                        }
                        try {
                            return method.invoke(connection, arguments);
                        } catch (InvocationTargetException failure) {
                            throw failure.getCause();
                        }
                    });
        }

        private static void closeScoped(Connection connection, String originalSchema, AtomicBoolean closed)
                throws SQLException {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            SQLException failure = null;
            try {
                if (!connection.isClosed()) {
                    connection.setSchema(originalSchema);
                }
            } catch (SQLException restoreFailure) {
                failure = restoreFailure;
            }
            try {
                connection.close();
            } catch (SQLException closeFailure) {
                if (failure == null) {
                    failure = closeFailure;
                } else {
                    failure.addSuppressed(closeFailure);
                }
            }
            if (failure != null) {
                throw failure;
            }
        }
    }
}
