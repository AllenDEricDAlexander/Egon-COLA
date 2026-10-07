package top.egon.cola.component.tianshu.admin.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlManifestBO;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlResult;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlTargetBO;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlTargetBO.RoleEnum;
import top.egon.cola.component.common.mybatis.ddl.EgonColaPostgreDdlRunner;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaPhysicalDataSourceFactory;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingTopologyValidator;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingYamlLoader;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaShardingStrategy;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaShardingStrategyFactory;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaSingleTableShardingStrategy;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaStandardTenantIdShardingStrategy;
import top.egon.cola.component.common.id.autoconfigure.IdGeneratorProperties;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.outbox.autoconfigure.OutboxMpStorageProperties;
import top.egon.cola.component.outbox.migration.OutboxManagedDdlInitializer;
import top.egon.cola.component.outbox.persistence.OutboxSchemaMetadataValidator;
import top.egon.cola.component.tianshu.admin.config.DdcDatabaseReadyValidator;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Explicit standalone entry point for validating or installing the Tianshu and Outbox schemas. */
@Slf4j
public final class DdcPersistenceMigrationCli {

    private static final String PERSISTENCE_RESOURCE = "egon-tianshu-persistence.yml";
    private static final String TIANSHU_MANIFEST_RESOURCE = "db/egon-mp/repository-manifest.json";
    private static final String OUTBOX_MANIFEST_RESOURCE = "db/egon-outbox-mp/manifest.json";

    private DdcPersistenceMigrationCli() {
    }

    public static void main(String[] args) throws IOException {
        DdcMigrationCliCommand command = parseInvocation(args);
        if (command.sourceConfig() == null) {
            execute(command.mode(), loadEnvironment(command.targetConfig()));
            return;
        }
        executeMigration(command);
    }

    public static Mode parseMode(String[] args) {
        return parseInvocation(args).mode();
    }

    private static DdcMigrationCliCommand parseInvocation(String[] args) {
        if (args == null || args.length == 0 || args[0] == null) throw usageFailure();
        Mode mode = switch (args[0]) {
            case "validate" -> Mode.VALIDATE;
            case "initialize" -> Mode.INITIALIZE;
            case "import" -> Mode.IMPORT;
            case "verify" -> Mode.VERIFY;
            default -> throw usageFailure();
        };
        if (args.length == 1) {
            if (mode == Mode.IMPORT || mode == Mode.VERIFY) throw usageFailure();
            return new DdcMigrationCliCommand(mode, null, null, null, null);
        }

        Map<String, Path> options = parseOptions(args);
        if (mode == Mode.INITIALIZE || (mode == Mode.VALIDATE && options.size() == 1
                && options.containsKey("--target-config"))) {
            if (options.size() != 1 || !options.containsKey("--target-config")) throw usageFailure();
            return new DdcMigrationCliCommand(mode, null, options.get("--target-config"), null, null);
        }
        if (!options.keySet().equals(Set.of(
                "--source-config", "--target-config", "--mapping", "--report-dir"))) {
            throw usageFailure();
        }
        Path sourceConfig = options.get("--source-config");
        Path targetConfig = options.get("--target-config");
        Path mapping = options.get("--mapping");
        if (sourceConfig.equals(targetConfig) || sourceConfig.equals(mapping) || targetConfig.equals(mapping)) {
            throw usageFailure();
        }
        return new DdcMigrationCliCommand(mode, sourceConfig, targetConfig, mapping, options.get("--report-dir"));
    }

    private static Map<String, Path> parseOptions(String[] args) {
        Map<String, Path> options = new LinkedHashMap<>();
        for (int index = 1; index < args.length; index += 2) {
            if (index + 1 >= args.length || args[index] == null || args[index + 1] == null
                    || !Set.of("--source-config", "--target-config", "--mapping", "--report-dir")
                    .contains(args[index])) throw usageFailure();
            Path path;
            try {
                path = Path.of(args[index + 1]);
            } catch (RuntimeException failure) {
                throw usageFailure();
            }
            if (!path.isAbsolute() || path.normalize().toString().isBlank()
                    || options.putIfAbsent(args[index], path.normalize()) != null) throw usageFailure();
        }
        return options;
    }

