package top.egon.cola.component.outbox.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.testcontainers.containers.PostgreSQLContainer;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlManifestBO;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlResult;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlResult.StatusEnum;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlTargetBO;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlTargetBO.RoleEnum;
import top.egon.cola.component.common.mybatis.ddl.EgonColaPostgreDdlRunner;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.ConfigStyleEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.DataSourceRoleEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.ModeEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.PhysicalDataSourceProperties;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingTopologyValidator;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingTopologyValidator.TopologyBO;
import top.egon.cola.component.outbox.autoconfigure.OutboxMpStorageProperties;
import top.egon.cola.component.outbox.autoconfigure.OutboxMybatisPlusAutoConfiguration;
import top.egon.cola.component.outbox.migration.OutboxLogicalDataSourceFactory;
import top.egon.cola.component.outbox.migration.OutboxManagedDdlInitializer;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxManagedDdlIntegrationTest {

    private static final String POSTGRES_ENABLED = "EGON_OUTBOX_TEST_POSTGRES_ENABLED";
    private static final String SCHEMA = "egon_outbox";
    private static final String BASE_YAML = """
            databaseName: egon
            rules:
              - !SINGLE
                tables:
                  - primary.egon_outbox.egon_cola_outbox_message
                  - primary.egon_outbox.ddl_history
            transaction:
              defaultType: LOCAL
            """;
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static PostgreSQLContainer<?> postgresql;

    @Test
    void ddlFailurePreventsLogicalDataSourceCreationWithoutOpeningPostgres() throws Exception {
        OutboxManagedDdlInitializer initializer = mock(OutboxManagedDdlInitializer.class);
        IllegalStateException ddlFailure = new IllegalStateException("managed DDL unavailable");
        doThrow(ddlFailure).when(initializer).initialize(anyMap(), any(byte[].class));

        OutboxLogicalDataSourceFactory factory = new OutboxLogicalDataSourceFactory(initializer);
        Map<String, DataSource> physical = Map.of("primary", mock(DataSource.class));
        byte[] yaml = "invalid-sharding-yaml".getBytes(StandardCharsets.UTF_8);

        RuntimeException failure = assertThrows(RuntimeException.class, () -> factory.create(physical, yaml));

        assertSame(ddlFailure, failure);
        verify(initializer).initialize(physical, yaml);
    }

    @Test
    void validatesNativePrimaryTopologyAndUsesTheCommonRunnerWithoutOpeningPostgres() {
        ValidationUtils validationUtils = new ValidationUtils(VALIDATOR_FACTORY.getValidator());
        EgonColaShardingProperties shardingProperties = shardingPropertiesWithoutPostgres();
        EgonColaShardingTopologyValidator topologyValidator = new EgonColaShardingTopologyValidator(validationUtils);
        TopologyBO expectedTopology = topologyValidator.validate(shardingProperties, BASE_YAML.getBytes(StandardCharsets.UTF_8));
        EgonColaPostgreDdlRunner runner = mock(EgonColaPostgreDdlRunner.class);
        AtomicReference<EgonColaDdlTargetBO> targetReference = new AtomicReference<>();
        when(runner.run(anyList())).thenAnswer(invocation -> {
            EgonColaDdlTargetBO target = (EgonColaDdlTargetBO) ((List<?>) invocation.getArgument(0)).getFirst();
            targetReference.set(target);
            return target.manifest().scripts().stream()
                    .map(script -> new EgonColaDdlResult(target.alias(), target.schema(), script.version(),
                            script.sha256(), StatusEnum.APPLIED, Duration.ZERO))
                    .toList();
        });
        OutboxMpStorageProperties storageProperties = new OutboxMpStorageProperties();
        DataSource primary = mock(DataSource.class);
        OutboxManagedDdlInitializer initializer = new OutboxManagedDdlInitializer(runner, topologyValidator,
                validationUtils, shardingProperties, storageProperties, new ObjectMapper());

        initializer.initialize(Map.of("primary", primary), BASE_YAML.getBytes(StandardCharsets.UTF_8));

        EgonColaDdlTargetBO target = targetReference.get();
        Assertions.assertNotNull(target);
        Assertions.assertEquals("primary", target.alias());
        Assertions.assertEquals("egon_outbox", target.schema());
        Assertions.assertEquals(RoleEnum.MASTER_DATA, target.role());
        Assertions.assertEquals("component-outbox", target.manifest().family());
        Assertions.assertEquals(expectedTopology.fingerprint(), target.routeFingerprint());
        Assertions.assertNotSame(primary, target.dataSource());
        Assertions.assertTrue(initializer.isReadyFor(primary, expectedTopology.fingerprint()));
        Assertions.assertSame(primary, initializer.physicalMetadataDataSource(expectedTopology.fingerprint()));
        Assertions.assertFalse(initializer.isReadyFor(mock(DataSource.class), expectedTopology.fingerprint()));
        Assertions.assertThrows(IllegalStateException.class,
                () -> initializer.physicalMetadataDataSource("a".repeat(64)));
    }

    @Test
    void bindsNamedMpStoragePropertiesAndRegistersTheManagedHookWithoutPostgres() {
        storageConfigurationRunner()
                .withPropertyValues(
                        OutboxMpStorageProperties.PREFIX + ".sql-session-factory-bean-name=outboxSqlSessionFactory",
                        OutboxMpStorageProperties.PREFIX + ".migration-mode=true",
                        OutboxMpStorageProperties.PREFIX + ".migration-lock-timeout=12s")
                .run(context -> {
                    Assertions.assertEquals("outboxSqlSessionFactory",
                            context.getBean("outboxMpStorageProperties", OutboxMpStorageProperties.class)
                                    .getSqlSessionFactoryBeanName());
                    Assertions.assertTrue(context.getBean("outboxMpStorageProperties", OutboxMpStorageProperties.class)
                            .isMigrationMode());
                    Assertions.assertEquals(Duration.ofSeconds(12),
                            context.getBean("outboxMpStorageProperties", OutboxMpStorageProperties.class)
                                    .getMigrationLockTimeout());
                    Assertions.assertEquals("db/egon-outbox-mp/manifest.json",
                            context.getBean("outboxMpStorageProperties", OutboxMpStorageProperties.class)
                                    .getManifestResource());
                    Assertions.assertTrue(context.containsBean("outboxManagedDdlInitializer"));
                    Assertions.assertTrue(context.containsBean("egonColaShardingLogicalDataSourceFactory"));
                });
        for (String invalidSetting : List.of(
                OutboxMpStorageProperties.PREFIX + ".migration-lock-timeout=0s",
                OutboxMpStorageProperties.PREFIX + ".manifest-resource=https://example.invalid/manifest.json")) {
            storageConfigurationRunner().withPropertyValues(invalidSetting)
                    .run(context -> Assertions.assertNotNull(context.getStartupFailure()));
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = POSTGRES_ENABLED, matches = "true")
    void appliesTheManifestOnceAndSkipsItOnRestart() throws Exception {
        FixtureBO fixture = fixture();

        fixture.initializer().initialize(fixture.physicalSources(), fixture.yaml());
        fixture.initializer().initialize(fixture.physicalSources(), fixture.yaml());

        assertTrue(tableExists(fixture.physicalDataSource(), "egon_cola_outbox_message"));
        assertEquals(1, scalar(fixture.physicalDataSource(), "SELECT count(*) FROM egon_outbox.ddl_history"));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = POSTGRES_ENABLED, matches = "true")
    void rejectsNonEmptySchemaWithoutManagedHistory() throws Exception {
        FixtureBO fixture = fixture();
        try (Connection connection = fixture.physicalDataSource().getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE egon_outbox.unmanaged_marker (id integer)");
        }

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> fixture.initializer().initialize(fixture.physicalSources(), fixture.yaml()));

        assertCauseContains(failure, "REBUILD_REQUIRED");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = POSTGRES_ENABLED, matches = "true")
    void rejectsChecksumDriftInTheInstalledPrefix() throws Exception {
        FixtureBO fixture = fixture();
        fixture.initializer().initialize(fixture.physicalSources(), fixture.yaml());
        execute(fixture.physicalDataSource(), "UPDATE egon_outbox.ddl_history SET checksum = repeat('a', 64)");

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> fixture.initializer().initialize(fixture.physicalSources(), fixture.yaml()));

        assertCauseContains(failure, "CHECKSUM_MISMATCH");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = POSTGRES_ENABLED, matches = "true")
    void rejectsRouteFingerprintDriftInTheInstalledPrefix() throws Exception {
        FixtureBO fixture = fixture();
        fixture.initializer().initialize(fixture.physicalSources(), fixture.yaml());
        byte[] changedYaml = BASE_YAML.replace("transaction:",
                "  - !SINGLE\n    tables: [primary.public.routing_metadata]\ntransaction:")
                .getBytes(StandardCharsets.UTF_8);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> fixture.initializer().initialize(fixture.physicalSources(), changedYaml));

        assertCauseContains(failure, "ROUTE_FINGERPRINT_MISMATCH");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = POSTGRES_ENABLED, matches = "true")
    void rollsBackSqlAndHistoryTogetherWhenAScriptFails() throws Exception {
        FixtureBO fixture = fixture();
        byte[] sql = "CREATE TABLE rollback_probe (id integer); SELECT 1 / 0;".getBytes(StandardCharsets.UTF_8);
        String path = "db/test/rollback_probe.sql";
        ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
        when(resolver.getResources(anyString())).thenReturn(new Resource[]{new ByteArrayResource(sql)});
        EgonColaPostgreDdlRunner runner = runner(fixture.validationUtils(), resolver);
        EgonColaDdlManifestBO manifest = new EgonColaDdlManifestBO("component-outbox", List.of(
                new EgonColaDdlManifestBO.ScriptBO("20260924_001", path, sha256(sql))));
        EgonColaDdlTargetBO target = new EgonColaDdlTargetBO("primary", SCHEMA, RoleEnum.MASTER_DATA,
                fixture.physicalDataSource(), manifest, fixture.routeFingerprint());

        assertThrows(IllegalStateException.class, () -> runner.run(List.of(target)));

        assertFalse(tableExists(fixture.physicalDataSource(), "rollback_probe"));
        assertFalse(tableExists(fixture.physicalDataSource(), "ddl_history"));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = POSTGRES_ENABLED, matches = "true")
    void concurrentInitializersSerializeOnTheManagedSchemaLock() throws Exception {
        FixtureBO fixture = fixture();
        OutboxManagedDdlInitializer second = initializer(fixture.shardingProperties(), fixture.storageProperties(),
                fixture.validationUtils());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> firstResult = executor.submit(() -> initializeTogether(fixture.initializer(), fixture, ready, start));
            Future<?> secondResult = executor.submit(() -> initializeTogether(second, fixture, ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            firstResult.get(60, TimeUnit.SECONDS);
            secondResult.get(60, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        assertTrue(tableExists(fixture.physicalDataSource(), "egon_cola_outbox_message"));
        assertEquals(1, scalar(fixture.physicalDataSource(), "SELECT count(*) FROM egon_outbox.ddl_history"));
    }

    @AfterEach
    void removeTestSchema() throws Exception {
        if (postgresql != null && postgresql.isRunning()) {
            try (Connection connection = adminDataSource().getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS egon_outbox CASCADE");
            }
        }
    }

    @AfterAll
    static void stopPostgresql() {
        if (postgresql != null) {
            postgresql.stop();
        }
        VALIDATOR_FACTORY.close();
    }

    private static FixtureBO fixture() throws Exception {
        PostgreSQLContainer<?> container = postgresql();
        execute(adminDataSource(), "CREATE SCHEMA egon_outbox");
        PGSimpleDataSource physical = dataSource(SCHEMA);
        ValidationUtils validationUtils = new ValidationUtils(VALIDATOR_FACTORY.getValidator());
        EgonColaShardingProperties shardingProperties = shardingProperties(container);
        OutboxMpStorageProperties storageProperties = new OutboxMpStorageProperties();
        byte[] yaml = BASE_YAML.getBytes(StandardCharsets.UTF_8);
        TopologyBO topology = new EgonColaShardingTopologyValidator(validationUtils).validate(shardingProperties, yaml);
        return new FixtureBO(initializer(shardingProperties, storageProperties, validationUtils),
                Map.of("primary", physical), yaml, physical, validationUtils, shardingProperties,
                storageProperties, topology.fingerprint());
    }

    private static OutboxManagedDdlInitializer initializer(EgonColaShardingProperties shardingProperties,
                                                            OutboxMpStorageProperties storageProperties,
                                                            ValidationUtils validationUtils) {
        return new OutboxManagedDdlInitializer(runner(validationUtils, new PathMatchingResourcePatternResolver()),
                new EgonColaShardingTopologyValidator(validationUtils), validationUtils, shardingProperties,
                storageProperties, new ObjectMapper());
    }

    private static EgonColaPostgreDdlRunner runner(ValidationUtils validationUtils,
                                                   ResourcePatternResolver resolver) {
        return new EgonColaPostgreDdlRunner(validationUtils, resolver, Clock.systemUTC(),
                Duration.ofSeconds(5), Duration.ofSeconds(30));
    }

    private static ApplicationContextRunner storageConfigurationRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        ConfigurationPropertiesAutoConfiguration.class,
                        ValidationAutoConfiguration.class))
                .withUserConfiguration(OutboxMybatisPlusAutoConfiguration.class, TestDependenciesConfiguration.class);
    }

    private static EgonColaShardingProperties shardingProperties(PostgreSQLContainer<?> container) {
        return shardingProperties(container.getJdbcUrl(), container.getUsername(), container.getPassword());
    }

    private static EgonColaShardingProperties shardingPropertiesWithoutPostgres() {
        return shardingProperties("jdbc:postgresql://localhost/test", "test", "test");
    }

    private static EgonColaShardingProperties shardingProperties(String jdbcUrl, String username, String password) {
        return EgonColaShardingProperties.builder()
                .mode(ModeEnum.SHARDING)
                .configStyle(ConfigStyleEnum.NATIVE)
                .transactionDefaultType("LOCAL")
                .dataSources(List.of(new PhysicalDataSourceProperties("primary", "primary", DataSourceRoleEnum.PRIMARY,
                        "org.postgresql.Driver", jdbcUrl, username, password)))
                .build();
    }

    private static PostgreSQLContainer<?> postgresql() {
        if (postgresql == null) {
            synchronized (OutboxManagedDdlIntegrationTest.class) {
                if (postgresql == null) {
                    postgresql = new PostgreSQLContainer<>("postgres:16.6-alpine");
                    postgresql.start();
                }
            }
        }
        return postgresql;
    }

    private static PGSimpleDataSource adminDataSource() {
        PostgreSQLContainer<?> container = postgresql();
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(container.getJdbcUrl());
        dataSource.setUser(container.getUsername());
        dataSource.setPassword(container.getPassword());
        return dataSource;
    }

    private static PGSimpleDataSource dataSource(String schema) {
        PGSimpleDataSource dataSource = adminDataSource();
        dataSource.setCurrentSchema(schema);
        return dataSource;
    }

    private static void initializeTogether(OutboxManagedDdlInitializer initializer, FixtureBO fixture,
                                           CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("DDL concurrency test did not start");
            }
            initializer.initialize(fixture.physicalSources(), fixture.yaml());
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static boolean tableExists(DataSource dataSource, String table) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT EXISTS (SELECT 1 FROM pg_catalog.pg_class c "
                     + "JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = ? AND c.relname = ?)")) {
            statement.setString(1, SCHEMA);
            statement.setString(2, table);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }

    private static long scalar(DataSource dataSource, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private static void execute(DataSource dataSource, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static void assertCauseContains(Throwable failure, String message) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(message)) {
                return;
            }
        }
        throw new AssertionError("Expected failure cause containing " + message, failure);
    }

    private static String sha256(byte[] value) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }

    private record FixtureBO(OutboxManagedDdlInitializer initializer, Map<String, DataSource> physicalSources,
                             byte[] yaml, PGSimpleDataSource physicalDataSource, ValidationUtils validationUtils,
                             EgonColaShardingProperties shardingProperties,
                             OutboxMpStorageProperties storageProperties, String routeFingerprint) {

        private FixtureBO {
            yaml = yaml.clone();
        }

        @Override
        public byte[] yaml() {
            return yaml.clone();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestDependenciesConfiguration {

        @Bean("egonColaValidationUtils")
        ValidationUtils egonColaValidationUtils() {
            return new ValidationUtils(VALIDATOR_FACTORY.getValidator());
        }

        @Bean("egonColaPostgreDdlRunner")
        EgonColaPostgreDdlRunner egonColaPostgreDdlRunner(ValidationUtils validationUtils) {
            return runner(validationUtils, new PathMatchingResourcePatternResolver());
        }

        @Bean("egonColaShardingTopologyValidator")
        EgonColaShardingTopologyValidator egonColaShardingTopologyValidator(ValidationUtils validationUtils) {
            return new EgonColaShardingTopologyValidator(validationUtils);
        }

        @Bean("egon.cola.component.mybatis-plus.sharding-top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties")
        EgonColaShardingProperties egonColaShardingProperties() {
            return shardingPropertiesWithoutPostgres();
        }

        @Bean("outboxStateMachineObjectMapper")
        ObjectMapper outboxStateMachineObjectMapper() {
            return new ObjectMapper();
        }
    }
}
