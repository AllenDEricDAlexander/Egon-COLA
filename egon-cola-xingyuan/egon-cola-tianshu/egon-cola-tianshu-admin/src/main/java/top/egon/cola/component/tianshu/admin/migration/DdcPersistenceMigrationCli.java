package top.egon.cola.component.tianshu.admin.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import top.egon.cola.component.outbox.autoconfigure.OutboxMpStorageProperties;
import top.egon.cola.component.outbox.migration.OutboxManagedDdlInitializer;
import top.egon.cola.component.outbox.persistence.OutboxSchemaMetadataValidator;
import top.egon.cola.component.tianshu.admin.config.DdcDatabaseReadyValidator;

import javax.sql.DataSource;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Map;

/** Explicit standalone entry point for validating or installing the Tianshu and Outbox schemas. */
@Slf4j
public final class DdcPersistenceMigrationCli {

    private static final String PERSISTENCE_RESOURCE = "egon-tianshu-persistence.yml";
    private static final String TIANSHU_MANIFEST_RESOURCE = "db/egon-mp/repository-manifest.json";
    private static final String OUTBOX_MANIFEST_RESOURCE = "db/egon-outbox-mp/manifest.json";

    private DdcPersistenceMigrationCli() {
    }

    public static void main(String[] args) throws IOException {
        execute(parseMode(args));
    }

    public static Mode parseMode(String[] args) {
        if (args == null || args.length != 1 || args[0] == null) {
            throw usageFailure();
        }
        return switch (args[0]) {
            case "validate" -> Mode.VALIDATE;
            case "initialize" -> Mode.INITIALIZE;
            default -> throw usageFailure();
        };
    }

    private static void execute(Mode mode) throws IOException {
        StandardEnvironment environment = loadEnvironment();
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
            } finally {
                physicalFactory.close(physicalSources.values());
            }
        }
    }

    private static StandardEnvironment loadEnvironment() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(
                PERSISTENCE_RESOURCE, new ClassPathResource(PERSISTENCE_RESOURCE));
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
        return new IllegalArgumentException("USAGE: DdcPersistenceMigrationCli <validate|initialize>");
    }

    public enum Mode {
        VALIDATE,
        INITIALIZE
    }
}