    private static void execute(Mode mode) throws IOException {
        execute(mode, loadEnvironment(null));
    }

    private static String execute(Mode mode, StandardEnvironment environment) throws IOException {
        if (mode == Mode.IMPORT || mode == Mode.VERIFY) throw usageFailure();
        EgonColaShardingProperties shardingProperties = bind(environment,
                EgonColaShardingProperties.PREFIX, EgonColaShardingProperties.class);
        EgonColaMybatisPlusProperties mybatisProperties = bind(environment,
                EgonColaMybatisPlusProperties.PREFIX, EgonColaMybatisPlusProperties.class);
        OutboxMpStorageProperties storageProperties = bind(environment,
                OutboxMpStorageProperties.PREFIX, OutboxMpStorageProperties.class);

        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            ValidationUtils validationUtils = new ValidationUtils(validatorFactory.getValidator());
            validationUtils.validate(shardingProperties);
            validationUtils.validate(mybatisProperties);
            validationUtils.validate(storageProperties);

            ResourcePatternResolver resources = new PathMatchingResourcePatternResolver();
            ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
            EgonColaShardingTopologyValidator topologyValidator =
                    new EgonColaShardingTopologyValidator(validationUtils);
            EgonColaShardingStrategyFactory strategyFactory = new EgonColaShardingStrategyFactory(
                    validationUtils, Map.of(
                    "STANDARD_TENANT_ID",
                    new EgonColaStandardTenantIdShardingStrategy(),
                    "SINGLE",
                    new EgonColaSingleTableShardingStrategy()));
            EgonColaShardingYamlLoader yamlLoader = new EgonColaShardingYamlLoader(
                    strategyFactory, new DefaultResourceLoader(), environment);
            byte[] yaml = yamlLoader.load(shardingProperties);
            var topology = topologyValidator.validate(shardingProperties, yaml);
            EgonColaDdlManifestBO tianshuManifest = loadManifest(
                    resources, objectMapper, validationUtils, TIANSHU_MANIFEST_RESOURCE, "tianshu");
            EgonColaPostgreDdlRunner ddlRunner = new EgonColaPostgreDdlRunner(
                    validationUtils, resources, Clock.systemUTC(), mybatisProperties.getDdl().getLockTimeout(),
                    mybatisProperties.getDdl().getStatementTimeout());
            EgonColaPhysicalDataSourceFactory physicalFactory = new EgonColaPhysicalDataSourceFactory();
            Map<String, DataSource> physicalSources = physicalFactory.create(shardingProperties);
            try {
                OutboxManagedDdlInitializer outboxInitializer = new OutboxManagedDdlInitializer(
                        ddlRunner, topologyValidator, validationUtils, shardingProperties,
                        storageProperties.setAutoInitializeDdl(false), objectMapper);
                outboxInitializer.initialize(physicalSources, yaml);

                if (mode == Mode.INITIALIZE) {
                    EgonColaShardingProperties.PhysicalDataSourceProperties primary =
                            shardingProperties.getDataSources().getFirst();
                    DataSource primaryDataSource = physicalSources.get(primary.name());
                    List<EgonColaDdlResult> results = ddlRunner.run(List.of(new EgonColaDdlTargetBO(
                            primary.name(), "public", RoleEnum.MASTER_DATA, primaryDataSource,
                            tianshuManifest, topology.fingerprint())));
                    validateTianshuResults(results, tianshuManifest, primary.name());
                    storageProperties.setAutoInitializeDdl(true);
                    outboxInitializer.initialize(physicalSources, yaml);
                }

                DdcDatabaseReadyValidator.validateTianshuSchema(
                        physicalSources.get(shardingProperties.getDataSources().getFirst().name()),
                        topology.fingerprint(), tianshuManifest);
                OutboxSchemaMetadataValidator outboxSchemaValidator = new OutboxSchemaMetadataValidator(
                        outboxInitializer, topology.fingerprint(), storageProperties, objectMapper, validationUtils);
                outboxSchemaValidator.validate();
                log.info("Tianshu persistence {} completed: routeFingerprint={}", mode, topology.fingerprint());
                return topology.fingerprint();
            } finally {
                physicalFactory.close(physicalSources.values());
            }
        }
    }

    private static void executeMigration(DdcMigrationCliCommand command) throws IOException {
        validateProtectedFile(command.sourceConfig(), "source-config");
        validateProtectedFile(command.targetConfig(), "target-config");
        validateProtectedFile(command.mapping(), "mapping");

        StandardEnvironment sourceEnvironment = loadEnvironment(command.sourceConfig());
        StandardEnvironment targetEnvironment = loadEnvironment(command.targetConfig());
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        JsonNode mapping = readSignedMapping(command.mapping(), targetEnvironment, objectMapper);
        String jobId = requiredText(mapping, "jobId");
        if (!jobId.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}") || jobId.contains("..")) {
            throw new IllegalStateException("TIANSHU_MIGRATION_JOB_ID_INVALID");
        }
        String targetRouteFingerprint = execute(Mode.VALIDATE, targetEnvironment);
        if (!targetRouteFingerprint.equals(requiredText(mapping, "targetRouteFingerprint"))) {
            throw new IllegalStateException("TIANSHU_MIGRATION_TARGET_ROUTE_MISMATCH");
        }
        Path reportDirectory = prepareReportDirectory(command.reportDir(), jobId);

        EgonColaShardingProperties sourceProperties = bind(sourceEnvironment,
                EgonColaShardingProperties.PREFIX, EgonColaShardingProperties.class);
        EgonColaShardingProperties targetProperties = bind(targetEnvironment,
                EgonColaShardingProperties.PREFIX, EgonColaShardingProperties.class);
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            new ValidationUtils(validatorFactory.getValidator()).validate(sourceProperties);
        }
        requireSinglePrimary(sourceProperties, "SOURCE");
        requireSinglePrimary(targetProperties, "TARGET");

        IdGeneratorProperties idProperties = Binder.get(targetEnvironment)
                .bind(IdGeneratorProperties.PREFIX, Bindable.of(IdGeneratorProperties.class))
                .orElseGet(IdGeneratorProperties::new);
        if (!idProperties.isEnabled() || idProperties.getMachineId() == null
                || idProperties.getMachineId() < 0 || idProperties.getMachineId() > 1023
                || idProperties.getMaxClockBackward() == null || idProperties.getMaxClockBackward().isNegative()) {
            throw new IllegalStateException("TIANSHU_MIGRATION_MACHINE_ID_REQUIRED");
        }
        if (command.mode() == Mode.IMPORT) {
            SnowflakeIdGenerator.initialize(idProperties.getMachineId(), idProperties.getMaxClockBackward());
        }

        EgonColaPhysicalDataSourceFactory physicalFactory = new EgonColaPhysicalDataSourceFactory();
        Map<String, DataSource> sourceDataSources = physicalFactory.create(sourceProperties);
        Map<String, DataSource> targetDataSources = Map.of();
        try {
            targetDataSources = physicalFactory.create(targetProperties);
            DataSource sourceDataSource = onlyPrimaryDataSource(sourceProperties, sourceDataSources, "SOURCE");
            DataSource targetDataSource = onlyPrimaryDataSource(targetProperties, targetDataSources, "TARGET");
            DdcLegacyImportService importService = new DdcLegacyImportService();
            DdcLegacyTableStrategy tableStrategy = new DdcLegacyTableStrategy();
            DdcMigrationReportResult result;
            try {
                try (var sourceConnection = sourceDataSource.getConnection();
                     var targetConnection = targetDataSource.getConnection()) {
                    sourceConnection.setReadOnly(true);
                    sourceConnection.setTransactionIsolation(java.sql.Connection.TRANSACTION_REPEATABLE_READ);
                    sourceConnection.setAutoCommit(false);
                    if (command.mode() != Mode.IMPORT) targetConnection.setReadOnly(true);
                    if (sameDatabase(sourceConnection, targetConnection)) {
                        result = databaseFailureReport(command.mode(), jobId, targetRouteFingerprint,
                                "SOURCE_TARGET_DATABASE_OVERLAP");
                    } else {
                        result = switch (command.mode()) {
                            case VALIDATE -> importService.validateDatabase(
                                    sourceConnection, targetConnection, mapping, targetRouteFingerprint, tableStrategy);
                            case IMPORT -> importService.importDatabase(
                                    sourceConnection, targetConnection, mapping, targetRouteFingerprint, jobId,
                                    tableStrategy, SnowflakeIdGenerator::nextLongId, Clock.systemUTC(), reportDirectory);
                            case VERIFY -> importService.verifyDatabase(
                                    sourceConnection, targetConnection, mapping, targetRouteFingerprint, tableStrategy);
                            case INITIALIZE -> throw usageFailure();
                        };
                    }
                }
            } catch (java.sql.SQLException failure) {
                log.error("Tianshu migration database connection failed: mode={} sqlState={}",
                        command.mode(), failure.getSQLState());
                result = databaseFailureReport(command.mode(), jobId, targetRouteFingerprint,
                        "MIGRATION_CONNECTION_FAILURE");
            }
            writeReport(reportDirectory, command.mode(), jobId, objectMapper, result);
            if (!"SUCCESS".equals(result.getStatus())) {
                throw new IllegalStateException("TIANSHU_MIGRATION_" + command.mode() + "_FAILED: "
                        + result.getErrorCode());
            }
            log.info("Tianshu migration {} completed: jobId={} routeFingerprint={}",
                    command.mode(), jobId, targetRouteFingerprint);
        } finally {
            physicalFactory.close(sourceDataSources.values());
            physicalFactory.close(targetDataSources.values());
        }
    }

    static boolean sameDatabase(Connection source, Connection target) throws SQLException {
        return databaseIdentity(source).equals(databaseIdentity(target));
    }

    private static DdcPostgresDatabaseIdentity databaseIdentity(Connection connection) throws SQLException {
        String sql = "SELECT current_database(), inet_server_addr()::text, inet_server_port()";
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            if (!result.next()) throw new SQLException("TIANSHU_MIGRATION_DATABASE_IDENTITY_UNAVAILABLE");
            String database = result.getString(1);
            String serverAddress = result.getString(2);
            int serverPort = result.getInt(3);
            if (database == null || database.isBlank() || serverAddress == null || serverAddress.isBlank()
                    || result.wasNull() || serverPort <= 0) {
                throw new SQLException("TIANSHU_MIGRATION_DATABASE_IDENTITY_UNAVAILABLE");
            }
            return new DdcPostgresDatabaseIdentity(database, serverAddress, serverPort);
        }
    }

    private static DdcMigrationReportResult databaseFailureReport(Mode mode, String jobId,
                                                                  String targetRouteFingerprint,
                                                                  String errorCode) {
        return DdcMigrationReportResult.builder().mode(mode.name()).jobId(jobId).status("FAILED")
                .targetRouteFingerprint(targetRouteFingerprint).differenceCount(1)
                .errorCode(errorCode).errorStage("CONNECTION").build();
    }

    private static JsonNode readSignedMapping(Path mappingFile, StandardEnvironment targetEnvironment,
                                              ObjectMapper objectMapper) throws IOException {
        JsonNode envelope = objectMapper.readTree(mappingFile.toFile());
        String keyId = requiredText(envelope, "keyId");
        if (!keyId.matches("[A-Za-z0-9._-]{1,64}")) {
            throw new IllegalStateException("TIANSHU_MIGRATION_MAPPING_KEY_ID_INVALID");
        }
        String publicKey = targetEnvironment.getProperty(
                "egon.cola.component.tianshu.migration.trusted-mapping-public-keys." + keyId);
        if (publicKey == null || publicKey.isBlank()) {
            throw new IllegalStateException("TIANSHU_MIGRATION_MAPPING_KEY_UNTRUSTED");
        }
        byte[] payload;
        byte[] signature;
        try {
            payload = Base64.getDecoder().decode(requiredText(envelope, "payload"));
            signature = Base64.getDecoder().decode(requiredText(envelope, "signature"));
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException("TIANSHU_MIGRATION_MAPPING_SIGNATURE_INVALID", failure);
        }
        if (payload.length == 0 || payload.length > 4 * 1024 * 1024
                || !DdcLegacyImportService.verifyMappingSignature(payload, signature, publicKey)) {
            throw new IllegalStateException("TIANSHU_MIGRATION_MAPPING_SIGNATURE_INVALID");
        }
        JsonNode mapping = objectMapper.readTree(payload);
        if (!mapping.isObject()) throw new IllegalStateException("TIANSHU_MIGRATION_MAPPING_INVALID");
        return mapping;
    }

    private static String requiredText(JsonNode object, String field) {
        JsonNode value = object == null ? null : object.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalStateException("TIANSHU_MIGRATION_FIELD_REQUIRED: " + field);
        }
        return value.asText();
    }

    private static void validateProtectedFile(Path path, String name) {
        if (path == null || Files.isSymbolicLink(path)
                || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(path)) {
            throw new IllegalStateException("TIANSHU_MIGRATION_PROTECTED_FILE_REQUIRED: " + name);
        }
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS);
            if (permissions.contains(PosixFilePermission.GROUP_READ)
                    || permissions.contains(PosixFilePermission.GROUP_WRITE)
                    || permissions.contains(PosixFilePermission.GROUP_EXECUTE)
                    || permissions.contains(PosixFilePermission.OTHERS_READ)
                    || permissions.contains(PosixFilePermission.OTHERS_WRITE)
                    || permissions.contains(PosixFilePermission.OTHERS_EXECUTE)) {
                throw new IllegalStateException("TIANSHU_MIGRATION_FILE_PERMISSIONS_UNSAFE: " + name);
            }
        } catch (IOException | UnsupportedOperationException failure) {
            throw new IllegalStateException("TIANSHU_MIGRATION_FILE_PERMISSIONS_UNVERIFIED: " + name, failure);
        }
    }

    private static Path prepareReportDirectory(Path directory, String jobId) throws IOException {
        if (directory == null || Files.isSymbolicLink(directory)) {
            throw new IllegalStateException("TIANSHU_MIGRATION_REPORT_DIRECTORY_INVALID");
        }
        boolean existed = Files.exists(directory, LinkOption.NOFOLLOW_LINKS);
        Files.createDirectories(directory);
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("TIANSHU_MIGRATION_REPORT_DIRECTORY_INVALID");
        }
        try {
            Set<PosixFilePermission> ownerOnly = Set.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);
            if (existed) {
                Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(directory,
                        LinkOption.NOFOLLOW_LINKS);
                if (permissions.contains(PosixFilePermission.GROUP_READ)
                        || permissions.contains(PosixFilePermission.GROUP_WRITE)
                        || permissions.contains(PosixFilePermission.GROUP_EXECUTE)
                        || permissions.contains(PosixFilePermission.OTHERS_READ)
                        || permissions.contains(PosixFilePermission.OTHERS_WRITE)
                        || permissions.contains(PosixFilePermission.OTHERS_EXECUTE)) {
                    throw new IllegalStateException("TIANSHU_MIGRATION_REPORT_PERMISSIONS_UNSAFE");
                }
            } else {
                Files.setPosixFilePermissions(directory, ownerOnly);
            }
        } catch (UnsupportedOperationException failure) {
            throw new IllegalStateException("TIANSHU_MIGRATION_REPORT_PERMISSIONS_UNVERIFIED", failure);
        }
        try (var entries = Files.list(directory)) {
            if (entries.anyMatch(path -> !path.getFileName().toString().startsWith(jobId + "."))) {
                throw new IllegalStateException("TIANSHU_MIGRATION_REPORT_DIRECTORY_NOT_EMPTY");
            }
        }
        return directory;
    }

    private static void writeReport(Path directory, Mode mode, String jobId, ObjectMapper objectMapper,
                                    DdcMigrationReportResult report) throws IOException {
        Path target = directory.resolve(jobId + "." + mode.name().toLowerCase() + ".json");
        Path temporary = Files.createTempFile(directory, jobId + ".", ".tmp");
        try {
            objectMapper.writeValue(temporary.toFile(), report);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException failure) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void requireSinglePrimary(EgonColaShardingProperties properties, String label) {
        if (properties.getDataSources() == null || properties.getDataSources().size() != 1
                || properties.getDataSources().getFirst().role() != EgonColaShardingProperties.DataSourceRoleEnum.PRIMARY) {
            throw new IllegalStateException("TIANSHU_MIGRATION_" + label + "_PRIMARY_REQUIRED");
        }
    }

    private static DataSource onlyPrimaryDataSource(EgonColaShardingProperties properties,
                                                     Map<String, DataSource> dataSources, String label) {
        String name = properties.getDataSources().getFirst().name();
        DataSource dataSource = dataSources.get(name);
        if (dataSource == null) throw new IllegalStateException("TIANSHU_MIGRATION_" + label + "_DATASOURCE_REQUIRED");
        return dataSource;
    }

    private static StandardEnvironment loadEnvironment(Path configFile) throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        String resourceName = configFile == null ? PERSISTENCE_RESOURCE : configFile.toString();
        var resource = configFile == null ? new ClassPathResource(PERSISTENCE_RESOURCE)
                : new FileSystemResource(configFile);
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(resourceName, resource);
        sources.forEach(environment.getPropertySources()::addLast);
        return environment;
    }

    private static <T> T bind(StandardEnvironment environment, String prefix, Class<T> type) {
        return Binder.get(environment).bind(prefix, Bindable.of(type))
                .orElseThrow(() -> new IllegalStateException("TIANSHU_CONFIGURATION_REQUIRED: " + prefix));
    }

    private static EgonColaDdlManifestBO loadManifest(ResourcePatternResolver resources, ObjectMapper objectMapper,
                                                      ValidationUtils validationUtils, String path, String family) {
        try {
            var matches = resources.getResources("classpath*:" + path);
            if (matches.length != 1) {
                throw new IllegalStateException("TIANSHU_DDL_MANIFEST_RESOURCE_AMBIGUOUS: " + path);
            }
            try (var input = matches[0].getInputStream()) {
                EgonColaDdlManifestBO manifest = validationUtils.validate(
                        objectMapper.readValue(input, EgonColaDdlManifestBO.class));
                if (!family.equals(manifest.family())) {
                    throw new IllegalStateException("TIANSHU_DDL_MANIFEST_FAMILY_INVALID: " + family);
                }
                return manifest;
            }
        } catch (IOException failure) {
            throw new IllegalStateException("TIANSHU_DDL_MANIFEST_UNREADABLE: " + path, failure);
        }
    }

    private static void validateTianshuResults(List<EgonColaDdlResult> results,
                                               EgonColaDdlManifestBO manifest, String primary) {
        if (results == null || results.size() != manifest.scripts().size()) {
            throw new IllegalStateException("TIANSHU_DDL_RESULT_INCOMPLETE");
        }
        for (int index = 0; index < results.size(); index++) {
            EgonColaDdlResult result = results.get(index);
            EgonColaDdlManifestBO.ScriptBO script = manifest.scripts().get(index);
            if (!primary.equals(result.alias()) || !"public".equals(result.schema())
                    || !script.version().equals(result.version()) || !script.sha256().equals(result.checksum())) {
                throw new IllegalStateException("TIANSHU_DDL_RESULT_MISMATCH");
            }
        }
    }

    private static IllegalArgumentException usageFailure() {
        return new IllegalArgumentException("USAGE: DdcPersistenceMigrationCli <validate|initialize|import|verify> "
                + "[--source-config <file> --target-config <file> --mapping <file> --report-dir <directory>]");
    }

    public enum Mode {
        VALIDATE,
        INITIALIZE,
        IMPORT,
        VERIFY
    }

    private record DdcMigrationCliCommand(Mode mode, Path sourceConfig, Path targetConfig,
                                          Path mapping, Path reportDir) {
    }

    private record DdcPostgresDatabaseIdentity(String database, String serverAddress, int serverPort) {
    }
}
