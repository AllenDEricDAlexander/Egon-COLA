package top.egon.cola.component.tianshu.admin.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.nio.channels.FileChannel;
import java.nio.file.StandardCopyOption;
import org.slf4j.MDC;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.id.autoconfigure.IdGeneratorAutoConfiguration;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlManifestBO;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlTargetBO;
import top.egon.cola.component.common.mybatis.ddl.EgonColaPostgreDdlRunner;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaTwoLevelRouteStrategy;
import top.egon.cola.component.common.mybatis.routing.EgonColaWriteTargetResolver;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingTopologyValidator;
import top.egon.cola.component.common.mybatis.sharding.resolver.EgonColaShardingWriteTargetResolver;
import top.egon.cola.component.tianshu.admin.config.DdcDatabaseReadyValidator;
import top.egon.cola.component.tianshu.admin.model.bo.DdcBizBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAppBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcBizConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcBizResultConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAppResultConverter;
import top.egon.cola.component.tianshu.admin.model.vo.DdcBizResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAppResult;
import top.egon.cola.component.tianshu.admin.repository.DdcBizRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAppRepository;
import top.egon.cola.component.tianshu.admin.repository.impl.DdcBizRepositoryImpl;
import top.egon.cola.component.tianshu.admin.repository.impl.DdcAppRepositoryImpl;
import top.egon.cola.component.tianshu.admin.repository.impl.DdcEnvRepositoryImpl;
import top.egon.cola.component.tianshu.admin.repository.impl.DdcNamespaceRepositoryImpl;
import top.egon.cola.component.tianshu.admin.repository.impl.DdcNamespaceEnvAppBindingRepositoryImpl;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcBizService;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcAppService;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcScopeGate;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcNamespaceEnvAppBindingService;
import static org.junit.jupiter.api.Assertions.*;

/** Tianshu-owned test entrance; ordinary tests never open a database or listener. */
public class IsolatedTianshuCatalogProvisioningIT {
    @TempDir Path temporaryRepository;
    @BeforeEach void canonicalRepository() throws IOException { temporaryRepository = temporaryRepository.toRealPath(); }
    public interface Prepare { }
    public interface Provision { }
    public interface Runtime { }

    static final Map<String, String> DATABASE_NAMES = Map.of(
            "idp", "xingyuan_it_idp_20261011", "rbac", "xingyuan_it_rbac_20261011",
            "tianshu", "xingyuan_it_tianshu_20261011", "yuheng", "xingyuan_it_yuheng_20261011",
            "llm", "xingyuan_it_llm_20261011", "light", "xingyuan_it_light_20261011",
            "web", "xingyuan_it_web_20261011", "service", "xingyuan_it_service_20261011",
            "agent", "xingyuan_it_agent_20261011");

    static DataSource guardPhysicalDataSource(DataSource physicalDataSource,
                                                        IsolatedDatabaseReceiptDTO receipt) {
        return new GuardedPhysicalDataSource(physicalDataSource, receipt);
    }

    static DataSource physicalDataSource(VerifiedDatabaseInputsBO inputs) throws IOException {
        validateDatabaseReceipt(inputs.getManifest(), inputs.getManifestSha256(), "tianshu", inputs.getReceipt());
        String expectedUrl = "jdbc:postgresql://127.0.0.1:5432/" + DATABASE_NAMES.get("tianshu");
        if (!expectedUrl.equals(inputs.getJdbcUrl())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        char[] credential = readProtectedCharacters(Path.of(inputs.getManifest().getPostgres().getCredentialFile()),
                null, 4096);
        try {
            if (credential.length == 0) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
            }
            var source = new DriverManagerDataSource(
                    expectedUrl + "?connectTimeout=5&socketTimeout=30", inputs.getReceipt().getDatabaseOwner(),
                    new String(credential));
            return guardPhysicalDataSource(source, inputs.getReceipt());
        } finally {
            Arrays.fill(credential, '\0');
        }
    }

    static char[] readProtectedCharacters(Path file, Path taskRoot, int maximumBytes) throws IOException {
        byte[] bytes = readProtectedBytes(file, taskRoot, maximumBytes);
        CharBuffer decoded = null;
        try {
            decoded = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes));
            int length = decoded.remaining();
            // A file may end with one line terminator; spaces and embedded newlines remain part of the credential.
            if (length > 0 && decoded.get(length - 1) == '\n') {
                length--;
                if (length > 0 && decoded.get(length - 1) == '\r') {
                    length--;
                }
            }
            char[] characters = new char[length];
            decoded.get(characters, 0, length);
            return characters;
        } catch (CharacterCodingException invalidEncoding) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        } finally {
            Arrays.fill(bytes, (byte) 0);
            if (decoded != null) {
                decoded.clear();
                for (int index = 0; index < decoded.capacity(); index++) {
                    decoded.put(index, '\0');
                }
            }
        }
    }

    @lombok.RequiredArgsConstructor
    public static class GuardedPhysicalDataSource extends AbstractDataSource {
        private final DataSource delegate;
        private final IsolatedDatabaseReceiptDTO receipt;


        // ShardingSphere inspects JavaBean connection properties before using the guarded connections.
        public String getUrl() {
            return delegate instanceof DriverManagerDataSource source ? source.getUrl() : null;
        }

        public String getUsername() {
            return delegate instanceof DriverManagerDataSource source ? source.getUsername() : null;
        }

        public String getPassword() {
            return delegate instanceof DriverManagerDataSource source ? source.getPassword() : null;
        }

        @Override
        public Connection getConnection() throws SQLException {
            var connection = delegate.getConnection();
            try {
                verifyDatabaseConnection(connection, receipt);
                return connection;
            } catch (SQLException | RuntimeException rejected) {
                try {
                    connection.close();
                } catch (SQLException closeFailure) {
                    rejected.addSuppressed(closeFailure);
                }
                throw rejected;
            }
        }

        @Override
        public Connection getConnection(String username, String password) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
    }

    private static final Map<String, String> PLATFORM_BUSINESSES = Map.of(
            "tianquan-jianshen-admin", "permission", "tianquan-shoubing-admin", "identity",
            "yuheng-admin", "platform", "tianshu-admin", "platform");

    static void validateCatalogRecipe(PlatformCatalogRecipeDTO recipe, IsolatedIntegrationManifestDTO manifest) {
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            if (recipe == null || !validators.getValidator().validate(recipe).isEmpty()) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
            }
        }
        if (!manifest.getRunId().equals(recipe.getRunId())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        Set<String> businesses = new HashSet<>();
        for (var business : recipe.getBusinesses()) {
            if (!businesses.add(business.getBizCode()) || !Set.of("identity", "permission", "platform").contains(business.getBizCode())) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
            }
        }
        Set<String> applications = new HashSet<>();
        for (var application : recipe.getApplications()) {
            if (!applications.add(application.getAppCode())
                    || !Objects.equals(PLATFORM_BUSINESSES.get(application.getAppCode()), application.getBizCode())) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
            }
        }
        if (!applications.equals(PLATFORM_BUSINESSES.keySet())) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void provisionPlatformCatalog() {
        try {
            Path repository = Path.of(requiredProperty("xingyuan.isolated.repository-root"));
            Path manifest = Path.of(requiredProperty("xingyuan.isolated.manifest-file"));
            Path receipt = Path.of(requiredProperty("xingyuan.isolated.receipt-file"));
            var inputs = readVerifiedDatabaseInputs(manifest, receipt, repository, "tianshu");
            var prepared = readCatalogInputs(inputs);
            Path root = Path.of(inputs.getManifest().getRuntimeRoot());
            requirePrivateDirectory(root.resolve("evidence"));
            Path lockPath = root.resolve("evidence/tianshu-platform-catalog.lock");
            try (var lockFile = FileChannel.open(lockPath, Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS),
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))) {
                readProtectedBytes(lockPath, root.resolve("evidence"), 1024);
                try (var lock = lockFile.tryLock()) {
                    if (lock == null) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
                    executeCatalogProvisioning(manifest, receipt, repository, prepared);
                }
            }
        } catch (IOException unavailable) {
            throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE");
        }
    }

    private static void executeCatalogProvisioning(Path manifest, Path receipt, Path repository,
                                                  PreparedCatalogInputsBO prepared) throws IOException {
        var inputs = prepared.getDatabase();
        Path root = Path.of(inputs.getManifest().getRuntimeRoot());
        Path checkpoint = root.resolve("evidence/tianshu-platform-catalog.json");
        String resume = System.getProperty("xingyuan.isolated.resume", "false");
        if (!Set.of("true", "false").contains(resume)) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
        byte[] previous = Files.exists(checkpoint, LinkOption.NOFOLLOW_LINKS)
                ? readProtectedBytes(checkpoint, root.resolve("evidence"), 128 * 1024) : null;
        CatalogProvisioningReceiptDTO prior = previous == null ? null : strictMapper().readValue(previous, CatalogProvisioningReceiptDTO.class);
        if (prior != null && (!"true".equals(resume) || !matchingReceipt(prior, prepared))) {
            throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
        }
        if (!ZoneId.systemDefault().normalized().equals(java.time.ZoneOffset.UTC)) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var pending = CatalogProvisioningReceiptDTO.builder().schemaVersion(1).runId(inputs.getManifest().getRunId())
                .manifestSha256(inputs.getManifestSha256()).databaseOid(inputs.getReceipt().getOid())
                .recipeSha256(prepared.getRecipeSha256()).registrySha256(prepared.getRegistrySha256())
                .tenantId(prepared.getTenantId()).status("RUNNING").recordedAt(Instant.now())
                .businesses(Map.of()).applications(Map.of()).build();
        prepared.setInitializeSchema(prior == null);
        Map<String, String> oldMdc = MDC.getCopyOfContextMap();
        try {
            MDC.put("tenantId", prepared.getTenantId());
            MDC.put("userId", prepared.getActorId());
            try (var context = openCatalogContext(manifest, receipt, repository, prepared)) {
                Runnable guard = context.getBean("isolatedOwnershipGuard", Runnable.class);
                var tx = new TransactionTemplate(context.getBean("transactionManager", PlatformTransactionManager.class));
                var bizService = context.getBean("ddcBizService", DdcBizService.class);
                var appService = context.getBean("ddcAppService", DdcAppService.class);
                var bizRepository = context.getBean("ddcBizRepository", DdcBizRepository.class);
                var appRepository = context.getBean("ddcAppRepository", DdcAppRepository.class);
                var validation = context.getBean("egonColaValidationUtils", ValidationUtils.class);
                if (prior == null) {
                    guard.run();
                    publishCheckpoint(checkpoint, strictMapper().writeValueAsBytes(pending), null);
                    tx.executeWithoutResult(status -> {
                        new JdbcTemplate(context.getBean("dataSource", DataSource.class))
                                .queryForObject("select pg_advisory_xact_lock(?)", Object.class, 0x5449414e5348554cL);
                        for (var business : prepared.getRecipe().getBusinesses()) {
                            if (bizRepository.findByBizCode(business.getBizCode()).isPresent()) {
                                throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
                            }
                        }
                        for (var application : prepared.getRecipe().getApplications()) {
                            if (appRepository.findFirstByAppCodeOrderByBizCodeAsc(application.getAppCode()).isPresent()) {
                                throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
                            }
                        }
                        for (var business : prepared.getRecipe().getBusinesses()) {
                            guard.run();
                            bizService.save(validation.validate(DdcBizBO.builder().bizCode(business.getBizCode())
                                    .bizName(business.getBizName()).description(catalogMarker(prepared)).enabled(true).build(),
                                    EgonColaModelValidationGroups.Insert.class));
                        }
                        for (var application : prepared.getRecipe().getApplications()) {
                            guard.run();
                            appService.save(validation.validate(DdcAppBO.builder().appCode(application.getAppCode())
                                    .appName(application.getAppName()).bizCode(application.getBizCode())
                                    .owner(prepared.getActorId()).description(catalogMarker(prepared)).enabled(true).build(),
                                    EgonColaModelValidationGroups.Insert.class));
                        }
                    });
                }
                // A resumed unknown outcome is read-only: all seven original owner facts must exist.
                guard.run();
                var confirmed = confirmedCatalogReceipt(context, prepared);
                if (prior != null && "COMMITTED".equals(prior.getStatus())
                        && (!prior.getBusinesses().equals(confirmed.getBusinesses()) || !prior.getApplications().equals(confirmed.getApplications()))) {
                    throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
                }
                publishCheckpoint(checkpoint, strictMapper().writeValueAsBytes(confirmed),
                        previous == null ? strictMapper().writeValueAsBytes(pending) : previous);
            } catch (RuntimeException failedOwner) {
                Throwable cause = failedOwner;
                for (int depth = 0; cause != null && depth < 32; depth++, cause = cause.getCause()) {
                    System.err.println("XINGYUAN_FAILURE_TYPE " + cause.getClass().getName());
                    if (cause instanceof org.springframework.beans.factory.BeanCreationException beanFailure) {
                        System.err.println("XINGYUAN_FAILURE_BEAN " + beanFailure.getBeanName());
                    }
                    java.util.Arrays.stream(cause.getStackTrace()).limit(12).forEach(frame -> System.err.println("  at " + frame));
                }
                if (prior == null && !Files.exists(checkpoint, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IllegalStateException("XINGYUAN_DEPENDENCY_UNAVAILABLE");
                }
                // Preserve the pending receipt; neither this invocation nor resume repeats an unknown write.
                throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
            }
        } finally {
            if (oldMdc == null) { MDC.clear(); } else { MDC.setContextMap(oldMdc); }
        }
    }

    private static CatalogProvisioningReceiptDTO confirmedCatalogReceipt(AnnotationConfigApplicationContext context,
                                                                         PreparedCatalogInputsBO prepared) {
        var bizService = context.getBean("ddcBizService", DdcBizService.class);
        var appRepository = context.getBean("ddcAppRepository", DdcAppRepository.class);
        var bizConverter = context.getBean("ddcBizResultConverterImpl", DdcBizResultConverter.class);
        var appConverter = context.getBean("ddcAppResultConverterImpl", DdcAppResultConverter.class);
        var businesses = new LinkedHashMap<String, DdcBizResult>();
        var applications = new LinkedHashMap<String, DdcAppResult>();
        for (var recipe : prepared.getRecipe().getBusinesses()) {
            DdcBizBO row = bizService.findByBizCode(recipe.getBizCode());
            if (!ownedCatalogFact(row.getPublicId(), row.getTenantId(), row.getCreateUserId(), row.getDescription(), row.getEnabled(), prepared)
                    || !recipe.getBizName().equals(row.getBizName())) {
                throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
            }
            businesses.put(recipe.getBizCode(), bizConverter.toTarget(row));
        }
        for (var recipe : prepared.getRecipe().getApplications()) {
            DdcAppBO row = appRepository.findByBizCodeAndAppCode(recipe.getBizCode(), recipe.getAppCode())
                    .orElseThrow(() -> new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"));
            if (!ownedCatalogFact(row.getPublicId(), row.getTenantId(), row.getCreateUserId(), row.getDescription(), row.getEnabled(), prepared)
                    || !recipe.getAppName().equals(row.getAppName()) || !prepared.getActorId().equals(row.getOwner())) {
                throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
            }
            applications.put(recipe.getAppCode(), appConverter.toTarget(row));
        }
        var inputs = prepared.getDatabase();
        return CatalogProvisioningReceiptDTO.builder().schemaVersion(1).runId(inputs.getManifest().getRunId())
                .manifestSha256(inputs.getManifestSha256()).databaseOid(inputs.getReceipt().getOid())
                .recipeSha256(prepared.getRecipeSha256()).registrySha256(prepared.getRegistrySha256())
                .tenantId(prepared.getTenantId()).status("COMMITTED").recordedAt(Instant.now())
                .businesses(Map.copyOf(businesses)).applications(Map.copyOf(applications)).build();
    }

    private static boolean ownedCatalogFact(String publicId, Long tenantId, String actor, String marker, Boolean enabled,
                                            PreparedCatalogInputsBO prepared) {
        return publicId != null && publicId.matches("[1-9][0-9]{0,18}")
                && prepared.getTenantId().equals(String.valueOf(tenantId)) && prepared.getActorId().equals(actor)
                && catalogMarker(prepared).equals(marker) && Boolean.TRUE.equals(enabled);
    }

    private static String catalogMarker(PreparedCatalogInputsBO prepared) {
        return "xingyuan-it:" + prepared.getDatabase().getManifest().getRunId() + ":platform-catalog";
    }

    private static boolean matchingReceipt(CatalogProvisioningReceiptDTO receipt, PreparedCatalogInputsBO prepared) {
        return receipt.getSchemaVersion() == 1 && prepared.getDatabase().getManifest().getRunId().equals(receipt.getRunId())
                && prepared.getDatabase().getManifestSha256().equals(receipt.getManifestSha256())
                && prepared.getDatabase().getReceipt().getOid() == receipt.getDatabaseOid()
                && prepared.getRecipeSha256().equals(receipt.getRecipeSha256())
                && prepared.getRegistrySha256().equals(receipt.getRegistrySha256())
                && prepared.getTenantId().equals(receipt.getTenantId())
                && receipt.getStatus() != null && Set.of("RUNNING", "COMMITTED").contains(receipt.getStatus());
    }

    static PreparedCatalogInputsBO readCatalogInputs(VerifiedDatabaseInputsBO inputs) throws IOException {
        Path root = Path.of(inputs.getManifest().getRuntimeRoot());
        byte[] recipeBytes = boundPolicy(inputs, root.resolve("policies/platform-catalog.json"), 128 * 1024);
        byte[] yaml = boundPolicy(inputs, root.resolve("profiles/tianshu-sharding.yml"), 2 * 1024 * 1024);
        var recipe = strictMapper().readValue(recipeBytes, PlatformCatalogRecipeDTO.class);
        validateCatalogRecipe(recipe, inputs.getManifest());
        requirePrivateDirectory(root.resolve("secrets"));
        byte[] registryBytes = readProtectedBytes(root.resolve("secrets/runtime-registry.json"), root.resolve("secrets"), 2 * 1024 * 1024);
        var registry = strictMapper().readTree(registryBytes);
        var identityReceipt = strictMapper().readTree(readProtectedBytes(root.resolve("evidence/identity.json"), root.resolve("evidence"), 128 * 1024));
        if (!registry.path("schemaVersion").isIntegralNumber() || registry.path("schemaVersion").intValue() != 1
                || !inputs.getManifest().getRunId().equals(registry.path("runId").asText())
                || !inputs.getManifestSha256().equals(registry.path("manifestSha256").asText())
                || !inputs.getManifest().getRunId().equals(identityReceipt.path("runId").asText())
                || !inputs.getManifestSha256().equals(identityReceipt.path("manifestSha256").asText())
                || !"CLI-002".equals(identityReceipt.path("operationId").asText())
                || !"COMMITTED".equals(identityReceipt.path("status").asText())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var tenantNode = registry.path("tenantIds").path(recipe.getTenantKey());
        var actorNode = registry.path("subjects").path(recipe.getActorKey());
        if (!tenantNode.isTextual() || !tenantNode.textValue().matches("[1-9][0-9]{0,18}")
                || tenantNode.textValue().equals(inputs.getManifest().getPlatformStoragePartitionId())
                || !actorNode.isTextual() || actorNode.textValue().isBlank() || actorNode.textValue().length() > 128) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
        Long.parseLong(tenantNode.textValue());
        return PreparedCatalogInputsBO.builder().database(inputs).recipe(recipe).recipeSha256(sha256(recipeBytes))
                .registrySha256(sha256(registryBytes)).tenantId(tenantNode.textValue()).actorId(actorNode.textValue()).yaml(yaml).build();
    }

    private static byte[] boundPolicy(VerifiedDatabaseInputsBO inputs, Path file, int maximum) throws IOException {
        Path root = Path.of(inputs.getManifest().getRuntimeRoot());
        requirePrivateDirectory(file.getParent());
        var references = inputs.getManifest().getPolicyFiles().stream().filter(ref -> file.toString().equals(ref.getPath())).toList();
        if (references.size() != 1) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
        byte[] data = readProtectedBytes(file, root, maximum);
        if (!sha256(data).equals(references.getFirst().getSha256())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        return data;
    }

    private static String sha256(byte[] data) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }
        catch (NoSuchAlgorithmException unavailable) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"); }
    }

    static AnnotationConfigApplicationContext openCatalogContext(Path manifest, Path receipt, Path repository,
                                                                 PreparedCatalogInputsBO prepared) throws IOException {
        if (!"true".equals(System.getenv("XINGYUAN_ISOLATED_INTEGRATION"))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var inputs = prepared.getDatabase();
        DataSource physical = physicalDataSource(inputs);
        Runnable guard = () -> {
            try {
                var current = readVerifiedDatabaseInputs(manifest, receipt, repository, "tianshu");
                var currentCatalog = readCatalogInputs(current);
                if (!inputs.equals(current) || !prepared.getRecipeSha256().equals(currentCatalog.getRecipeSha256())
                        || !prepared.getRegistrySha256().equals(currentCatalog.getRegistrySha256())) {
                    throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                }
                try (var connection = physical.getConnection()) { verifyDatabaseConnection(connection, inputs.getReceipt()); }
            } catch (IOException | SQLException unavailable) {
                throw new IllegalStateException("XINGYUAN_DEPENDENCY_UNAVAILABLE");
            }
        };
        guard.run();
        var context = new AnnotationConfigApplicationContext();
        context.getBeanFactory().setConversionService(org.springframework.boot.convert.ApplicationConversionService.getSharedInstance());
        context.setBeanNameGenerator(new org.springframework.context.annotation.AnnotationBeanNameGenerator());
        context.getDefaultListableBeanFactory().setAllowBeanDefinitionOverriding(false);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("isolatedTianshu", Map.of(
                "egon.cola.component.id.machine-id", "713",
                "egon.cola.component.mybatis-plus.ddl.enabled", "false",
                "mybatis-plus.mapper-locations", "classpath*:mybatis/mapper/tianshu/**/*.xml")));
        context.registerBean("isolatedVerifiedDatabaseInputs", VerifiedDatabaseInputsBO.class, () -> inputs);
        context.registerBean("isolatedPreparedCatalogInputs", PreparedCatalogInputsBO.class, () -> prepared);
        context.registerBean("isolatedPhysicalDataSource", DataSource.class, () -> physical);
        context.registerBean("isolatedOwnershipGuard", Runnable.class, () -> guard);
        context.register(TianshuPersistenceConfiguration.class);
        try { context.refresh(); return context; }
        catch (RuntimeException rejected) { context.getDefaultListableBeanFactory().destroySingletons(); context.close(); throw rejected; }
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
        return value;
    }

    private static void publishCheckpoint(Path path, byte[] contents, byte[] expected) throws IOException {
        requirePrivateDirectory(path.getParent());
        if (expected == null ? Files.exists(path, LinkOption.NOFOLLOW_LINKS)
                : !MessageDigest.isEqual(expected, readProtectedBytes(path, path.getParent(), 128 * 1024))) {
            throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
        }
        Path temporary = Files.createTempFile(path.getParent(), ".catalog-", ".json",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        try {
            try (var output = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(contents);
                while (buffer.hasRemaining()) { output.write(buffer); }
                output.force(true);
            }
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            try (var directory = FileChannel.open(path.getParent(), StandardOpenOption.READ)) { directory.force(true); }
        } finally { Files.deleteIfExists(temporary); }
    }

    @TestConfiguration(value = "isolatedTianshuPersistenceConfiguration", proxyBeanMethods = false)
    @ImportAutoConfiguration({ValidationAutoConfiguration.class, IdGeneratorAutoConfiguration.class,
            EgonColaMybatisPlusAutoConfiguration.class, MybatisPlusAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class, TransactionAutoConfiguration.class})
    @MapperScan(basePackages = "top.egon.cola.component.tianshu.admin.dao")
    @ComponentScan(basePackageClasses = DdcBizConverter.class, useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = org.springframework.stereotype.Component.class))
    @Import({DdcBizRepositoryImpl.class, DdcAppRepositoryImpl.class, DdcEnvRepositoryImpl.class,
            DdcNamespaceRepositoryImpl.class, DdcNamespaceEnvAppBindingRepositoryImpl.class,
            DdcBizService.class, DdcAppService.class, DdcScopeGate.class, DdcNamespaceEnvAppBindingService.class})
    public static class TianshuPersistenceConfiguration {
        @Bean("jacksonObjectMapper") ObjectMapper mapper() { return strictMapper(); }

        @Bean("egonColaShardingLogicalDataSourceFactory")
        top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingDataSourceBootstrapper.LogicalDataSourceFactory logicalFactory(
                @Qualifier("egonColaValidationUtils") ValidationUtils validation,
                @Qualifier("egon.cola.component.mybatis-plus.sharding-top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties")
                EgonColaShardingProperties properties) {
            var runner = new EgonColaPostgreDdlRunner(validation, new PathMatchingResourcePatternResolver(),
                    Clock.systemUTC(), Duration.ofSeconds(5), Duration.ofSeconds(30));
            var initializer = new top.egon.cola.component.outbox.migration.OutboxManagedDdlInitializer(runner,
                    new EgonColaShardingTopologyValidator(validation), validation, properties,
                    new top.egon.cola.component.outbox.autoconfigure.OutboxMpStorageProperties(), strictMapper());
            return new top.egon.cola.component.outbox.migration.OutboxLogicalDataSourceFactory(initializer);
        }

        @Bean("egon.cola.component.mybatis-plus.sharding-top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties")
        EgonColaShardingProperties shardingProperties(@Qualifier("isolatedVerifiedDatabaseInputs") VerifiedDatabaseInputsBO inputs) {
            return EgonColaShardingProperties.builder().mode(EgonColaShardingProperties.ModeEnum.SHARDING)
                    .configStyle(EgonColaShardingProperties.ConfigStyleEnum.NATIVE)
                    .nativeRulesResource("file:" + inputs.getManifest().getRuntimeRoot() + "/profiles/tianshu-sharding.yml")
                    .dataSources(List.of(new EgonColaShardingProperties.PhysicalDataSourceProperties("tianshu_0", "tianshu_0",
                            EgonColaShardingProperties.DataSourceRoleEnum.PRIMARY, "org.postgresql.Driver", inputs.getJdbcUrl(),
                            inputs.getReceipt().getDatabaseOwner(), ""))).build();
        }

        @Bean("isolatedTianshuTopology")
        EgonColaShardingTopologyValidator.TopologyBO topology(@Qualifier("egonColaValidationUtils") ValidationUtils validation,
                @Qualifier("isolatedPreparedCatalogInputs") PreparedCatalogInputsBO prepared,
                @Qualifier("isolatedPhysicalDataSource") DataSource physical,
                @Qualifier("isolatedOwnershipGuard") Runnable guard,
                @Qualifier("egon.cola.component.mybatis-plus.sharding-top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties") EgonColaShardingProperties properties) throws IOException {
            var topology = new EgonColaShardingTopologyValidator(validation).validate(properties, prepared.getYaml());
            EgonColaDdlManifestBO manifest;
            try (var stream = new ClassPathResource("db/egon-mp/repository-manifest.json").getInputStream()) {
                manifest = validation.validate(strictMapper().readValue(stream, EgonColaDdlManifestBO.class));
            }
            if (!"tianshu".equals(manifest.family())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            guard.run();
            if (prepared.isInitializeSchema()) {
                var runner = new EgonColaPostgreDdlRunner(validation, new PathMatchingResourcePatternResolver(),
                        Clock.systemUTC(), Duration.ofSeconds(5), Duration.ofSeconds(30));
                var results = runner.run(List.of(new EgonColaDdlTargetBO("tianshu_0", "public", EgonColaDdlTargetBO.RoleEnum.MASTER_DATA,
                        physical, manifest, topology.fingerprint())));
                if (results.isEmpty() || results.size() != manifest.scripts().size() - manifest.startingIndex(results.getFirst().version())
                        || results.stream().anyMatch(result -> !"public".equals(result.schema())
                        || !"tianshu_0".equals(result.alias()))) { throw new IllegalStateException("XINGYUAN_PARTIAL"); }
            }
            guard.run();
            DdcDatabaseReadyValidator.validateTianshuSchema(physical, topology.fingerprint(), manifest);
            return topology;
        }

        @Bean(name = "dataSource", destroyMethod = "close")
        @org.springframework.context.annotation.Primary
        DataSource logicalDataSource(@Qualifier("isolatedPhysicalDataSource") DataSource physical,
                @Qualifier("isolatedTianshuTopology") EgonColaShardingTopologyValidator.TopologyBO topology,
                @Qualifier("egonColaShardingLogicalDataSourceFactory") top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingDataSourceBootstrapper.LogicalDataSourceFactory factory,
                @Qualifier("isolatedOwnershipGuard") Runnable guard) throws Exception {
            guard.run(); return factory.create(Map.of("tianshu_0", physical), topology.yaml());
        }

        @Bean("egonColaRoutingProfiles")
        Map<String, EgonColaRoutingProfileBO> routingProfiles(@Qualifier("isolatedTianshuTopology") EgonColaShardingTopologyValidator.TopologyBO topology) { return topology.profiles(); }
        @Bean("egonColaShardingRouteFingerprint")
        String fingerprint(@Qualifier("isolatedTianshuTopology") EgonColaShardingTopologyValidator.TopologyBO topology) { return topology.fingerprint(); }
        @Bean("egonColaWriteTargetResolver")
        EgonColaWriteTargetResolver writeTargetResolver(@Qualifier("isolatedTianshuTopology") EgonColaShardingTopologyValidator.TopologyBO topology,
                @Qualifier("egonColaTwoLevelRouteStrategy") EgonColaTwoLevelRouteStrategy strategy,
                @Qualifier("egonColaValidationUtils") ValidationUtils validation) {
            return new EgonColaShardingWriteTargetResolver(topology.profiles(), strategy, validation, topology.fingerprint());
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class PreparedCatalogInputsBO {
        private VerifiedDatabaseInputsBO database;
        private PlatformCatalogRecipeDTO recipe;
        private String recipeSha256;
        private String registrySha256;
        private String tenantId;
        private boolean initializeSchema;
        @lombok.ToString.Exclude private String actorId;
        @lombok.ToString.Exclude private byte[] yaml;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class CatalogProvisioningReceiptDTO {
        private int schemaVersion;
        private String runId;
        private String manifestSha256;
        private long databaseOid;
        private String recipeSha256;
        private String registrySha256;
        private String tenantId;
        private String status;
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
        private Instant recordedAt;
        private Map<String, DdcBizResult> businesses;
        private Map<String, DdcAppResult> applications;
    }

    @Test
    void aForeignPlatformRecipeCannotReachAnOwner() {
        var manifest = intent();
        var recipe = recipe(manifest);
        recipe.getApplications().getFirst().setAppCode("foreign-business");
        assertThrows(IllegalArgumentException.class, () -> validateCatalogRecipe(recipe, manifest));
    }

    @Test
    void duplicateJsonAndAnOldDatabaseReceiptAreRejectedBeforeConnecting() throws Exception {
        assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class,
                () -> strictMapper().readTree("{\"schemaVersion\":1,\"schemaVersion\":1}"));
        var manifest = intent();
        String digest = "a".repeat(64);
        var receipt = IsolatedDatabaseReceiptDTO.builder().runId(manifest.getRunId()).manifestSha256(digest)
                .role("tianshu").databaseName("old_tianshu").oid(42).databaseOwner("task_owner")
                .ownerComment("xingyuan-it:" + manifest.getRunId() + ":" + digest).build();
        assertThrows(IllegalArgumentException.class, () -> validateDatabaseReceipt(manifest, digest, "tianshu", receipt));
    }

    @Test
    void anUnsafePrivateFileCannotBeReadAsARecipe() throws Exception {
        Path file = temporaryRepository.resolve("recipe.json");
        Files.writeString(file, "{}");
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"));
        assertThrows(IllegalArgumentException.class, () -> readProtectedBytes(file, temporaryRepository, 100));
    }

    @Test
    void approvedCatalogRecipeRejectsDuplicateBusinessesAndForeignRuns() {
        var manifest = intent();
        assertDoesNotThrow(() -> validateCatalogRecipe(recipe(manifest), manifest));
        var duplicate = recipe(manifest);
        duplicate.setBusinesses(List.of(duplicate.getBusinesses().getFirst(), duplicate.getBusinesses().getFirst(), duplicate.getBusinesses().getLast()));
        assertThrows(IllegalArgumentException.class, () -> validateCatalogRecipe(duplicate, manifest));
        var foreign = recipe(manifest).setRunId(UUID.randomUUID().toString());
        assertThrows(IllegalArgumentException.class, () -> validateCatalogRecipe(foreign, manifest));
    }

    @Test
    void catalogInputsRequireCommittedIdentityAndHashBoundRecipesBeforeOpeningAContext() throws Exception {
        var manifest = intent();
        Path root = Path.of(manifest.getRuntimeRoot());
        for (String directory : List.of("", "evidence", "secrets", "policies", "profiles")) {
            Path path = root.resolve(directory);
            Files.createDirectories(path);
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwx------"));
        }
        Path recipeFile = root.resolve("policies/platform-catalog.json");
        Path yaml = root.resolve("profiles/tianshu-sharding.yml");
        writePrivate(recipeFile, strictMapper().writeValueAsBytes(recipe(manifest)));
        writePrivate(yaml, "controlled-policy-no-io".getBytes(StandardCharsets.UTF_8));
        manifest.setPolicyFiles(List.of(FileReferenceDTO.builder().path(recipeFile.toString()).sha256(sha256(Files.readAllBytes(recipeFile))).build(),
                FileReferenceDTO.builder().path(yaml.toString()).sha256(sha256(Files.readAllBytes(yaml))).build()));
        String digest = "a".repeat(64);
        Path registryFile = root.resolve("secrets/runtime-registry.json");
        Map<String, Object> registry = Map.of("schemaVersion", 1, "runId", manifest.getRunId(), "manifestSha256", digest,
                "tenantIds", Map.of("default", "17", "tenant-b", "18"), "subjects", Map.of("admin", "real-owner-subject"));
        writePrivate(registryFile, strictMapper().writeValueAsBytes(registry));
        Path identityFile = root.resolve("evidence/identity.json");
        var identity = new LinkedHashMap<String, Object>(Map.of("runId", manifest.getRunId(), "manifestSha256", digest,
                "operationId", "CLI-002", "status", "COMMITTED"));
        writePrivate(identityFile, strictMapper().writeValueAsBytes(identity));
        var database = VerifiedDatabaseInputsBO.builder().manifest(manifest).manifestSha256(digest).build();
        var prepared = readCatalogInputs(database);
        assertEquals("17", prepared.getTenantId());
        assertEquals("real-owner-subject", prepared.getActorId());
        assertFalse(prepared.toString().contains("real-owner-subject"));
        identity.put("status", "PARTIAL");
        writePrivate(identityFile, strictMapper().writeValueAsBytes(identity));
        assertThrows(IllegalArgumentException.class, () -> readCatalogInputs(database));
        identity.put("status", "COMMITTED");
        writePrivate(identityFile, strictMapper().writeValueAsBytes(identity));
        Files.writeString(recipeFile, "{}", StandardOpenOption.TRUNCATE_EXISTING);
        assertThrows(IllegalArgumentException.class, () -> readCatalogInputs(database));
    }

    @Test
    void aCheckpointCannotOverwriteChangedEvidenceOrALink() throws Exception {
        Path directory = temporaryRepository.resolve("evidence");
        Files.createDirectory(directory, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Path target = directory.resolve("catalog.json");
        byte[] original = "original".getBytes(StandardCharsets.UTF_8);
        publishCheckpoint(target, original, null);
        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(target));
        assertThrows(IllegalStateException.class, () -> publishCheckpoint(target, new byte[]{1}, new byte[]{2}));
        assertArrayEquals(original, Files.readAllBytes(target));
        Path link = directory.resolve("linked.json");
        Files.createSymbolicLink(link, target);
        assertThrows(IllegalArgumentException.class, () -> publishCheckpoint(link, original, original));
    }

    @Test
    void aTaskFileInAPublicDirectoryIsRejectedEvenWhenTheFileIsPrivate() throws Exception {
        Path task = temporaryRepository.resolve("private-task");
        Files.createDirectory(task, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Path exposed = Files.createDirectory(task.resolve("exposed"), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x")));
        Path file = exposed.resolve("recipe.json");
        writePrivate(file, "{}".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> readProtectedBytes(file, task, 100));
    }

    private static void writePrivate(Path path, byte[] bytes) throws IOException {
        Files.write(path, bytes);
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
    }

    private static PlatformCatalogRecipeDTO recipe(IsolatedIntegrationManifestDTO manifest) {
        return PlatformCatalogRecipeDTO.builder().schemaVersion(1).runId(manifest.getRunId())
                .tenantKey("default").actorKey("admin")
                .businesses(List.of("permission", "identity", "platform").stream().map(code ->
                        BusinessPreparationDTO.builder().bizCode(code).bizName(code).build()).toList())
                .applications(PLATFORM_BUSINESSES.entrySet().stream().map(entry ->
                        ApplicationPreparationDTO.builder().appCode(entry.getKey()).bizCode(entry.getValue())
                                .appName(entry.getKey()).build()).toList()).build();
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class PlatformCatalogRecipeDTO {
        @Min(1) @Max(1) private int schemaVersion;
        @NotBlank private String runId;
        @NotBlank @Pattern(regexp = "default") private String tenantKey;
        @NotBlank @Pattern(regexp = "admin") private String actorKey;
        @NotNull @Size(min = 3, max = 3) @Valid private List<@NotNull BusinessPreparationDTO> businesses;
        @NotNull @Size(min = 4, max = 4) @Valid private List<@NotNull ApplicationPreparationDTO> applications;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class BusinessPreparationDTO {
        @NotBlank @Size(max = 128) private String bizCode;
        @NotBlank @Size(max = 128) private String bizName;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class ApplicationPreparationDTO {
        @NotBlank @Size(max = 128) private String appCode;
        @NotBlank @Size(max = 128) private String appName;
        @NotBlank @Size(max = 128) private String bizCode;
    }

    static void requirePrivateDirectory(Path directory) throws IOException {
        for (Path current = directory; current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
        }
        var user = directory.getFileSystem().getUserPrincipalLookupService()
                .lookupPrincipalByName(System.getProperty("user.name"));
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
                || !Files.getOwner(directory, LinkOption.NOFOLLOW_LINKS).equals(user)
                || !Files.getPosixFilePermissions(directory, LinkOption.NOFOLLOW_LINKS)
                .equals(PosixFilePermissions.fromString("rwx------"))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
    }

    static byte[] readProtectedBytes(Path file, Path taskRoot, int maximumBytes) throws IOException {
        if (!file.isAbsolute() || !file.equals(file.normalize()) || maximumBytes < 1) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        if (taskRoot != null && (!taskRoot.isAbsolute() || !taskRoot.equals(taskRoot.normalize())
                || file.equals(taskRoot) || !file.startsWith(taskRoot))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        for (Path current = file; current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE");
        }
        var user = file.getFileSystem().getUserPrincipalLookupService()
                .lookupPrincipalByName(System.getProperty("user.name"));
        if (taskRoot != null) {
            for (Path directory = file.getParent(); directory != null && directory.startsWith(taskRoot); directory = directory.getParent()) {
                if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
                        || !Files.getOwner(directory, LinkOption.NOFOLLOW_LINKS).equals(user)
                        || !Files.getPosixFilePermissions(directory, LinkOption.NOFOLLOW_LINKS)
                        .equals(PosixFilePermissions.fromString("rwx------"))) {
                    throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                }
            }
        }
        if (!Files.getOwner(file, LinkOption.NOFOLLOW_LINKS).equals(user)
                || !Files.getPosixFilePermissions(file, LinkOption.NOFOLLOW_LINKS)
                .equals(PosixFilePermissions.fromString("rw-------"))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        BasicFileAttributes before = Files.readAttributes(file, BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
        if (before.size() > maximumBytes) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
        byte[] contents;
        try (var input = Files.newInputStream(file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            contents = input.readNBytes(maximumBytes + 1);
        }
        BasicFileAttributes after = Files.readAttributes(file, BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
        if (contents.length > maximumBytes) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
        if (!Objects.equals(before.fileKey(), after.fileKey())
                || before.size() != after.size()
                || !before.lastModifiedTime().equals(after.lastModifiedTime())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        return contents;
    }

    static void validateDatabaseReceipt(IsolatedIntegrationManifestDTO manifest, String digest,
                                        String role, IsolatedDatabaseReceiptDTO receipt) {
        validateTargets(manifest, Path.of(manifest.getRepoRoot()));
        if (receipt == null || digest == null || !digest.matches("[0-9a-f]{64}")
                || !manifest.getRunId().equals(receipt.getRunId())
                || !digest.equals(receipt.getManifestSha256())
                || !role.equals(receipt.getRole())
                || !Objects.equals(DATABASE_NAMES.get(role), receipt.getDatabaseName())
                || receipt.getOid() <= 0L
                || receipt.getDatabaseOwner() == null || receipt.getDatabaseOwner().isBlank()
                || !("xingyuan-it:" + manifest.getRunId() + ":" + digest).equals(receipt.getOwnerComment())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
    }

    static VerifiedDatabaseInputsBO readVerifiedDatabaseInputs(
            Path manifestFile, Path receiptFile, Path repository, String role) throws IOException {
        byte[] manifestBytes = readProtectedBytes(manifestFile, null, 128 * 1024);
        var manifest = strictMapper().readValue(manifestBytes, IsolatedIntegrationManifestDTO.class);
        validateTargets(manifest, repository);
        Path taskRoot = Path.of(manifest.getRuntimeRoot());
        requirePrivateDirectory(taskRoot);
        requirePrivateDirectory(taskRoot.resolve("evidence"));
        if (!manifestFile.startsWith(taskRoot)) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        String digest;
        try {
            digest = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(manifestBytes));
        } catch (NoSuchAlgorithmException unavailableAlgorithm) {
            throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE");
        }
        var receipt = strictMapper().readValue(
                readProtectedBytes(receiptFile, taskRoot.resolve("evidence"), 128 * 1024),
                IsolatedDatabaseReceiptDTO.class);
        validateDatabaseReceipt(manifest, digest, role, receipt);
        return VerifiedDatabaseInputsBO.builder().manifest(manifest).receipt(receipt)
                .manifestSha256(digest).jdbcUrl("jdbc:postgresql://127.0.0.1:5432/" + receipt.getDatabaseName()).build();
    }

    static void verifyDatabaseConnection(Connection connection, IsolatedDatabaseReceiptDTO receipt)
            throws SQLException {
        try (var query = connection.prepareStatement("""
                SELECT current_database(), host(inet_server_addr()), inet_server_port(),
                       d.oid::bigint, shobj_description(d.oid, 'pg_database'), pg_get_userbyid(d.datdba)
                FROM pg_database d WHERE d.datname = current_database()
                """)) {
            query.setQueryTimeout(5);
            try (var rows = query.executeQuery()) {
                if (!rows.next() || !receipt.getDatabaseName().equals(rows.getString(1))
                        || !"127.0.0.1".equals(rows.getString(2)) || rows.getInt(3) != 5432
                        || receipt.getOid() != rows.getLong(4)
                        || !receipt.getOwnerComment().equals(rows.getString(5))
                        || !receipt.getDatabaseOwner().equals(rows.getString(6)) || rows.next()) {
                    throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                }
            }
        }
    }

    static ObjectMapper strictMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .featuresToEnable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION,
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
                        DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT,
                        MapperFeature.ALLOW_COERCION_OF_SCALARS).build();
    }

    static void validateTargets(IsolatedIntegrationManifestDTO manifest, Path repository) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            if (!validator.validate(manifest, Default.class, Prepare.class).isEmpty()) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
            }
        }
        Path expectedRepository = repository.toAbsolutePath().normalize();
        Path taskRoot = expectedRepository.resolve("target/xingyuan-integration-" + manifest.getRunId());
        if (!expectedRepository.toString().equals(manifest.getRepoRoot())
                || !taskRoot.toString().equals(manifest.getRuntimeRoot())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        if (!"https://127.0.0.1:28443".equals(manifest.getPublicOrigin())
                || !"127.0.0.1:5432".equals(manifest.getPostgres().getEndpoint())
                || !"postgres".equals(manifest.getPostgres().getMaintenanceDatabase())
                || !"redis://127.0.0.1:6379".equals(manifest.getRedis().getEndpoint())
                || !List.of("default", "tenant-b").equals(manifest.getTenantCodes())
                || !"local-two-tenant-users-v1".equals(manifest.getUserRecipe())
                || !"local-explicit-role-fields-v1".equals(manifest.getAuthorizationRecipe())
                || !UUID.fromString(manifest.getRunId()).toString().equals(manifest.getRunId())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var roles = new HashSet<String>();
        var names = new HashSet<String>();
        for (DatabaseTargetDTO database : manifest.getDatabases()) {
            if (!database.getName().equals(DATABASE_NAMES.get(database.getRole()))
                    || !roles.add(database.getRole()) || !names.add(database.getName())) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
        }
        if (!roles.equals(DATABASE_NAMES.keySet())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        try {
            if (Long.parseLong(manifest.getPlatformStoragePartitionId()) <= 0L) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
        } catch (NumberFormatException invalidPartition) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
    }

    private IsolatedIntegrationManifestDTO intent() {
        String runId = UUID.randomUUID().toString();
        String repository = temporaryRepository.toAbsolutePath().normalize().toString();
        String runtime = Path.of(repository).resolve("target/xingyuan-integration-" + runId).toString();
        return IsolatedIntegrationManifestDTO.builder().schemaVersion(1).runId(runId)
                .repoRoot(repository).runtimeRoot(runtime).publicOrigin("https://127.0.0.1:28443")
                .postgres(PostgresTargetDTO.builder().endpoint("127.0.0.1:5432")
                        .maintenanceDatabase("postgres").credentialFile(runtime + "/secrets/postgres.json").build())
                .redis(RedisTargetDTO.builder().endpoint("redis://127.0.0.1:6379")
                        .database(15).credentialFile(runtime + "/secrets/redis.json").build())
                .databases(DATABASE_NAMES.entrySet().stream().map(e -> DatabaseTargetDTO.builder()
                        .role(e.getKey()).name(e.getValue()).build()).toList())
                .platformStoragePartitionId("900000000000000001")
                .tenantCodes(List.of("default", "tenant-b"))
                .userRecipe("local-two-tenant-users-v1").authorizationRecipe("local-explicit-role-fields-v1")
                .processes(List.of()).artifacts(Map.of()).secretFiles(Map.of())
                .deadlines(DeadlineDTO.builder().startupSeconds(300).operationSeconds(900).httpSeconds(30).build())
                .catalogFiles(List.of()).policyFiles(List.of()).build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class IsolatedIntegrationManifestDTO {
        @Min(1) @Max(1) private int schemaVersion;
        @NotBlank @Pattern(regexp = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}") private String runId;
        @NotBlank @Size(max = 4096) private String repoRoot;
        @NotBlank @Size(max = 4096) private String runtimeRoot;
        @NotBlank private String publicOrigin;
        @NotNull @Valid private PostgresTargetDTO postgres;
        @NotNull @Valid private RedisTargetDTO redis;
        @NotNull @Size(min = 9, max = 12) @Valid private List<DatabaseTargetDTO> databases;
        @NotBlank @Pattern(regexp = "[1-9][0-9]{0,18}") private String platformStoragePartitionId;
        @NotNull @Size(min = 2, max = 2) private List<@NotBlank String> tenantCodes;
        @NotBlank private String userRecipe;
        @NotBlank private String authorizationRecipe;
        @NotNull @Size(max = 32) @Valid private List<ProcessTargetDTO> processes;
        @NotNull @Size(max = 32) @Valid private Map<@NotBlank String, ArtifactReferenceDTO> artifacts;
        @NotNull @Size(max = 32) private Map<@NotBlank String, @NotBlank String> secretFiles;
        @NotNull @Valid private DeadlineDTO deadlines;
        @NotNull @Size(max = 32) @Valid private List<FileReferenceDTO> catalogFiles;
        @NotNull @Size(max = 32) @Valid private List<FileReferenceDTO> policyFiles;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class PostgresTargetDTO {
        @NotBlank private String endpoint;
        @NotBlank private String maintenanceDatabase;
        @NotBlank @Size(max = 4096) private String credentialFile;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class RedisTargetDTO {
        @NotBlank private String endpoint;
        @Min(1) @Max(15) private int database;
        @NotBlank @Size(max = 4096) private String credentialFile;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class DatabaseTargetDTO {
        @NotBlank private String role;
        @NotBlank @Pattern(regexp = "[a-z][a-z0-9_]{0,62}") private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class ProcessTargetDTO {
        @NotBlank private String id;
        @NotBlank private String artifactRole;
        @NotBlank @Pattern(regexp = "JAVA|NODE|NGINX") private String kind;
        @Min(1) @Max(65535) private int port;
        @Builder.Default @Size(max = 2) private List<@Min(1) @Max(65535) Integer> additionalPorts = List.of();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class ArtifactReferenceDTO {
        @NotBlank @Size(max = 4096) private String path;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String sha256;
        @NotBlank @Size(max = 128) private String buildId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class FileReferenceDTO {
        @NotBlank @Size(max = 4096) private String path;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String sha256;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class DeadlineDTO {
        @Min(1) @Max(300) private int startupSeconds;
        @Min(1) @Max(900) private int operationSeconds;
        @Min(1) @Max(30) private int httpSeconds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class IsolatedDatabaseReceiptDTO {
        @NotBlank private String runId;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String manifestSha256;
        @NotBlank private String role;
        @NotBlank private String databaseName;
        @Min(1) private long oid;
        @NotBlank private String ownerComment;
        @NotBlank private String databaseOwner;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class VerifiedDatabaseInputsBO {
        private IsolatedIntegrationManifestDTO manifest;
        private IsolatedDatabaseReceiptDTO receipt;
        private String manifestSha256;
        private String jdbcUrl;
    }

}
