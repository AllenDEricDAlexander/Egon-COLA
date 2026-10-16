package top.egon.cola.platform.tianquan.shoubing.admin.integration;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.RSAKey;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.SimpleTransactionStatus;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.id.autoconfigure.IdGeneratorAutoConfiguration;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration;
import top.egon.cola.component.common.mybatis.ddl.EgonColaPostgreDdlRunner;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaTwoLevelRouteStrategy;
import top.egon.cola.component.common.mybatis.routing.EgonColaWriteTargetResolver;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingDataSourceBootstrapper;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingTopologyValidator;
import top.egon.cola.component.common.mybatis.sharding.resolver.EgonColaShardingWriteTargetResolver;
import top.egon.cola.component.outbox.autoconfigure.OutboxMpStorageProperties;
import top.egon.cola.component.outbox.migration.OutboxManagedDdlInitializer;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.converter.IdentityCredentialConverterImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.converter.IdentityUserConverterImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.IdentityDirectoryContext;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.IdentityPersistenceAdapter;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.IdentityUserDirectory;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.MpIdentityUserDirectory;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repository.IdentityCredentialRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repository.IdentityUserRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.service.impl.SpringPasswordHashAdapter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.config.OAuthManagedDdlComponent;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.config.OAuthPersistenceConfiguration;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthRealmConverterImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.CreatedOAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl.OAuthRealmRepositoryImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthClientService;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.CreateResourceServerDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.UpsertClientResourceGrantDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerService;
import top.egon.cola.platform.tianquan.shoubing.admin.support.bootstrap.IdpBootstrapService;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.converter.IdentityTenantConverterImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.converter.IdentityTenantMembershipConverterImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.repo.IdentityTenantMembershipRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.repo.IdentityTenantRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.TenantMembershipService;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.TenantService;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.impl.TenantMembershipServiceImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.impl.TenantServiceImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.token.config.OAuthKeyEncryptionProperties;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSigningKeyBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthSigningKeyConverterImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthSigningKeyRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl.OAuthSigningKeyRepositoryImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthKeyCipherComponent;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthPayloadCipherComponent;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthSecurityProjectionComponent;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUser;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUserStatus;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.port.IdentityUserStore;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Test-only input contracts and pre-I/O guards for the explicitly opted-in isolated run. */
public class IsolatedOAuthProvisioningIT {

    static final Map<String, String> DATABASE_NAMES = Map.of(
            "idp", "xingyuan_it_idp_20261011", "rbac", "xingyuan_it_rbac_20261011",
            "tianshu", "xingyuan_it_tianshu_20261011", "yuheng", "xingyuan_it_yuheng_20261011",
            "llm", "xingyuan_it_llm_20261011", "light", "xingyuan_it_light_20261011",
            "web", "xingyuan_it_web_20261011", "service", "xingyuan_it_service_20261011",
            "agent", "xingyuan_it_agent_20261011");

    static final Map<String, String> RUNTIME_PROFILES = Map.ofEntries(Map.entry("shoubing", "local"), Map.entry("jianshen", "local"),
                Map.entry("tianshu", "local"), Map.entry("yuheng", "local"), Map.entry("edge", "local"),
                Map.entry("biz", "operations"), Map.entry("mcp", "operations"), Map.entry("llm", "default"),
                Map.entry("native-light", "dev"), Map.entry("native-web", "dev"), Map.entry("native-service", "dev"), Map.entry("native-agent", "dev"));

    @TempDir
    Path temporaryRepository;

    @BeforeEach
    void useCanonicalTemporaryRepository() throws IOException {
        temporaryRepository = temporaryRepository.toRealPath();
    }

    @Test
    void unknownDuplicateTrailingAndCoercedJsonCannotReachAWriter() {
        ObjectMapper mapper = strictMapper();
        for (String input : List.of("{\"schemaVersion\":1,\"unknown\":true}",
                "{\"schemaVersion\":1,\"schemaVersion\":1}",
                "{\"schemaVersion\":1}{}", "{\"schemaVersion\":\"1\"}",
                "{\"schemaVersion\":1.5}", "{\"schemaVersion\":null}")) {
            assertThatThrownBy(() -> mapper.readValue(input, IsolatedIntegrationManifestDTO.class))
                    .isInstanceOf(IOException.class);
        }
    }

    @Test
    void aForeignDatabaseIsRejectedBeforeAnyWriter() {
        IsolatedIntegrationManifestDTO manifest = intent();
        manifest.getDatabases().getFirst().setName("egon_cola");
        AtomicInteger writes = new AtomicInteger();
        assertThatThrownBy(() -> guardedWrite(manifest, temporaryRepository, writes::incrementAndGet))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThat(writes).hasValue(0);
    }

    @Test
    void anEscapingTaskRootIsRejectedBeforeAnyWriter() {
        IsolatedIntegrationManifestDTO manifest = intent();
        manifest.setRuntimeRoot(temporaryRepository.resolve("target/../old-runtime").toString());
        AtomicInteger writes = new AtomicInteger();
        assertThatThrownBy(() -> guardedWrite(manifest, temporaryRepository, writes::incrementAndGet))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThat(writes).hasValue(0);
    }

    @Test
    void aDuplicateDatabaseRoleIsRejectedBeforeAnyWriter() {
        IsolatedIntegrationManifestDTO manifest = intent();
        manifest.getDatabases().get(1).setRole(manifest.getDatabases().getFirst().getRole());
        AtomicInteger writes = new AtomicInteger();
        assertThatThrownBy(() -> guardedWrite(manifest, temporaryRepository, writes::incrementAndGet))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThat(writes).hasValue(0);
    }

    @Test
    void aSymlinkManifestIsRejectedBeforeParsing() throws Exception {
        Path actual = temporaryRepository.resolve("actual.json");
        Files.writeString(actual, "{}");
        Files.setPosixFilePermissions(actual, PosixFilePermissions.fromString("rw-------"));
        Path link = temporaryRepository.resolve("manifest.json");
        Files.createSymbolicLink(link, actual);
        assertThatThrownBy(() -> readProtectedBytes(link, null, 128 * 1024))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    @Test
    void anOwnedBoundedRegularFileCanBeRead() throws Exception {
        Path manifest = temporaryRepository.resolve("manifest.json");
        Files.writeString(manifest, "{}");
        Files.setPosixFilePermissions(manifest, PosixFilePermissions.fromString("rw-------"));
        assertThat(readProtectedBytes(manifest, temporaryRepository, 128 * 1024))
                .isEqualTo("{}".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void aWorldReadableManifestIsRejectedBeforeParsing() throws Exception {
        Path manifest = temporaryRepository.resolve("manifest.json");
        Files.writeString(manifest, "{}");
        Files.setPosixFilePermissions(manifest, PosixFilePermissions.fromString("rw-r--r--"));
        assertThatThrownBy(() -> readProtectedBytes(manifest, null, 128 * 1024))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    @Test
    void aProtectedFileCannotEscapeItsTaskRoot() throws Exception {
        Path outside = temporaryRepository.resolve("outside.json");
        Files.writeString(outside, "{}");
        Files.setPosixFilePermissions(outside, PosixFilePermissions.fromString("rw-------"));
        Path root = temporaryRepository.resolve("task");
        Files.createDirectories(root);
        assertThatThrownBy(() -> readProtectedBytes(outside, root, 128 * 1024))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    @Test
    void aFileOverTheBoundCannotBeReadAsAnIntent() throws Exception {
        Path manifest = temporaryRepository.resolve("large.json");
        Files.writeString(manifest, "x".repeat(129 * 1024));
        Files.setPosixFilePermissions(manifest, PosixFilePermissions.fromString("rw-------"));
        assertThatThrownBy(() -> readProtectedBytes(manifest, null, 128 * 1024))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("XINGYUAN_INPUT_INVALID");
    }

    @Test
    void aTaskFileInAPublicDirectoryIsRejectedEvenWhenTheFileIsPrivate() throws Exception {
        Path task = temporaryRepository.resolve("private-task");
        Files.createDirectory(task, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Path exposed = Files.createDirectory(task.resolve("exposed"), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x")));
        Path file = exposed.resolve("recipe.json");
        Files.writeString(file, "{}");
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> readProtectedBytes(file, task, 100));
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

    static void writePrivateSecret(Path file, Path secretRoot, byte[] value) throws IOException {
        validatePrivateSecretDestination(file, secretRoot);
        if (value == null || value.length == 0 || value.length > 4096) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
        Path parent = file.getParent();
        var expectedParent = Files.readAttributes(parent, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        try (var channel = FileChannel.open(file,
                Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))) {
            // Check the retained private parent again before placing any credential bytes in the new file.
            if (!Objects.equals(expectedParent.fileKey(),
                    Files.readAttributes(parent, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).fileKey())
                    || Files.isSymbolicLink(parent) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            var buffer = ByteBuffer.wrap(value);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        } catch (FileAlreadyExistsException alreadyCreated) {
            throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
        }
        if (!Objects.equals(expectedParent.fileKey(),
                Files.readAttributes(parent, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).fileKey())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
    }

    static void validatePrivateSecretDestination(Path file, Path secretRoot) throws IOException {
        if (file == null || secretRoot == null || !file.isAbsolute() || !file.equals(file.normalize())
                || !secretRoot.isAbsolute() || !secretRoot.equals(secretRoot.normalize())
                || !file.startsWith(secretRoot) || file.equals(secretRoot)) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        for (Path current = file; current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
        }
        var user = file.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name"));
        for (Path current = file.getParent(); current != null && current.startsWith(secretRoot); current = current.getParent()) {
            if (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE");
            }
            if (!Files.getOwner(current, LinkOption.NOFOLLOW_LINKS).equals(user)
                    || !Files.getPosixFilePermissions(current, LinkOption.NOFOLLOW_LINKS)
                    .equals(PosixFilePermissions.fromString("rwx------"))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
        }
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
        }
    }

    static void publishResult(Path file, Path evidenceRoot, IsolatedIntegrationResult result) throws IOException {
        validatePrivateSecretDestination(file, evidenceRoot);
        if (result == null || result.getSchemaVersion() != 1 || result.getCompletedAt() == null
                || result.getCounts() == null || result.getTargets() == null || result.getSteps() == null
                || result.getOperationId() == null || !result.getOperationId().matches("CLI-00[1-9]")
                || result.getManifestSha256() == null || !result.getManifestSha256().matches("[0-9a-f]{64}") || result.getStatus() == null
                || !Set.of("COMMITTED", "RUNNING", "PASSED", "STOPPED", "BLOCKED", "PARTIAL", "FAILED", "UNKNOWN_COMMIT")
                .contains(result.getStatus())) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            if (!validators.getValidator().validate(result.getCounts()).isEmpty()) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
            }
        }
        byte[] bytes = strictMapper().writeValueAsBytes(result);
        publishProtectedBytes(file, evidenceRoot, bytes);
    }

    static void publishProtectedBytes(Path file, Path protectedRoot, byte[] bytes) throws IOException {
        publishProtectedBytes(file, protectedRoot, bytes, 128 * 1024);
    }

    static void publishProtectedBytes(Path file, Path protectedRoot, byte[] bytes, int maximumBytes) throws IOException {
        validatePrivateSecretDestination(file, protectedRoot);
        if (maximumBytes < 1 || maximumBytes > 2 * 1024 * 1024 || bytes.length > maximumBytes) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
        Path parent = file.getParent();
        var originalParent = Files.readAttributes(parent, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        Path temporary = Files.createTempFile(parent, ".xingyuan-result-", ".tmp",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        try {
            try (var channel = FileChannel.open(temporary, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                var buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            validatePrivateSecretDestination(file, protectedRoot);
            if (!Objects.equals(originalParent.fileKey(),
                    Files.readAttributes(parent, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).fileKey())) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            Files.move(temporary, file, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            try (var directory = FileChannel.open(parent, StandardOpenOption.READ)) {
                directory.force(true);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Test
    void reportPublisherRejectsForeignDestinationsAndExistingEvidence() throws Exception {
        Path evidence = Files.createDirectory(temporaryRepository.resolve("evidence"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        var result = IsolatedIntegrationResult.builder().schemaVersion(1).operationId("CLI-002")
                .runId(UUID.randomUUID().toString()).manifestSha256("a".repeat(64)).status("COMMITTED")
                .completedAt(Instant.now()).counts(IsolatedCountsDTO.builder().build()).build();
        Path foreign = temporaryRepository.resolve("foreign.json");
        assertThatThrownBy(() -> publishResult(foreign, evidence, result)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThat(Files.exists(foreign)).isFalse();
        Path existing = evidence.resolve("previous.json");
        Files.writeString(existing, "retained evidence");
        Files.setPosixFilePermissions(existing, PosixFilePermissions.fromString("rw-------"));
        assertThatThrownBy(() -> publishResult(existing, evidence, result)).hasMessage("XINGYUAN_STATE_CONFLICT");
        assertThat(Files.readString(existing)).isEqualTo("retained evidence");
    }

    @Test
    void reportPublicationIsPrivateCompleteAndLeavesNoTemporaryFrame() throws Exception {
        Path evidence = Files.createDirectory(temporaryRepository.resolve("evidence"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Path file = evidence.resolve("result.json");
        var result = IsolatedIntegrationResult.builder().schemaVersion(1).operationId("CLI-002")
                .runId(UUID.randomUUID().toString()).manifestSha256("a".repeat(64)).status("COMMITTED")
                .completedAt(Instant.now()).counts(IsolatedCountsDTO.builder().build()).build();
        publishResult(file, evidence, result);
        assertThat(Files.getPosixFilePermissions(file)).isEqualTo(PosixFilePermissions.fromString("rw-------"));
        assertThat(strictMapper().readValue(readProtectedBytes(file, evidence, 128 * 1024), IsolatedIntegrationResult.class))
                .isEqualTo(result);
        try (var files = Files.list(evidence)) {
            assertThat(files.toList()).containsExactly(file);
        }
    }

    @Test
    void secretWriterRejectsAnEscapedPathAndNeverOverwritesExistingData() throws Exception {
        Path secrets = Files.createDirectory(temporaryRepository.resolve("secrets"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Path escaped = temporaryRepository.resolve("foreign.secret");
        assertThatThrownBy(() -> writePrivateSecret(escaped, secrets, new byte[] {1, 2}))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThat(Files.exists(escaped)).isFalse();
        Path existing = secrets.resolve("client.secret");
        Files.write(existing, new byte[] {7}, StandardOpenOption.CREATE_NEW);
        Files.setPosixFilePermissions(existing, PosixFilePermissions.fromString("rw-------"));
        assertThatThrownBy(() -> writePrivateSecret(existing, secrets, new byte[] {1, 2}))
                .isInstanceOf(IllegalStateException.class).hasMessage("XINGYUAN_STATE_CONFLICT");
        assertThat(Files.readAllBytes(existing)).containsExactly((byte) 7);
    }

    @Test
    void newSecretFilesArePrivateAndDoNotFollowAReplacedDirectory() throws Exception {
        Path secrets = Files.createDirectory(temporaryRepository.resolve("secrets"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Path file = secrets.resolve("client.secret");
        writePrivateSecret(file, secrets, new byte[] {1, 2});
        assertThat(Files.getPosixFilePermissions(file)).isEqualTo(PosixFilePermissions.fromString("rw-------"));
        assertThat(readProtectedBytes(file, secrets, 4096)).containsExactly((byte) 1, (byte) 2);
        Path foreign = Files.createDirectory(temporaryRepository.resolve("foreign"));
        Path link = Files.createSymbolicLink(secrets.resolve("replaced"), foreign);
        assertThatThrownBy(() -> writePrivateSecret(link.resolve("stolen.secret"), secrets, new byte[] {1, 2}))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThat(Files.exists(foreign.resolve("stolen.secret"))).isFalse();
    }

    @Test
    void ownershipFailurePreventsEveryRealIdentityOwnerCall() throws Exception {
        var bootstrap = Mockito.mock(IdpBootstrapService.class);
        var identities = Mockito.mock(IdentityUserStore.class);
        var directory = Mockito.mock(IdentityDirectoryContext.class);
        var tenants = Mockito.mock(TenantService.class);
        var memberships = Mockito.mock(TenantMembershipService.class);
        var manifest = intent();
        Mockito.when(directory.directoryPartitionId()).thenReturn(
                Long.parseLong(manifest.getPlatformStoragePartitionId()));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var component = new IsolatedOAuthProvisioningComponent(
                    () -> { throw new IllegalStateException("XINGYUAN_TARGET_UNSAFE"); },
                    bootstrap, identities, directory, tenants, memberships, factory.getValidator(), strictMapper(),
                    Mockito.mock(OAuthRealmRepository.class),
                    Mockito.mock(OAuthSigningKeyRepository.class),
                    Mockito.mock(OAuthKeyCipherComponent.class),
                    Mockito.mock(PlatformTransactionManager.class), Clock.systemUTC(),
                    new StaticListableBeanFactory().getBeanProvider(OAuthClientService.class),
                    new StaticListableBeanFactory().getBeanProvider(ResourceServerService.class),
                    new StaticListableBeanFactory().getBeanProvider(OAuthSecurityProjectionComponent.class),
                    new StaticListableBeanFactory().getBeanProvider(OAuthClientRepository.class),
                    Mockito.mock(top.egon.cola.platform.tianquan.shoubing.core.port.PasswordCredentialStore.class),
                    Mockito.mock(top.egon.cola.platform.tianquan.shoubing.core.port.PasswordHashPort.class));
            assertThatThrownBy(() -> component.bootstrapIdentityAndTenants(manifest, Map.of(
                    "admin", temporaryRepository.resolve("admin.password"),
                    "viewer", temporaryRepository.resolve("viewer.password"), "provisioner", temporaryRepository.resolve("provisioner.password"))))
                    .isInstanceOf(IllegalStateException.class).hasMessage("XINGYUAN_TARGET_UNSAFE");
            Mockito.verifyNoInteractions(bootstrap, identities, tenants, memberships);
        }
    }

    @Test
    void realmOwnershipFailurePreventsKeyGenerationAndTransaction() {
        var realmRepository = Mockito.mock(OAuthRealmRepository.class);
        var keyRepository = Mockito.mock(OAuthSigningKeyRepository.class);
        var keyCipher = Mockito.mock(OAuthKeyCipherComponent.class);
        var transactions = Mockito.mock(PlatformTransactionManager.class);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var component = new IsolatedOAuthProvisioningComponent(
                    () -> { throw new IllegalStateException("XINGYUAN_TARGET_UNSAFE"); },
                    Mockito.mock(IdpBootstrapService.class),
                    Mockito.mock(IdentityUserStore.class),
                    Mockito.mock(IdentityDirectoryContext.class),
                    Mockito.mock(TenantService.class),
                    Mockito.mock(TenantMembershipService.class),
                    factory.getValidator(), strictMapper(), realmRepository, keyRepository, keyCipher, transactions,
                    Clock.systemUTC(),
                    new StaticListableBeanFactory().getBeanProvider(OAuthClientService.class),
                    new StaticListableBeanFactory().getBeanProvider(ResourceServerService.class),
                    new StaticListableBeanFactory().getBeanProvider(OAuthSecurityProjectionComponent.class),
                    new StaticListableBeanFactory().getBeanProvider(OAuthClientRepository.class),
                    Mockito.mock(top.egon.cola.platform.tianquan.shoubing.core.port.PasswordCredentialStore.class),
                    Mockito.mock(top.egon.cola.platform.tianquan.shoubing.core.port.PasswordHashPort.class));
            var facts = IsolatedOAuthProvisioningComponent.IdentityFactsBO.builder()
                    .tenantIds(Map.of("default", "1001", "tenant-b", "1002")).subjects(Map.of()).provisioningSubject("controlled-provisioner").build();
            assertThatThrownBy(() -> component.bootstrapRealms(intent(), facts))
                    .isInstanceOf(IllegalStateException.class).hasMessage("XINGYUAN_TARGET_UNSAFE");
            Mockito.verifyNoInteractions(realmRepository, keyRepository, keyCipher, transactions);
        }
    }

    @Test
    void initialRealmKeyFailureRollsBackTheOwningTransaction() {
        var realms = Mockito.mock(OAuthRealmRepository.class);
        var keys = Mockito.mock(OAuthSigningKeyRepository.class);
        var cipher = Mockito.mock(OAuthKeyCipherComponent.class);
        var transactions = Mockito.mock(PlatformTransactionManager.class);
        Mockito.when(transactions.getTransaction(ArgumentMatchers.any()))
                .thenAnswer(call -> new SimpleTransactionStatus());
        Mockito.when(realms.save(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.anyLong())).thenAnswer(call -> call.getArgument(1));
        Mockito.when(cipher.encryptPrivateKey(ArgumentMatchers.any(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString())).thenReturn("ENC:v1:test:abcdefghijklmnop:abcdefghijklmnopqrstuv");
        Mockito.when(keys.save(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.anyLong())).thenThrow(new IllegalStateException("XINGYUAN_DEPENDENCY_UNAVAILABLE"));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var component = realmComponent(factory.getValidator(), realms, keys, cipher, transactions, () -> { });
            var facts = IsolatedOAuthProvisioningComponent.IdentityFactsBO.builder()
                    .tenantIds(Map.of("default", "1001", "tenant-b", "1002")).subjects(Map.of()).provisioningSubject("controlled-provisioner").build();
            assertThatThrownBy(() -> component.bootstrapRealms(intent(), facts)).isInstanceOf(IllegalStateException.class)
                    .hasMessage("XINGYUAN_DEPENDENCY_UNAVAILABLE");
            Mockito.verify(transactions).rollback(ArgumentMatchers.any());
            Mockito.verify(transactions, Mockito.never()).commit(ArgumentMatchers.any());
            Mockito.verify(realms).save(ArgumentMatchers.any(), ArgumentMatchers.any(),
                    ArgumentMatchers.eq(0L));
            Mockito.verify(realms, Mockito.never()).findByRealmKey(ArgumentMatchers.any());
        }
    }

    @Test
    void initialRealmsHaveIndependentRsaKeysAndAreReconciledAfterCommit() throws Exception {
        var realms = Mockito.mock(OAuthRealmRepository.class);
        var keys = Mockito.mock(OAuthSigningKeyRepository.class);
        var cipher = Mockito.mock(OAuthKeyCipherComponent.class);
        var transactions = Mockito.mock(PlatformTransactionManager.class);
        Map<String, OAuthRealmBO> storedRealms = new HashMap<>();
        Map<String, OAuthSigningKeyBO> storedKeys = new HashMap<>();
        var transactionOpen = new AtomicBoolean();
        Mockito.when(transactions.getTransaction(ArgumentMatchers.any())).thenAnswer(call -> {
            transactionOpen.set(true);
            return new SimpleTransactionStatus();
        });
        Mockito.doAnswer(call -> { transactionOpen.set(false); return null; })
                .when(transactions).commit(ArgumentMatchers.any());
        Mockito.when(realms.save(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.anyLong())).thenAnswer(call -> {
            var trusted = call.getArgument(0, TrustedRealmBO.class);
            var realm = call.getArgument(1, OAuthRealmBO.class);
            storedRealms.put(trusted.getRealmKey(), realm);
            return realm;
        });
        Mockito.when(keys.save(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.anyLong())).thenAnswer(call -> {
            var key = call.getArgument(1, OAuthSigningKeyBO.class);
            storedKeys.put(key.getKid(), key);
            return key;
        });
        Mockito.when(realms.findByRealmKey(ArgumentMatchers.any())).thenAnswer(call -> {
            assertThat(transactionOpen).isFalse();
            var trusted = call.getArgument(0, TrustedRealmBO.class);
            return Optional.ofNullable(storedRealms.get(trusted.getRealmKey()));
        });
        Mockito.when(keys.findByKid(ArgumentMatchers.any(), ArgumentMatchers.anyString()))
                .thenAnswer(call -> { assertThat(transactionOpen).isFalse(); return Optional.ofNullable(storedKeys.get(call.getArgument(1))); });
        Mockito.when(cipher.encryptPrivateKey(ArgumentMatchers.any(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString())).thenReturn("ENC:v1:test:abcdefghijklmnop:abcdefghijklmnopqrstuv");
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var component = realmComponent(factory.getValidator(), realms, keys, cipher, transactions, () -> { });
            var facts = IsolatedOAuthProvisioningComponent.IdentityFactsBO.builder()
                    .tenantIds(Map.of("default", "1001", "tenant-b", "1002")).subjects(Map.of()).provisioningSubject("controlled-provisioner").build();
            var contexts = component.bootstrapRealms(intent(), facts);
            assertThat(contexts).extracting(TrustedRealmBO::getRealmKey)
                    .containsExactly("platform", "tenant:1001", "tenant:1002");
            assertThat(storedKeys).hasSize(3);
            Set<BigInteger> moduli = new HashSet<>();
            for (var key : storedKeys.values()) {
                var rsa = RSAKey.parse(key.getPublicJwk().toString()).toRSAPublicKey();
                assertThat(rsa.getModulus().bitLength()).isGreaterThanOrEqualTo(3072);
                assertThat(key.getPublicJwk().fieldNames()).toIterable().doesNotContain("d", "p", "q");
                moduli.add(rsa.getModulus());
            }
            assertThat(moduli).hasSize(3);
            Mockito.verify(transactions, Mockito.times(3)).commit(ArgumentMatchers.any());
            Mockito.verify(transactions, Mockito.never()).rollback(ArgumentMatchers.any());
        }
    }

    @Test
    void phaseTwoOwnershipFailurePreventsEveryOptionalOwnerLookup() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var component = realmComponent(factory.getValidator(),
                    Mockito.mock(OAuthRealmRepository.class),
                    Mockito.mock(OAuthSigningKeyRepository.class),
                    Mockito.mock(OAuthKeyCipherComponent.class),
                    Mockito.mock(PlatformTransactionManager.class),
                    () -> { throw new IllegalStateException("XINGYUAN_TARGET_UNSAFE"); });
            var trusted = TrustedRealmBO.builder()
                    .issuer(URI.create(intent().getPublicOrigin() + "/platform")).realmKey("platform")
                    .realmType(RealmTypeEnum.PLATFORM)
                    .storagePartitionId(Long.parseLong(intent().getPlatformStoragePartitionId())).trustVersion(0L).build();
            assertThatThrownBy(() -> component.recoverSecurity(intent(), trusted)).hasMessage("XINGYUAN_TARGET_UNSAFE");
            assertThatThrownBy(() -> component.provisionResource(intent(), trusted,
                    new CreateResourceServerDTO(
                            "resource", "https://127.0.0.1:28443/resources/resource", "biz", "app", "local", "resource",
                            "management", "app", "entry"))).hasMessage("XINGYUAN_TARGET_UNSAFE");
            assertThatThrownBy(() -> component.provisionClient(intent(), trusted, trusted,
                    new CreateOAuthClientDTO(), "admin", null))
                    .hasMessage("XINGYUAN_TARGET_UNSAFE");
            assertThatThrownBy(() -> component.provisionGrant(intent(), trusted, trusted, "client", "resource",
                    UpsertClientResourceGrantDTO.builder().build()))
                    .hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
    }

    @Test
    void confidentialClientPublicationIsPrivateTransactionalAndReconciledWithoutRetry() throws Exception {
        var manifest = intent();
        Path secrets = Files.createDirectories(Path.of(manifest.getRuntimeRoot()).resolve("secrets"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Path secretFile = secrets.resolve("client-a.secret");
        String oneTimeSecret = "controlled-client-secret-1234567890";
        var trusted = TrustedRealmBO.builder()
                .issuer(URI.create(manifest.getPublicOrigin() + "/platform")).realmKey("platform")
                .realmType(RealmTypeEnum.PLATFORM)
                .storagePartitionId(Long.parseLong(manifest.getPlatformStoragePartitionId())).trustVersion(0L).build();
        var input = CreateOAuthClientDTO.builder()
                .appId("client-a-app").clientId("client-a").clientName("Controlled service")
                .clientType(OAuthClient.ClientType.CONFIDENTIAL)
                .purpose(OAuthClientPurposeEnum.SERVICE)
                .accessTokenTtlSeconds(300).refreshTokenTtlSeconds(0)
                .redirectUris(List.of()).resourceUris(List.of()).allowedScopes(List.of("tianshu:registration:write")).build();
        var owners = Mockito.mock(OAuthClientService.class);
        var clients = Mockito.mock(OAuthClientRepository.class);
        var realms = Mockito.mock(OAuthRealmRepository.class);
        var identities = Mockito.mock(IdentityUserStore.class);
        var transactions = Mockito.mock(PlatformTransactionManager.class);
        var open = new AtomicBoolean();
        var expectedFile = new AtomicReference<>(secretFile);
        Mockito.when(identities.findById("101")).thenReturn(Optional.of(
                new IdentityUser("101", "alice", "alice", "Alice",
                        IdentityUserStatus.ACTIVE, 0, null, null, 0L)));
        Mockito.when(realms.findByIssuer(trusted)).thenReturn(Optional.of(
                OAuthRealmBO.builder()
                        .issuer(trusted.getIssuer()).realmKey(trusted.getRealmKey()).realmType(trusted.getRealmType())
                        .storagePartitionId(trusted.getStoragePartitionId()).trustVersion(0L).status("ACTIVE").activeKid("key").build()));
        Mockito.when(transactions.getTransaction(ArgumentMatchers.any())).thenAnswer(call -> {
            open.set(true);
            return new SimpleTransactionStatus();
        });
        Mockito.doAnswer(call -> {
            assertThat(readProtectedBytes(expectedFile.get(), secrets, 4096)).isEqualTo(oneTimeSecret.getBytes(StandardCharsets.UTF_8));
            open.set(false);
            return null;
        }).when(transactions).commit(ArgumentMatchers.any());
        Mockito.when(owners.create(ArgumentMatchers.eq(trusted), ArgumentMatchers.eq(trusted),
                ArgumentMatchers.any(), ArgumentMatchers.eq("101"))).thenAnswer(call -> {
            assertThat(open).isTrue();
            var command = call.getArgument(2, CreateOAuthClientDTO.class);
            return CreatedOAuthClientVO.builder()
                    .clientId(command.getClientId()).issuer(trusted.getIssuer().toASCIIString()).version(0L)
                    .clientSecret(oneTimeSecret).build();
        });
        var committed = OAuthClientBO.builder()
                .id(44L).clientId(input.getClientId()).clientType(input.getClientType()).purpose(input.getPurpose())
                .appId(input.getAppId()).clientName(input.getClientName()).accessTtlSeconds(input.getAccessTokenTtlSeconds())
                .refreshTtlSeconds(input.getRefreshTokenTtlSeconds()).redirectUris(input.getRedirectUris())
                .status(OAuthClient.Status.ACTIVE)
                .resourceUris(input.getResourceUris()).allowedScopes(input.getAllowedScopes())
                .credentialVersion(1L).secretHash(new BCryptPasswordEncoder(4).encode(oneTimeSecret)).build();
        Mockito.when(clients.findByClientId(trusted, "client-a")).thenAnswer(call -> {
            assertThat(open).isFalse();
            return Optional.of(committed);
        });
        var phaseOwners = new StaticListableBeanFactory();
        phaseOwners.addBean("OAuthClientServiceImpl", owners);
        phaseOwners.addBean("oAuthClientRepository", clients);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var component = new IsolatedOAuthProvisioningComponent(() -> { },
                    Mockito.mock(IdpBootstrapService.class),
                    identities, Mockito.mock(IdentityDirectoryContext.class),
                    Mockito.mock(TenantService.class),
                    Mockito.mock(TenantMembershipService.class),
                    factory.getValidator(), strictMapper(), realms,
                    Mockito.mock(OAuthSigningKeyRepository.class),
                    Mockito.mock(OAuthKeyCipherComponent.class),
                    transactions, Clock.systemUTC(),
                    phaseOwners.getBeanProvider(OAuthClientService.class),
                    phaseOwners.getBeanProvider(ResourceServerService.class),
                    phaseOwners.getBeanProvider(OAuthSecurityProjectionComponent.class),
                    phaseOwners.getBeanProvider(OAuthClientRepository.class),
                    Mockito.mock(top.egon.cola.platform.tianquan.shoubing.core.port.PasswordCredentialStore.class),
                    Mockito.mock(top.egon.cola.platform.tianquan.shoubing.core.port.PasswordHashPort.class));
            assertThatThrownBy(() -> component.provisionClient(manifest, trusted, trusted, input, "101",
                    temporaryRepository.resolve("foreign.secret"))).hasMessage("XINGYUAN_TARGET_UNSAFE");
            Mockito.verifyNoInteractions(owners, clients, transactions);
            assertThat(component.provisionClient(manifest, trusted, trusted, input, "101", secretFile)).isSameAs(committed);
            assertThat(component.provisionClient(manifest, trusted, trusted, input, "101", secretFile, true)).isSameAs(committed);
            Files.writeString(secretFile, "mismatched-private-file");
            assertThatThrownBy(() -> component.provisionClient(manifest, trusted, trusted, input, "101", secretFile, true))
                    .hasMessage("XINGYUAN_COMMIT_UNKNOWN");
            Files.writeString(secretFile, oneTimeSecret);
            input.setClientId("client-b").setAppId("client-b-app");
            expectedFile.set(secrets.resolve("client-b.secret"));
            assertThatThrownBy(() -> component.provisionClient(manifest, trusted, trusted, input, "101", expectedFile.get()))
                    .hasMessage("XINGYUAN_COMMIT_UNKNOWN");
            assertThat(Files.exists(expectedFile.get())).isTrue();
            Mockito.verify(owners, Mockito.times(2)).create(
                    ArgumentMatchers.eq(trusted), ArgumentMatchers.eq(trusted),
                    ArgumentMatchers.any(), ArgumentMatchers.eq("101"));
            Mockito.verify(transactions, Mockito.times(2)).commit(ArgumentMatchers.any());
        }
    }

    private IsolatedOAuthProvisioningComponent realmComponent(Validator validator,
            OAuthRealmRepository realms,
            OAuthSigningKeyRepository keys,
            OAuthKeyCipherComponent cipher,
            PlatformTransactionManager transactions, Runnable guard) {
        return new IsolatedOAuthProvisioningComponent(guard,
                Mockito.mock(IdpBootstrapService.class),
                Mockito.mock(IdentityUserStore.class),
                Mockito.mock(IdentityDirectoryContext.class),
                Mockito.mock(TenantService.class),
                Mockito.mock(TenantMembershipService.class),
                validator, strictMapper(), realms, keys, cipher, transactions, Clock.systemUTC(),
                    new StaticListableBeanFactory().getBeanProvider(OAuthClientService.class),
                    new StaticListableBeanFactory().getBeanProvider(ResourceServerService.class),
                    new StaticListableBeanFactory().getBeanProvider(OAuthSecurityProjectionComponent.class),
                    new StaticListableBeanFactory().getBeanProvider(OAuthClientRepository.class),
                    Mockito.mock(top.egon.cola.platform.tianquan.shoubing.core.port.PasswordCredentialStore.class),
                    Mockito.mock(top.egon.cola.platform.tianquan.shoubing.core.port.PasswordHashPort.class));
    }

    @Test
    void aPreparedDatabaseReceiptCannotChangeRunOidOrOwnerComment() {
        var manifest = intent();
        String digest = "a".repeat(64);
        var valid = IsolatedDatabaseReceiptDTO.builder().runId(manifest.getRunId()).manifestSha256(digest)
                .role("idp").databaseName(DATABASE_NAMES.get("idp")).oid(42L)
                .ownerComment("xingyuan-it:" + manifest.getRunId() + ":" + digest).databaseOwner("fixture-owner").build();
        for (String changed : List.of("run", "digest", "role", "database", "oid", "comment")) {
            var receipt = IsolatedDatabaseReceiptDTO.builder().runId(valid.getRunId())
                    .manifestSha256(valid.getManifestSha256()).role(valid.getRole()).databaseName(valid.getDatabaseName())
                    .oid(valid.getOid()).ownerComment(valid.getOwnerComment()).databaseOwner(valid.getDatabaseOwner()).build();
            switch (changed) {
                case "run" -> receipt.setRunId(UUID.randomUUID().toString());
                case "digest" -> receipt.setManifestSha256("b".repeat(64));
                case "role" -> receipt.setRole("rbac");
                case "database" -> receipt.setDatabaseName("egon_cola");
                case "oid" -> receipt.setOid(0L);
                case "comment" -> receipt.setOwnerComment("foreign-run");
                default -> throw new AssertionError(changed);
            }
            assertThatThrownBy(() -> validateDatabaseReceipt(manifest, digest, "idp", receipt))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
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

    static Runnable ownershipGuard(Path manifestFile, Path receiptFile, Path repository, String role,
                                   DataSource physicalDataSource) throws IOException {
        // Validate the protected inputs before the callback can establish any connection.
        var initial = readVerifiedDatabaseInputs(manifestFile, receiptFile, repository, role);
        return () -> {
            try {
                var current = readVerifiedDatabaseInputs(manifestFile, receiptFile, repository, role);
                if (!initial.getManifestSha256().equals(current.getManifestSha256())
                        || initial.getReceipt().getOid() != current.getReceipt().getOid()
                        || !initial.getReceipt().getDatabaseOwner().equals(current.getReceipt().getDatabaseOwner())) {
                    throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                }
                try (var connection = physicalDataSource.getConnection()) {
                    verifyDatabaseConnection(connection, current.getReceipt());
                }
            } catch (IOException | SQLException unavailable) {
                throw new IllegalStateException("XINGYUAN_DEPENDENCY_UNAVAILABLE");
            }
        };
    }

    static void verifyRedisOwnership(org.redisson.api.RedissonClient redis,
                                     IsolatedIntegrationManifestDTO manifest, String manifestSha256) {
        var config = redis.getConfig();
        if (config == null || config.isClusterConfig() || config.isSentinelConfig()) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var single = config.useSingleServer();
        if (single == null || !manifest.getRedis().getEndpoint().equals(single.getAddress())
                || manifest.getRedis().getDatabase() != single.getDatabase()
                || manifestSha256 == null || !manifestSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        String expected = "xingyuan-it:" + manifest.getRunId() + ":" + manifestSha256;
        if (!expected.equals(redis.<String>getBucket("xingyuan-it:owner", org.redisson.client.codec.StringCodec.INSTANCE).get())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
    }

    @Test
    void redisOwnershipRequiresTheExactDatabaseEndpointAndImmutableRunMarker() {
        var manifest = intent();
        String digest = "a".repeat(64);
        var redis = Mockito.mock(org.redisson.api.RedissonClient.class);
        var config = new org.redisson.config.Config();
        config.useSingleServer().setAddress(manifest.getRedis().getEndpoint()).setDatabase(manifest.getRedis().getDatabase());
        Mockito.when(redis.getConfig()).thenReturn(config);
        @SuppressWarnings("unchecked")
        org.redisson.api.RBucket<String> marker = Mockito.mock(org.redisson.api.RBucket.class);
        Mockito.when(redis.<String>getBucket("xingyuan-it:owner", org.redisson.client.codec.StringCodec.INSTANCE)).thenReturn(marker);
        Mockito.when(marker.get()).thenReturn("xingyuan-it:foreign:" + digest);
        assertThatThrownBy(() -> verifyRedisOwnership(redis, manifest, digest))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("XINGYUAN_TARGET_UNSAFE");
        Mockito.when(marker.get()).thenReturn("xingyuan-it:" + manifest.getRunId() + ":" + digest);
        verifyRedisOwnership(redis, manifest, digest);
        Mockito.clearInvocations(marker);
        config.useSingleServer().setDatabase(0);
        assertThatThrownBy(() -> verifyRedisOwnership(redis, manifest, digest)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        Mockito.verifyNoInteractions(marker);
        config.useSingleServer().setDatabase(manifest.getRedis().getDatabase()).setAddress("redis://redis.example:6379");
        assertThatThrownBy(() -> verifyRedisOwnership(redis, manifest, digest)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        Mockito.verifyNoInteractions(marker);
    }

    static DataSource guardPhysicalDataSource(DataSource physicalDataSource,
                                                        IsolatedDatabaseReceiptDTO receipt) {
        return new GuardedPhysicalDataSource(physicalDataSource, receipt);
    }

    static DataSource physicalDataSource(VerifiedDatabaseInputsBO inputs) throws IOException {
        validateDatabaseReceipt(inputs.getManifest(), inputs.getManifestSha256(), "idp", inputs.getReceipt());
        String expectedUrl = "jdbc:postgresql://127.0.0.1:5432/" + DATABASE_NAMES.get("idp");
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

    @Test
    void protectedCredentialsPreserveSpacesAndEmbeddedNewlines() throws Exception {
        Path file = temporaryRepository.resolve("credential.txt");
        Files.writeString(file, "long 密码 \ninside \r\n");
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        char[] characters = readProtectedCharacters(file, temporaryRepository, 4096);
        try {
            assertThat(characters).containsExactly("long 密码 \ninside ".toCharArray());
        } finally {
            Arrays.fill(characters, '\0');
        }
    }

    @lombok.RequiredArgsConstructor
    private static class GuardedPhysicalDataSource extends AbstractDataSource {
        private final DataSource delegate;
        private final IsolatedDatabaseReceiptDTO receipt;

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

    @TestConfiguration(proxyBeanMethods = false)
    public static class GuardedDataSourceConfiguration {
        @Bean("isolatedPhysicalDataSource")
        DataSource isolatedPhysicalDataSource(
                @Qualifier("isolatedVerifiedDatabaseInputs")
                VerifiedDatabaseInputsBO inputs) throws IOException {
            return physicalDataSource(inputs);
        }

        @Bean("isolatedOwnershipGuard")
        Runnable isolatedOwnershipGuard(
                @Qualifier("isolatedPhysicalDataSource")
                DataSource physicalDataSource,
                Environment environment,
                @Qualifier("isolatedVerifiedDatabaseInputs") VerifiedDatabaseInputsBO inputs,
                @Qualifier("rbac3RuntimeRedissonClient")
                org.springframework.beans.factory.ObjectProvider<org.redisson.api.RedissonClient> redisClients) throws IOException {
            Runnable databaseGuard = ownershipGuard(Path.of(environment.getRequiredProperty("xingyuan.isolated.manifest-file")),
                    Path.of(environment.getRequiredProperty("xingyuan.isolated.receipt-file")),
                    Path.of(environment.getRequiredProperty("xingyuan.isolated.repository-root")), "idp", physicalDataSource);
            boolean requireRedis = environment.getProperty("xingyuan.isolated.require-redis-owner", Boolean.class, false);
            return () -> {
                databaseGuard.run();
                if (requireRedis) {
                    var redis = redisClients.getIfAvailable();
                    if (redis == null) {
                        throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE");
                    }
                    verifyRedisOwnership(redis, inputs.getManifest(), inputs.getManifestSha256());
                }
            };
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class OAuthRedisConfiguration {
        @Bean(name = "rbac3RuntimeRedissonClient", destroyMethod = "shutdown")
        org.redisson.api.RedissonClient redisClient(
                @Qualifier("isolatedVerifiedDatabaseInputs") VerifiedDatabaseInputsBO inputs,
                @Qualifier("isolatedPhysicalDataSource") DataSource physical,
                Environment environment) throws IOException {
            requireOptIn(System.getenv("XINGYUAN_ISOLATED_INTEGRATION"));
            ownershipGuard(Path.of(environment.getRequiredProperty("xingyuan.isolated.manifest-file")),
                    Path.of(environment.getRequiredProperty("xingyuan.isolated.receipt-file")),
                    Path.of(environment.getRequiredProperty("xingyuan.isolated.repository-root")), "idp", physical).run();
            char[] password = readProtectedCharacters(Path.of(inputs.getManifest().getRedis().getCredentialFile()), null, 4096);
            try {
                var config = new org.redisson.config.Config().setThreads(2).setNettyThreads(2);
                config.useSingleServer().setAddress(inputs.getManifest().getRedis().getEndpoint())
                        .setDatabase(inputs.getManifest().getRedis().getDatabase()).setPassword(new String(password))
                        .setClientName("xingyuan-it:" + inputs.getManifest().getRunId())
                        .setConnectTimeout(1000).setTimeout(500).setRetryAttempts(0)
                        .setConnectionMinimumIdleSize(1).setConnectionPoolSize(2)
                        .setSubscriptionConnectionMinimumIdleSize(1).setSubscriptionConnectionPoolSize(2);
                var redis = org.redisson.Redisson.create(config);
                try {
                    verifyRedisOwnership(redis, inputs.getManifest(), inputs.getManifestSha256());
                    return redis;
                } catch (RuntimeException rejected) {
                    redis.shutdown();
                    throw rejected;
                }
            } finally {
                Arrays.fill(password, '\0');
            }
        }

        @Bean("oauthRevocationProperties")
        top.egon.cola.platform.tianquan.shoubing.admin.token.config.OAuthRevocationProperties revocationProperties(
                @Qualifier("isolatedVerifiedDatabaseInputs") VerifiedDatabaseInputsBO inputs) {
            var properties = top.egon.cola.platform.tianquan.shoubing.admin.token.config.OAuthRevocationProperties.builder()
                    .primaryEndpoint(URI.create(inputs.getManifest().getRedis().getEndpoint()))
                    .epoch(inputs.getManifest().getRunId()).build();
            properties.validateValues();
            return properties;
        }

        @Bean("oauthPublicOrigin")
        URI publicOrigin(@Qualifier("isolatedVerifiedDatabaseInputs") VerifiedDatabaseInputsBO inputs) {
            return URI.create(inputs.getManifest().getPublicOrigin());
        }

        @Bean("idpSecureRandom")
        java.security.SecureRandom secureRandom() {
            return new java.security.SecureRandom();
        }
    }

    static AnnotationConfigApplicationContext openIdentityContext(
            Path manifestFile, Path receiptFile, Path repository) throws IOException {
        requireOptIn(System.getenv("XINGYUAN_ISOLATED_INTEGRATION"));
        var inputs = readVerifiedDatabaseInputs(manifestFile, receiptFile, repository, "idp");
        var context = new AnnotationConfigApplicationContext();
        context.setBeanNameGenerator(new org.springframework.context.annotation.AnnotationBeanNameGenerator());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("isolatedIdentityInputs", Map.of(
                "xingyuan.isolated.manifest-file", manifestFile.toString(),
                "xingyuan.isolated.receipt-file", receiptFile.toString(),
                "xingyuan.isolated.repository-root", repository.toString(),
                "egon.tianquan-shoubing.identity-directory.platform-storage-partition-id",
                inputs.getManifest().getPlatformStoragePartitionId(),
                "egon.cola.component.id.machine-id", "711",
                "mybatis-plus.mapper-locations", "classpath*:mapper/**/*.xml")));
        context.registerBean("isolatedVerifiedDatabaseInputs", VerifiedDatabaseInputsBO.class, () -> inputs);
        context.register(IdentityPersistenceConfiguration.class);
        try {
            context.refresh();
            return context;
        } catch (RuntimeException failedInitialization) {
            context.close();
            throw failedInitialization;
        }
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void provisionIdentity() {
        try {
            Path repository = Path.of(requiredOperationProperty("xingyuan.isolated.repository-root"));
            Path manifestFile = Path.of(requiredOperationProperty("xingyuan.isolated.manifest-file"));
            Path receiptFile = Path.of(requiredOperationProperty("xingyuan.isolated.receipt-file"));
            Path resultFile = Path.of(requiredOperationProperty("xingyuan.isolated.result-file"));
            String resumeValue = System.getProperty("xingyuan.isolated.resume", "false");
            if (!Set.of("true", "false").contains(resumeValue)) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
            }
            var inputs = readVerifiedDatabaseInputs(manifestFile, receiptFile, repository, "idp");
            Path taskRoot = Path.of(inputs.getManifest().getRuntimeRoot());
            if (Files.exists(resultFile, LinkOption.NOFOLLOW_LINKS)) {
                requireMatchingCheckpoint(resultFile, taskRoot.resolve("evidence"), inputs, "COMMITTED");
                if (!Boolean.parseBoolean(resumeValue)) {
                    throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
                }
            } else {
                validatePrivateSecretDestination(resultFile, taskRoot.resolve("evidence"));
            }
            var references = inputs.getManifest().getPolicyFiles().stream()
                    .filter(reference -> Path.of(reference.getPath()).getFileName().toString().equals("identity-provisioning.json")).toList();
            if (references.size() != 1) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE");
            }
            var recipe = readIdentityRecipe(inputs.getManifest(), references.getFirst(), repository);
            if (recipe.getCliResources().isEmpty()) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE");
            }
            var passwords = new java.util.LinkedHashMap<String, Path>();
            for (String key : List.of("admin", "viewer", "provisioner")) {
                String reference = inputs.getManifest().getSecretFiles().get(key + "-password");
                if (reference == null) {
                    throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE");
                }
                Path passwordFile = Path.of(reference);
                char[] password = readProtectedCharacters(passwordFile, taskRoot.resolve("secrets"), 4096);
                try {
                    if (password.length < 12) {
                        throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
                    }
                } finally {
                    Arrays.fill(password, '\0');
                }
                passwords.put(key, passwordFile);
            }
            var command = IsolatedPhaseCommandDTO.builder().manifest(inputs.getManifest()).recipe(recipe)
                    .passwordFiles(Map.copyOf(passwords)).resume(Boolean.parseBoolean(resumeValue)).build();
            List<IsolatedStepResultDTO> stages = new java.util.ArrayList<>();
            try (var context = openIdentityContext(manifestFile, receiptFile, repository)) {
                var owner = context.getBean("isolatedOAuthProvisioningComponent", IsolatedOAuthProvisioningComponent.class);
                for (var stage : List.of(ProvisionStageEnum.IDENTITY, ProvisionStageEnum.REALMS)) {
                    runProvisioningStage(owner, command, stage, inputs, stages);
                }
            }
            try (var context = openOAuthContext(manifestFile, receiptFile, repository, command.getIdentityFacts().getTenantIds())) {
                var owner = context.getBean("isolatedOAuthProvisioningComponent", IsolatedOAuthProvisioningComponent.class);
                for (var stage : List.of(ProvisionStageEnum.SECURITY, ProvisionStageEnum.MANAGEMENT_CLIENTS, ProvisionStageEnum.RESOURCES,
                        ProvisionStageEnum.CLIENTS, ProvisionStageEnum.GRANTS)) {
                    runProvisioningStage(owner, command, stage, inputs, stages);
                }
                var guard = context.getBean("isolatedOwnershipGuard", Runnable.class);
                guard.run();
                var registry = buildRuntimeRegistry(command, inputs.getManifestSha256());
                Path registryFile = taskRoot.resolve("secrets/runtime-registry.json");
                if (Files.exists(registryFile, LinkOption.NOFOLLOW_LINKS)) {
                    var previous = strictMapper().readValue(readProtectedBytes(registryFile, taskRoot.resolve("secrets"), 2 * 1024 * 1024),
                            IsolatedRuntimeRegistryDTO.class);
                    if (!command.isResume() || !registry.equals(previous)) {
                        throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
                    }
                } else {
                    publishProtectedBytes(registryFile, taskRoot.resolve("secrets"), strictMapper().writeValueAsBytes(registry), 2 * 1024 * 1024);
                }
                guard.run();
                var report = provisioningReport(command, inputs, "COMMITTED", stages);
                if (Files.exists(resultFile, LinkOption.NOFOLLOW_LINKS)) {
                    requireMatchingCheckpoint(resultFile, taskRoot.resolve("evidence"), inputs, "COMMITTED");
                    if (!command.isResume()) {
                        throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
                    }
                } else {
                    publishResult(resultFile, taskRoot.resolve("evidence"), report);
                }
            }
        } catch (IOException | RuntimeException failure) {
            String code = failure.getMessage();
            if (code == null || !code.matches("XINGYUAN_(INPUT_INVALID|TARGET_UNSAFE|INPUT_UNAVAILABLE|STATE_CONFLICT|DEPENDENCY_UNAVAILABLE|PARTIAL|COMMIT_UNKNOWN)")) {
                code = failure instanceof org.springframework.transaction.TransactionException
                        ? "XINGYUAN_COMMIT_UNKNOWN" : "XINGYUAN_DEPENDENCY_UNAVAILABLE";
            }
            throw new IllegalStateException(code);
        }
    }

    private static String requiredOperationProperty(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE");
        }
        return value;
    }

    private static void runProvisioningStage(IsolatedOAuthProvisioningComponent owner, IsolatedPhaseCommandDTO command,
                                            ProvisionStageEnum stage, VerifiedDatabaseInputsBO inputs,
                                            List<IsolatedStepResultDTO> stages) throws IOException {
        Path evidence = Path.of(command.getManifest().getRuntimeRoot()).resolve("evidence");
        Path checkpoint = evidence.resolve("identity-" + stage.name().toLowerCase(java.util.Locale.ROOT) + ".json");
        boolean previous = Files.exists(checkpoint, LinkOption.NOFOLLOW_LINKS);
        if (previous) {
            if (!command.isResume()) {
                throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
            }
            var recorded = requireMatchingCheckpoint(checkpoint, evidence, inputs, "PARTIAL");
            if (recorded.getSteps().size() != 1 || !stage.name().equals(recorded.getSteps().getFirst().getStepId())
                    || !"COMMITTED".equals(recorded.getSteps().getFirst().getState())) {
                throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
            }
        }
        command.setStage(stage);
        owner.executePhase(command);
        var step = IsolatedStepResultDTO.builder().stepId(stage.name()).state("COMMITTED")
                .evidence("evidence/" + checkpoint.getFileName()).build();
        if (!previous) {
            publishResult(checkpoint, evidence, provisioningReport(command, inputs, "PARTIAL", List.of(step)));
        }
        stages.add(step);
    }

    static IsolatedIntegrationResult requireMatchingCheckpoint(Path file, Path evidence, VerifiedDatabaseInputsBO inputs,
                                                               String status) throws IOException {
        var recorded = strictMapper().readValue(readProtectedBytes(file, evidence, 128 * 1024), IsolatedIntegrationResult.class);
        if (recorded.getSchemaVersion() != 1 || !"CLI-002".equals(recorded.getOperationId())
                || !inputs.getManifest().getRunId().equals(recorded.getRunId())
                || !inputs.getManifestSha256().equals(recorded.getManifestSha256()) || !status.equals(recorded.getStatus())) {
            throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
        }
        return recorded;
    }

    private static IsolatedIntegrationResult provisioningReport(IsolatedPhaseCommandDTO command, VerifiedDatabaseInputsBO inputs,
                                                                 String status, List<IsolatedStepResultDTO> stages) {
        var source = IsolatedProvisioningBO.builder().schemaVersion(1).operationId("CLI-002")
                .runId(command.getManifest().getRunId()).manifestSha256(inputs.getManifestSha256()).status(status)
                .completedAt(Instant.now()).steps(List.copyOf(stages))
                .targets(List.of(IsolatedTargetResultDTO.builder().targetKey("idp").identity(inputs.getReceipt().getDatabaseName())
                        .version(Long.toString(inputs.getReceipt().getOid())).state("COMMITTED").build()))
                .counts(IsolatedCountsDTO.builder().databaseCount(1).tenantCount(command.getIdentityFacts() == null ? 0 : command.getIdentityFacts().getTenantIds().size())
                        .clientCount(command.getCommittedClients().size()).build()).build();
        return new IsolatedProvisioningResultConverterImpl().toTarget(source);
    }

    static AnnotationConfigApplicationContext openOAuthContext(
            Path manifestFile, Path receiptFile, Path repository, Map<String, String> recordedTenantIds) throws IOException {
        return openOAuthContext(manifestFile, receiptFile, repository, recordedTenantIds, false);
    }

    static AnnotationConfigApplicationContext openOAuthContext(Path manifestFile, Path receiptFile, Path repository,
            Map<String, String> recordedTenantIds, boolean oracle) throws IOException {
        requireOptIn(System.getenv("XINGYUAN_ISOLATED_INTEGRATION"));
        var inputs = readVerifiedDatabaseInputs(manifestFile, receiptFile, repository, "idp");
        Map<String, String> actualTenantIds;
        try (var identityContext = openIdentityContext(manifestFile, receiptFile, repository)) {
            var tenants = identityContext.getBean("tenantServiceImpl", TenantService.class).list();
            if (tenants.size() != 2 || tenants.stream().anyMatch(tenant -> tenant.status() != top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantBO.Status.ACTIVE)) {
                throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
            }
            actualTenantIds = tenants.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                    TenantService.TenantView::tenantCode, TenantService.TenantView::tenantId));
            if (!actualTenantIds.equals(recordedTenantIds)
                    || !actualTenantIds.keySet().equals(Set.copyOf(inputs.getManifest().getTenantCodes()))) {
                throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
            }
        }
        var context = new AnnotationConfigApplicationContext();
        context.setBeanNameGenerator(new org.springframework.context.annotation.AnnotationBeanNameGenerator());
        Map<String, Object> properties = new java.util.LinkedHashMap<>();
        properties.put("xingyuan.isolated.manifest-file", manifestFile.toString());
        properties.put("xingyuan.isolated.receipt-file", receiptFile.toString());
        properties.put("xingyuan.isolated.repository-root", repository.toString());
        properties.put("xingyuan.isolated.require-redis-owner", "true");
        properties.put("egon.tianquan-shoubing.identity-directory.platform-storage-partition-id", inputs.getManifest().getPlatformStoragePartitionId());
        properties.put("egon.tianquan-shoubing.oauth.platform-management-tenant-id", actualTenantIds.get("default"));
        properties.put("egon.tianquan-shoubing.identity-state-key-prefix", "xingyuan-it:" + inputs.getManifest().getRunId() + ":identity:");
        properties.put("egon.cola.platform.tianquan.shoubing.audit.stream", "xingyuan-it:" + inputs.getManifest().getRunId() + ":oauth:audit:v1");
        properties.put("egon.cola.component.id.machine-id", "711");
        properties.put("egon.cola.component.mybatis-plus.tenant-id.ignored-tables[0]", "egon_cola_outbox_message");
        properties.put("egon.cola.component.transactional-outbox.node-id", "xingyuan-it:" + inputs.getManifest().getRunId());
        properties.put("egon.cola.component.transactional-outbox.storage.data-source-bean-name", "dataSource");
        properties.put("egon.cola.component.transactional-outbox.storage.transaction-manager-bean-name", "transactionManager");
        properties.put("egon.cola.component.transactional-outbox.state-machine.business.enabled", "false");
        properties.put("mybatis-plus.mapper-locations", "classpath*:mapper/**/*.xml");
        if (oracle) { properties.putAll(bffOracleRuntimeProperties(inputs)); }
        properties.put("spring.profiles.active", "local");
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("isolatedOAuthInputs", properties));
        context.registerBean("isolatedVerifiedDatabaseInputs", VerifiedDatabaseInputsBO.class, () -> inputs);
        context.register(OAuthManagementConfiguration.class);
        if (oracle) { context.register(BffOracleConfiguration.class); }
        try {
            context.refresh();
            return context;
        } catch (RuntimeException failedInitialization) {
            context.close();
            throw failedInitialization;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @org.springframework.boot.context.properties.EnableConfigurationProperties(top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties.class)
    @org.springframework.boot.autoconfigure.ImportAutoConfiguration({
            top.egon.cola.component.outbox.autoconfigure.OutboxMybatisPlusAutoConfiguration.class,
            top.egon.cola.component.outbox.autoconfigure.OutboxStateMachineAutoConfiguration.class})
    @org.springframework.context.annotation.ComponentScan(basePackages = {
            "top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl",
            "top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl",
            "top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.impl",
            "top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.impl",
            "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter",
            "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter",
            "top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter",
            "top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.converter"})
    @MapperScan(basePackages = {"top.egon.cola.platform.tianquan.shoubing.admin.resource.dao",
            "top.egon.cola.platform.tianquan.shoubing.admin.audit.dao"}, annotationClass = Mapper.class)
    @Import({IdentityPersistenceConfiguration.class, OAuthRedisConfiguration.class,
            top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.TenantIssuerServiceImpl.class,
            top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver.class,
            top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthClientServiceImpl.class,
            top.egon.cola.platform.tianquan.shoubing.admin.resource.service.impl.ResourceServerServiceImpl.class,
            top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.TokenRevocationServiceImpl.class,
            OAuthSecurityProjectionComponent.class,
            top.egon.cola.platform.tianquan.shoubing.admin.support.outbox.service.IdentityOutboxPublisher.class,
            top.egon.cola.platform.tianquan.shoubing.admin.token.support.outbox.OAuthSecurityDeliveryHandler.class,
            top.egon.cola.platform.tianquan.shoubing.admin.audit.support.outbox.OAuthAuditOutboxDeliveryHandler.class,
            top.egon.cola.platform.tianquan.shoubing.admin.resource.support.outbox.ResourceServerProjectionDeliveryHandler.class})
    public static class OAuthManagementConfiguration {
        @Bean("egonColaShardingRouteFingerprint")
        String routeFingerprint(@Qualifier("isolatedOAuthTopology") EgonColaShardingTopologyValidator.TopologyBO topology) {
            return topology.fingerprint();
        }

        @Bean("outboxInfrastructure")
        top.egon.cola.component.outbox.autoconfigure.OutboxInfrastructure outboxInfrastructure(
                org.springframework.beans.factory.config.ConfigurableListableBeanFactory beans,
                top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties properties,
                @Qualifier("egonColaValidationUtils") ValidationUtils validation) {
            new top.egon.cola.component.outbox.autoconfigure.OutboxConfigurationValidator(validation).validate(properties);
            return new top.egon.cola.component.outbox.autoconfigure.OutboxInfrastructureResolver().resolve(beans, properties);
        }

        @Bean("outboxMessageValidator")
        top.egon.cola.component.outbox.validation.OutboxMessageValidator outboxMessageValidator(
                @Qualifier("jacksonObjectMapper") ObjectMapper mapper,
                top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties properties,
                @Qualifier("egonColaValidationUtils") ValidationUtils validation) {
            return new top.egon.cola.component.outbox.validation.OutboxMessageValidator(mapper,
                    Math.toIntExact(properties.getPayload().getMaxBytes().toBytes()), properties.getPayload().getMaxHeaderCount(),
                    Math.toIntExact(properties.getPayload().getMaxHeaderBytes().toBytes()), validation);
        }

        @Bean("outboxMetrics")
        top.egon.cola.component.outbox.observability.OutboxMetrics outboxMetrics() {
            return new top.egon.cola.component.outbox.observability.NoopOutboxMetrics();
        }

        @Bean("deliveryHandlerRegistry")
        top.egon.cola.component.outbox.delivery.DeliveryHandlerRegistry deliveryHandlerRegistry(
                org.springframework.beans.factory.ObjectProvider<top.egon.cola.component.outbox.delivery.DeliveryHandler> handlers) {
            return new top.egon.cola.component.outbox.delivery.DeliveryHandlerRegistry(handlers.orderedStream().toList());
        }

        @Bean("transactionalOutbox")
        top.egon.cola.component.outbox.api.TransactionalOutbox transactionalOutbox(
                @Qualifier("outboxMessageValidator") top.egon.cola.component.outbox.validation.OutboxMessageValidator validator,
                @Qualifier("outboxInfrastructure") top.egon.cola.component.outbox.autoconfigure.OutboxInfrastructure infrastructure,
                @Qualifier("outboxStore") top.egon.cola.component.outbox.store.OutboxStore store,
                @Qualifier("deliveryHandlerRegistry") top.egon.cola.component.outbox.delivery.DeliveryHandlerRegistry handlers,
                @Qualifier("outboxMetrics") top.egon.cola.component.outbox.observability.OutboxMetrics metrics,
                @Qualifier("jacksonObjectMapper") ObjectMapper mapper,
                org.springframework.context.ApplicationEventPublisher events,
                top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties properties) {
            return new top.egon.cola.component.outbox.transaction.DefaultTransactionalOutbox(validator,
                    new top.egon.cola.component.outbox.serialization.JacksonOutboxMessageSerializer(mapper),
                    new top.egon.cola.component.outbox.api.UuidOutboxIdGenerator(), mapper,
                    new top.egon.cola.component.outbox.transaction.OutboxTransactionGuard(infrastructure.dataSource()),
                    store, handlers, new top.egon.cola.component.outbox.transaction.OutboxAfterCommitBuffer(events, metrics),
                    metrics, properties);
        }

        @Bean(name = "outboxSchemaValidator", initMethod = "validate")
        top.egon.cola.component.outbox.store.OutboxSchemaValidator outboxSchemaValidator(
                @Qualifier("outboxStore") top.egon.cola.component.outbox.store.OutboxStore store,
                @Qualifier("egonColaValidationUtils") ValidationUtils validation) {
            return new top.egon.cola.component.outbox.store.OutboxSchemaValidator(store, validation);
        }

        @Bean("resourceServerProjectionService")
        top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerProjectionService resourceProjections(
                @Qualifier("rbac3RuntimeRedissonClient") org.redisson.api.RedissonClient redis,
                @Qualifier("jacksonObjectMapper") ObjectMapper mapper) {
            return new top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerProjectionService(redis, mapper);
        }

        @Bean("resourceServerEventAdapter")
        top.egon.cola.platform.tianquan.shoubing.admin.resource.support.outbox.TransactionalOutboxResourceServerEventAdapter resourceEvents(
                @Qualifier("transactionalOutbox") top.egon.cola.component.outbox.api.TransactionalOutbox outbox,
                @Qualifier("idpClock") Clock clock) {
            return new top.egon.cola.platform.tianquan.shoubing.admin.resource.support.outbox.TransactionalOutboxResourceServerEventAdapter(outbox, clock);
        }
    }

    static void requireOptIn(String value) {
        if (!"true".equals(value)) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
    }

    @Test
    void contextOptInRequiresAnExactExplicitTrue() {
        for (String value : Arrays.asList(null, "", "false", "TRUE", "1", " true")) {
            assertThatThrownBy(() -> requireOptIn(value)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
        requireOptIn("true");
    }

    @Test
    void fixedSecurityPhaseUsesOnlyTheThreePreparedRealmBindings() {
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var component = Mockito.spy(realmComponent(validators.getValidator(), Mockito.mock(OAuthRealmRepository.class),
                    Mockito.mock(OAuthSigningKeyRepository.class), Mockito.mock(OAuthKeyCipherComponent.class),
                    Mockito.mock(PlatformTransactionManager.class), () -> { }));
            var manifest = intent();
            TrustedRealmBO platform = TrustedRealmBO.builder().issuer(URI.create(manifest.getPublicOrigin() + "/platform"))
                    .realmKey("platform").realmType(RealmTypeEnum.PLATFORM)
                    .storagePartitionId(Long.parseLong(manifest.getPlatformStoragePartitionId())).trustVersion(0L).build();
            TrustedRealmBO first = TrustedRealmBO.builder().issuer(URI.create(manifest.getPublicOrigin() + "/tenants/1001"))
                    .realmKey("tenant:1001").realmType(RealmTypeEnum.TENANT).storagePartitionId(1001L).businessTenantId(1001L).trustVersion(0L).build();
            TrustedRealmBO second = TrustedRealmBO.builder().issuer(URI.create(manifest.getPublicOrigin() + "/tenants/1002"))
                    .realmKey("tenant:1002").realmType(RealmTypeEnum.TENANT).storagePartitionId(1002L).businessTenantId(1002L).trustVersion(0L).build();
            Mockito.doReturn(0L).when(component).recoverSecurity(ArgumentMatchers.eq(manifest), ArgumentMatchers.any());
            var command = IsolatedPhaseCommandDTO.builder().stage(ProvisionStageEnum.SECURITY).manifest(manifest)
                    .realms(Map.of("platform", platform, "default", first, "tenant-b", second)).build();
            component.executePhase(command);
            var ordering = Mockito.inOrder(component);
            ordering.verify(component).recoverSecurity(manifest, platform);
            ordering.verify(component).recoverSecurity(manifest, first);
            ordering.verify(component).recoverSecurity(manifest, second);
            Mockito.verify(component, Mockito.times(3)).recoverSecurity(ArgumentMatchers.eq(manifest), ArgumentMatchers.any());
        }
    }

    @Test
    void phaseDispatchCannotLookupAnOwnerAfterOwnershipFailure() {
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var transactions = Mockito.mock(PlatformTransactionManager.class);
            var realms = Mockito.mock(OAuthRealmRepository.class);
            var component = realmComponent(validators.getValidator(), realms, Mockito.mock(OAuthSigningKeyRepository.class),
                    Mockito.mock(OAuthKeyCipherComponent.class), transactions,
                    () -> { throw new IllegalStateException("XINGYUAN_TARGET_UNSAFE"); });
            var command = IsolatedPhaseCommandDTO.builder().stage(ProvisionStageEnum.SECURITY).manifest(intent()).build();
            assertThatThrownBy(() -> component.executePhase(command)).hasMessage("XINGYUAN_TARGET_UNSAFE");
            Mockito.verifyNoInteractions(realms, transactions);
        }
    }

    @Test
    void identityResumeVerifiesOriginalCredentialsAndKeepsCommittedFactsWithoutWrites() throws Exception {
        var manifest = intent();
        Path secretRoot = Files.createDirectories(Path.of(manifest.getRuntimeRoot()).resolve("secrets"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Map<String, Path> passwords = Map.of("admin", secretRoot.resolve("admin.password"), "viewer", secretRoot.resolve("viewer.password"),
                "provisioner", secretRoot.resolve("provisioner.password"));
        for (Path file : passwords.values()) {
            Files.writeString(file, "controlled-password-123456");
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        }
        var bootstrap = Mockito.mock(IdpBootstrapService.class);
        var identities = Mockito.mock(IdentityUserStore.class);
        var credentials = Mockito.mock(top.egon.cola.platform.tianquan.shoubing.core.port.PasswordCredentialStore.class);
        var hashes = Mockito.mock(top.egon.cola.platform.tianquan.shoubing.core.port.PasswordHashPort.class);
        var directory = Mockito.mock(IdentityDirectoryContext.class);
        var tenants = Mockito.mock(TenantService.class);
        var memberships = Mockito.mock(TenantMembershipService.class);
        var transactions = Mockito.mock(PlatformTransactionManager.class);
        Mockito.when(directory.directoryPartitionId()).thenReturn(Long.parseLong(manifest.getPlatformStoragePartitionId()));
        Instant now = Instant.parse("2026-10-14T00:00:00Z");
        for (var user : Map.of("alice", "101", "viewer", "102", "xingyuan-it-provisioner", "103").entrySet()) {
            Mockito.when(identities.findByNormalizedUsername(user.getKey())).thenReturn(Optional.of(
                    new IdentityUser(user.getValue(), user.getKey(), user.getKey(), user.getKey(), IdentityUserStatus.ACTIVE,
                            0, null, null, 0L)));
            Mockito.when(credentials.findActive(user.getValue())).thenReturn(Optional.of(
                    new top.egon.cola.platform.tianquan.shoubing.core.identity.PasswordCredential(user.getValue(), "stored-hash", now,
                            false, top.egon.cola.platform.tianquan.shoubing.core.identity.PasswordCredential.Status.ACTIVE, 0L)));
        }
        Mockito.when(hashes.matches(ArgumentMatchers.any(char[].class), ArgumentMatchers.eq("stored-hash"))).thenReturn(true);
        var first = new TenantService.TenantView("1001", "default", "Isolated default",
                top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantBO.Status.ACTIVE, "{}", 1L, now, now);
        var second = new TenantService.TenantView("1002", "tenant-b", "Isolated tenant-b",
                top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantBO.Status.ACTIVE, "{}", 1L, now, now);
        Mockito.when(tenants.list()).thenReturn(List.of(first, second));
        for (var tenant : List.of(first, second)) {
            Mockito.when(tenants.get(tenant.tenantId())).thenReturn(tenant);
            Mockito.when(memberships.listByTenant(tenant.tenantId())).thenReturn(List.of(
                    new TenantMembershipService.MembershipView(tenant.tenantId(), "101", "alice",
                            top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantMembershipBO.Status.ACTIVE, 0L, now),
                    new TenantMembershipService.MembershipView(tenant.tenantId(), "102", "viewer",
                            top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantMembershipBO.Status.ACTIVE, 0L, now),
                    new TenantMembershipService.MembershipView(tenant.tenantId(), "103", "xingyuan-it-provisioner",
                            top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantMembershipBO.Status.ACTIVE, 0L, now)));
            for (String subject : List.of("101", "102", "103")) {
                Mockito.when(memberships.resolve(subject, tenant.tenantId())).thenReturn(new TenantMembershipService.TenantMembershipProfile(
                        subject, tenant.tenantId(), tenant.tenantName(), subject,
                        top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantBO.Status.ACTIVE,
                        IdentityUserStatus.ACTIVE,
                        top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantMembershipBO.Status.ACTIVE,
                        top.egon.cola.platform.tianquan.shoubing.core.port.TenantMembershipPort.MembershipStatus.ACTIVE, 0L, now));
            }
        }
        var optional = new StaticListableBeanFactory();
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var component = new IsolatedOAuthProvisioningComponent(() -> { }, bootstrap, identities, directory, tenants, memberships,
                    validators.getValidator(), strictMapper(), Mockito.mock(OAuthRealmRepository.class),
                    Mockito.mock(OAuthSigningKeyRepository.class), Mockito.mock(OAuthKeyCipherComponent.class), transactions, Clock.systemUTC(),
                    optional.getBeanProvider(OAuthClientService.class), optional.getBeanProvider(ResourceServerService.class),
                    optional.getBeanProvider(OAuthSecurityProjectionComponent.class), optional.getBeanProvider(OAuthClientRepository.class),
                    credentials, hashes);
            var resumed = org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> component.bootstrapIdentityAndTenants(manifest, passwords, true));
            assertThat(resumed.getTenantIds()).containsExactlyInAnyOrderEntriesOf(Map.of("default", "1001", "tenant-b", "1002"));
            assertThat(resumed.getSubjects()).containsExactlyInAnyOrderEntriesOf(Map.of("admin", "101", "viewer", "102"));
            assertThat(resumed.getProvisioningSubject()).isEqualTo("103");
            Mockito.verify(hashes, Mockito.times(3)).matches(ArgumentMatchers.any(char[].class), ArgumentMatchers.eq("stored-hash"));
            Mockito.when(hashes.matches(ArgumentMatchers.any(char[].class), ArgumentMatchers.eq("stored-hash"))).thenReturn(false);
            assertThatThrownBy(() -> component.bootstrapIdentityAndTenants(manifest, passwords, true)).hasMessage("XINGYUAN_STATE_CONFLICT");
            Mockito.verifyNoInteractions(bootstrap, transactions);
            Mockito.verify(tenants, Mockito.never()).create(ArgumentMatchers.any());
            Mockito.verify(tenants, Mockito.never()).update(ArgumentMatchers.anyString(), ArgumentMatchers.any());
            Mockito.verify(memberships, Mockito.never()).upsert(ArgumentMatchers.any());
            Mockito.when(hashes.matches(ArgumentMatchers.any(char[].class), ArgumentMatchers.eq("stored-hash"))).thenReturn(true);
            for (var user : Map.of("alice", "101", "viewer", "102", "xingyuan-it-provisioner", "103").entrySet()) {
                Mockito.when(identities.findByNormalizedUsername(user.getKey())).thenReturn(Optional.empty(), Optional.of(
                        new IdentityUser(user.getValue(), user.getKey(), user.getKey(), user.getKey(), IdentityUserStatus.ACTIVE,
                                0, null, null, 0L)));
            }
            Mockito.when(tenants.list()).thenReturn(List.of());
            Mockito.when(tenants.create(ArgumentMatchers.any())).thenAnswer(call -> {
                TenantService.CreateTenantCommand input = call.getArgument(0);
                var selected = input.tenantCode().equals("default") ? first : second;
                return new TenantService.TenantView(selected.tenantId(), selected.tenantCode(), selected.tenantName(),
                        top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantBO.Status.INITIALIZING,
                        "{}", 0L, now, now);
            });
            for (var tenant : List.of(first, second)) {
                Mockito.when(tenants.update(ArgumentMatchers.eq(tenant.tenantId()), ArgumentMatchers.any())).thenReturn(tenant);
                Mockito.when(memberships.listByTenant(tenant.tenantId())).thenReturn(List.of());
            }
            var fresh = component.bootstrapIdentityAndTenants(manifest, passwords, false);
            assertThat(fresh.getProvisioningSubject()).isEqualTo("103");
            assertThat(fresh.getSubjects()).containsExactlyInAnyOrderEntriesOf(Map.of("admin", "101", "viewer", "102"));
            for (String username : List.of("alice", "viewer", "xingyuan-it-provisioner")) {
                Mockito.verify(bootstrap).bootstrap(ArgumentMatchers.eq(username), ArgumentMatchers.any(char[].class));
            }
            Mockito.verifyNoMoreInteractions(bootstrap);
            Mockito.verify(memberships, Mockito.times(6)).upsert(ArgumentMatchers.any());
            Mockito.clearInvocations(tenants, memberships);
            Mockito.when(identities.findByNormalizedUsername("xingyuan-it-provisioner")).thenReturn(Optional.of(
                    new IdentityUser("101", "xingyuan-it-provisioner", "xingyuan-it-provisioner", "xingyuan-it-provisioner", IdentityUserStatus.ACTIVE,
                            0, null, null, 0L)));
            assertThatThrownBy(() -> component.bootstrapIdentityAndTenants(manifest, passwords, true)).hasMessage("XINGYUAN_STATE_CONFLICT");
            Mockito.verifyNoInteractions(tenants, memberships);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @MapperScan(basePackages = {
            "top.egon.cola.platform.tianquan.shoubing.admin.identity.dao",
            "top.egon.cola.platform.tianquan.shoubing.admin.tenant.dao",
            "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao",
            "top.egon.cola.platform.tianquan.shoubing.admin.token.dao"},
            annotationClass = Mapper.class)
    @Import({GuardedDataSourceConfiguration.class, ValidationAutoConfiguration.class,
            IdGeneratorAutoConfiguration.class, EgonColaMybatisPlusAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class, OAuthPersistenceConfiguration.class, OAuthManagedDdlComponent.class,
            IdentityDirectoryContext.class,
            IdentityUserRepository.class,
            IdentityCredentialRepository.class,
            IdentityUserConverterImpl.class,
            IdentityCredentialConverterImpl.class,
            IdentityPersistenceAdapter.class,
            MpIdentityUserDirectory.class,
            SpringPasswordHashAdapter.class,
            IdpBootstrapService.class,
            IdentityTenantRepository.class,
            IdentityTenantMembershipRepository.class,
            IdentityTenantConverterImpl.class,
            IdentityTenantMembershipConverterImpl.class,
            OAuthRealmRepositoryImpl.class,
            OAuthRealmConverterImpl.class,
            OAuthSigningKeyRepositoryImpl.class,
            OAuthSigningKeyConverterImpl.class,
            OAuthPayloadCipherComponent.class,
            OAuthKeyCipherComponent.class,
            IsolatedOAuthProvisioningComponent.class})
    public static class IdentityPersistenceConfiguration {

        @Bean("idpClock")
        @Primary
        Clock idpClock() {
            return Clock.systemUTC();
        }

        @Bean({"jacksonObjectMapper", "outboxStateMachineObjectMapper"})
        ObjectMapper jacksonObjectMapper() {
            return strictMapper();
        }

        @Bean("oauthKeyEncryptionProperties")
        OAuthKeyEncryptionProperties keyEncryptionProperties(
                @Qualifier("isolatedVerifiedDatabaseInputs") VerifiedDatabaseInputsBO inputs) throws IOException {
            String file = inputs.getManifest().getSecretFiles().get("oauth-kek");
            if (file == null) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE");
            }
            Path keyFile = Path.of(file);
            byte[] key = readProtectedBytes(keyFile, Path.of(inputs.getManifest().getRuntimeRoot()).resolve("secrets"), 32);
            try {
                if (key.length != 32) {
                    throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
                }
            } finally {
                Arrays.fill(key, (byte) 0);
            }
            return OAuthKeyEncryptionProperties.builder()
                    .keyId("xingyuan-it").keyFile(keyFile).build();
        }

        @Bean("egonColaShardingTopologyValidator")
        EgonColaShardingTopologyValidator topologyValidator(
                @Qualifier("egonColaValidationUtils") ValidationUtils validation) {
            return new EgonColaShardingTopologyValidator(validation);
        }

        @Bean("egon.cola.component.mybatis-plus.sharding-top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties")
        EgonColaShardingProperties shardingProperties(
                @Qualifier("isolatedVerifiedDatabaseInputs") VerifiedDatabaseInputsBO inputs) {
            return EgonColaShardingProperties.builder().mode(EgonColaShardingProperties.ModeEnum.SHARDING)
                    .configStyle(EgonColaShardingProperties.ConfigStyleEnum.NATIVE)
                    .nativeRulesResource("classpath:oauth-sharding-native.yml")
                    .dataSources(List.of(new EgonColaShardingProperties.PhysicalDataSourceProperties(
                            "oauth_primary", "oauth_primary", EgonColaShardingProperties.DataSourceRoleEnum.PRIMARY,
                            "org.postgresql.Driver", inputs.getJdbcUrl(), inputs.getReceipt().getDatabaseOwner(), "")))
                    .build();
        }

        @Bean("isolatedOAuthTopology")
        EgonColaShardingTopologyValidator.TopologyBO isolatedOAuthTopology(
                @Qualifier("egonColaShardingTopologyValidator") EgonColaShardingTopologyValidator validator,
                EgonColaShardingProperties properties) throws IOException {
            try (var yaml = new ClassPathResource("oauth-sharding-native.yml").getInputStream()) {
                return validator.validate(properties, yaml.readAllBytes());
            }
        }

        @Bean("outboxManagedDdlInitializer")
        OutboxManagedDdlInitializer outboxManagedDdlInitializer(
                @Qualifier("egonColaPostgreDdlRunner") EgonColaPostgreDdlRunner runner,
                @Qualifier("egonColaShardingTopologyValidator") EgonColaShardingTopologyValidator topology,
                @Qualifier("egonColaValidationUtils") ValidationUtils validation,
                EgonColaShardingProperties properties,
                @Qualifier("outboxStateMachineObjectMapper") ObjectMapper mapper) {
            return new OutboxManagedDdlInitializer(runner, topology, validation, properties,
                    OutboxMpStorageProperties.builder().build(), mapper);
        }

        @Bean("dataSource")
        @Primary
        DataSource dataSource(
                @Qualifier("isolatedPhysicalDataSource") DataSource physical,
                @Qualifier("isolatedOwnershipGuard") Runnable guard,
                @Qualifier("egonColaShardingLogicalDataSourceFactory")
                EgonColaShardingDataSourceBootstrapper.LogicalDataSourceFactory factory,
                @Qualifier("isolatedOAuthTopology") EgonColaShardingTopologyValidator.TopologyBO topology) throws Exception {
            guard.run();
            return factory.create(Map.of("oauth_primary", physical), topology.yaml());
        }

        @Bean("egonColaRoutingProfiles")
        Map<String, EgonColaRoutingProfileBO> routingProfiles(
                @Qualifier("isolatedOAuthTopology") EgonColaShardingTopologyValidator.TopologyBO topology) {
            return topology.profiles();
        }

        @Bean("egonColaWriteTargetResolver")
        EgonColaWriteTargetResolver writeTargetResolver(
                @Qualifier("egonColaRoutingProfiles") Map<String, EgonColaRoutingProfileBO> profiles,
                @Qualifier("egonColaTwoLevelRouteStrategy") EgonColaTwoLevelRouteStrategy strategy,
                @Qualifier("egonColaValidationUtils") ValidationUtils validation,
                @Qualifier("isolatedOAuthTopology") EgonColaShardingTopologyValidator.TopologyBO topology) {
            return new EgonColaShardingWriteTargetResolver(profiles, strategy, validation, topology.fingerprint());
        }

        @Bean("tenantServiceImpl")
        TenantService tenantService(
                @Qualifier("identityTenantRepository") IdentityTenantRepository tenants,
                @Qualifier("identityDirectoryContext") IdentityDirectoryContext directory,
                @Qualifier("idpClock") Clock clock) {
            return new TenantServiceImpl(tenants, directory, clock);
        }

        @Bean("tenantMembershipServiceImpl")
        TenantMembershipService tenantMembershipService(
                @Qualifier("identityTenantRepository") IdentityTenantRepository tenants,
                @Qualifier("identityTenantMembershipRepository") IdentityTenantMembershipRepository memberships,
                @Qualifier("mpIdentityUserDirectory") IdentityUserDirectory users,
                @Qualifier("idpClock") Clock clock) {
            return new TenantMembershipServiceImpl(
                    tenants, memberships, users, clock);
        }
    }

    @Test
    void replacedDatabaseConnectionIsClosedBeforeItCanReachAnOwner() throws Exception {
        var receipt = IsolatedDatabaseReceiptDTO.builder().databaseName(DATABASE_NAMES.get("idp"))
                .oid(42L).ownerComment("owned").databaseOwner("postgres").build();
        var connection = Mockito.mock(Connection.class);
        var query = Mockito.mock(PreparedStatement.class);
        var rows = Mockito.mock(ResultSet.class);
        var source = Mockito.mock(DataSource.class);
        Mockito.when(source.getConnection()).thenReturn(connection);
        Mockito.when(connection.prepareStatement(ArgumentMatchers.anyString())).thenReturn(query);
        Mockito.when(query.executeQuery()).thenReturn(rows);
        Mockito.when(rows.next()).thenReturn(true, false);
        Mockito.when(rows.getString(1)).thenReturn(receipt.getDatabaseName());
        Mockito.when(rows.getString(2)).thenReturn("127.0.0.1");
        Mockito.when(rows.getInt(3)).thenReturn(5432);
        Mockito.when(rows.getLong(4)).thenReturn(43L);
        Mockito.when(rows.getString(5)).thenReturn(receipt.getOwnerComment());
        Mockito.when(rows.getString(6)).thenReturn(receipt.getDatabaseOwner());

        var guarded = guardPhysicalDataSource(source, receipt);
        assertThatThrownBy(guarded::getConnection).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("XINGYUAN_TARGET_UNSAFE");
        Mockito.verify(connection).close();
        Mockito.verify(query).close();
        Mockito.verify(rows).close();
    }

    @Test
    void guardedConnectionCannotOverrideTheReceiptBoundCredentials() throws Exception {
        var source = Mockito.mock(DataSource.class);
        var guarded = guardPhysicalDataSource(source, new IsolatedDatabaseReceiptDTO());
        assertThatThrownBy(() -> guarded.getConnection("foreign", "unused"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("XINGYUAN_TARGET_UNSAFE");
        Mockito.verifyNoInteractions(source);
    }

    static void verifyDatabaseConnection(Connection connection, IsolatedDatabaseReceiptDTO receipt)
            throws SQLException {
        try (var query = connection.prepareStatement("""
                SELECT current_database(), inet_server_addr()::text, inet_server_port(),
                       d.oid::bigint, obj_description(d.oid, 'pg_database'), pg_get_userbyid(d.datdba)
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

    @Test
    void aConnectedDatabaseMustMatchThePreparedOidAndExactLoopbackEndpoint() throws Exception {
        var connection = Mockito.mock(Connection.class);
        var query = Mockito.mock(PreparedStatement.class);
        var rows = Mockito.mock(ResultSet.class);
        Mockito.when(connection.prepareStatement(ArgumentMatchers.anyString())).thenReturn(query);
        Mockito.when(query.executeQuery()).thenReturn(rows);
        var receipt = IsolatedDatabaseReceiptDTO.builder().databaseName(DATABASE_NAMES.get("idp"))
                .oid(42L).ownerComment("task-owner-marker").databaseOwner("fixture-owner").build();
        Mockito.when(rows.next()).thenReturn(true, false);
        Mockito.when(rows.getString(1)).thenReturn(receipt.getDatabaseName());
        Mockito.when(rows.getString(2)).thenReturn("127.0.0.1");
        Mockito.when(rows.getInt(3)).thenReturn(5432);
        Mockito.when(rows.getLong(4)).thenReturn(42L);
        Mockito.when(rows.getString(5)).thenReturn(receipt.getOwnerComment());
        Mockito.when(rows.getString(6)).thenReturn(receipt.getDatabaseOwner());
        verifyDatabaseConnection(connection, receipt);
        Mockito.verify(query).setQueryTimeout(5);
        Mockito.when(rows.next()).thenReturn(true, false);
        Mockito.when(rows.getLong(4)).thenReturn(43L);
        assertThatThrownBy(() -> verifyDatabaseConnection(connection, receipt))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("XINGYUAN_TARGET_UNSAFE");
        Mockito.when(rows.next()).thenReturn(true, false);
        Mockito.when(rows.getLong(4)).thenReturn(42L);
        Mockito.when(rows.getString(2)).thenReturn("192.0.2.1");
        assertThatThrownBy(() -> verifyDatabaseConnection(connection, receipt))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    @Test
    void immutableManifestAndReceiptAreBoundBeforeAnyConnection() throws Exception {
        var manifest = intent();
        Path root = Path.of(manifest.getRuntimeRoot());
        Files.createDirectories(root.resolve("evidence"));
        Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
        Files.setPosixFilePermissions(root.resolve("evidence"), PosixFilePermissions.fromString("rwx------"));
        Path intentFile = root.resolve("intent.json");
        Path receiptFile = root.resolve("evidence/db-idp.json");
        byte[] original = strictMapper().writeValueAsBytes(manifest);
        Files.write(intentFile, original);
        Files.setPosixFilePermissions(intentFile, PosixFilePermissions.fromString("rw-------"));
        String digest = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(original));
        var receipt = IsolatedDatabaseReceiptDTO.builder().runId(manifest.getRunId()).manifestSha256(digest)
                .role("idp").databaseName(DATABASE_NAMES.get("idp")).oid(42L).databaseOwner("fixture-owner")
                .ownerComment("xingyuan-it:" + manifest.getRunId() + ":" + digest).build();
        Files.write(receiptFile, strictMapper().writeValueAsBytes(receipt));
        Files.setPosixFilePermissions(receiptFile, PosixFilePermissions.fromString("rw-------"));
        var dataSource = Mockito.mock(DataSource.class);
        Runnable guard = ownershipGuard(intentFile, receiptFile, temporaryRepository, "idp", dataSource);
        Mockito.verifyNoInteractions(dataSource);
        manifest.getDeadlines().setHttpSeconds(29);
        byte[] changed = strictMapper().writeValueAsBytes(manifest);
        Files.write(intentFile, changed);
        String changedDigest = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(changed));
        receipt.setManifestSha256(changedDigest)
                .setOwnerComment("xingyuan-it:" + manifest.getRunId() + ":" + changedDigest);
        Files.write(receiptFile, strictMapper().writeValueAsBytes(receipt));
        assertThatThrownBy(guard::run).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("XINGYUAN_TARGET_UNSAFE");
        Mockito.verifyNoInteractions(dataSource);
    }

    @Test
    void reportsRetainRequiredEmptyFieldsAndUtcInstant() throws Exception {
        IsolatedIntegrationResult result = IsolatedIntegrationResult.builder()
                .schemaVersion(1).operationId("CLI-001").runId(UUID.randomUUID().toString())
                .manifestSha256("a".repeat(64)).status("COMMITTED")
                .completedAt(Instant.parse("2026-10-14T00:00:00Z"))
                .counts(IsolatedCountsDTO.builder().build()).build();
        var tree = strictMapper().readTree(strictMapper().writeValueAsBytes(result));
        assertThat(tree.path("targets").isArray()).isTrue();
        assertThat(tree.path("steps").isArray()).isTrue();
        assertThat(tree.has("error")).isTrue();
        assertThat(tree.path("error").isNull()).isTrue();
        assertThat(tree.path("counts").size()).isEqualTo(5);
        assertThat(tree.path("completedAt").asText()).isEqualTo("2026-10-14T00:00:00Z");
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

    static IdentityProvisioningRecipeDTO readIdentityRecipe(IsolatedIntegrationManifestDTO manifest,
                                                           FileReferenceDTO reference, Path repository) throws IOException {
        validateTargets(manifest, repository);
        if (reference == null || !manifest.getPolicyFiles().contains(reference)) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        byte[] bytes = readProtectedBytes(Path.of(reference.getPath()), Path.of(manifest.getRuntimeRoot()), 2 * 1024 * 1024);
        String digest;
        try {
            digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException unavailableAlgorithm) {
            throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE");
        }
        if (!digest.equals(reference.getSha256())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var recipe = strictMapper().readValue(bytes, IdentityProvisioningRecipeDTO.class);
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            if (!validators.getValidator().validate(recipe).isEmpty()) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
            }
        }
        if (!manifest.getRunId().equals(recipe.getRunId())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        validateCliBindings(recipe, manifest);
        return recipe;
    }

    static void validateCliBindings(IdentityProvisioningRecipeDTO recipe, IsolatedIntegrationManifestDTO manifest) {
        Set<String> keys = new HashSet<>();
        Set<String> selectedClients = new HashSet<>();
        for (var binding : recipe.getCliResources()) {
            if (binding == null || !keys.add(binding.getResourceKey()) || binding.getClientIds() == null
                    || !binding.getClientIds().keySet().equals(Set.of("default", "tenant-b"))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            var resources = recipe.getResources().stream().filter(value -> value != null
                    && Objects.equals(value.resourceServerId(), binding.getResourceServerId())).toList();
            if (resources.size() != 1) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            try {
                URI path = URI.create(binding.getReadPath());
                String raw = binding.getReadPath().toLowerCase(java.util.Locale.ROOT);
                if (path.isAbsolute() || path.getRawAuthority() != null || path.getRawFragment() != null
                        || path.getRawPath() == null || !path.getRawPath().startsWith("/")
                        || raw.contains("..") || raw.contains("%2e") || raw.contains("%2f") || raw.contains("%5c")
                        || raw.contains("\\") || !path.normalize().toASCIIString().equals(binding.getReadPath())) {
                    throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                }
                for (String realm : List.of("default", "tenant-b")) {
                    String id = binding.getClientIds().get(realm);
                    var clients = recipe.getClients().stream().filter(value -> value != null && value.getInput() != null
                            && realm.equals(value.getRealmCode()) && Objects.equals(id, value.getInput().getClientId())).toList();
                    if (clients.size() != 1 || !selectedClients.add(realm + ":" + id)) {
                        throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                    }
                    var input = clients.getFirst().getInput();
                    if (input.getClientType() != OAuthClient.ClientType.PUBLIC || input.getPurpose() != OAuthClientPurposeEnum.USER
                            || input.getRefreshTokenTtlSeconds() != 0 || input.getResourceUris() == null
                            || !input.getResourceUris().equals(List.of(resources.getFirst().resourceUri()))
                            || input.getAllowedScopes() == null || !input.getAllowedScopes().contains("openid")
                            || input.getAllowedScopes().contains("offline_access") || input.getRedirectUris() == null
                            || input.getRedirectUris().size() != 1) {
                        throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                    }
                    URI redirect = URI.create(input.getRedirectUris().getFirst());
                    if (!"https".equals(redirect.getScheme()) || !"127.0.0.1".equals(redirect.getHost())
                            || redirect.getPort() < 1024 || redirect.getPort() == 28443 || redirect.getUserInfo() != null
                            || redirect.getRawQuery() != null || redirect.getRawFragment() != null
                            || !redirect.normalize().toASCIIString().equals(input.getRedirectUris().getFirst())
                            || manifest.getProcesses().stream().anyMatch(process -> process.getPort() == redirect.getPort()
                            || process.getAdditionalPorts().contains(redirect.getPort()))) {
                        throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                    }
                }
            } catch (IllegalArgumentException rejected) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
        }
    }

    @Test
    void cliBindingsNeverSelectAConfidentialClientOrForeignResourceOrCallback() {
        String uri = "https://resources.example/tianshu";
        var first = CreateOAuthClientDTO.builder().clientId("cli-a").clientName("CLI A")
                .clientType(OAuthClient.ClientType.PUBLIC).purpose(OAuthClientPurposeEnum.USER)
                .accessTokenTtlSeconds(300).refreshTokenTtlSeconds(0)
                .redirectUris(List.of("https://127.0.0.1:28444/callback"))
                .resourceUris(List.of(uri)).allowedScopes(List.of("openid", "profile", "TIANSHU_READ")).build();
        var second = CreateOAuthClientDTO.builder().clientId("cli-b").clientName("CLI B")
                .clientType(OAuthClient.ClientType.PUBLIC).purpose(OAuthClientPurposeEnum.USER)
                .accessTokenTtlSeconds(300).refreshTokenTtlSeconds(0)
                .redirectUris(List.of("https://127.0.0.1:28444/callback"))
                .resourceUris(List.of(uri)).allowedScopes(List.of("openid", "profile", "TIANSHU_READ")).build();
        var binding = CliResourcePreparationDTO.builder().resourceKey("tianshu").resourceServerId("resource-tianshu")
                .clientIds(Map.of("default", "cli-a", "tenant-b", "cli-b"))
                .readPath("/api/v1/tianshu/registry/services/page?pageNo=1&pageSize=10").build();
        var recipe = IdentityProvisioningRecipeDTO.builder().resources(List.of(new CreateResourceServerDTO(
                "resource-tianshu", uri, "platform", "tianshu", "local", "Tianshu", "manager", "ddc-admin", "TIANSHU_READ")))
                .clients(List.of(OAuthClientPreparationDTO.builder().realmCode("default").input(first).build(),
                        OAuthClientPreparationDTO.builder().realmCode("tenant-b").input(second).build()))
                .cliResources(List.of(binding)).build();
        validateCliBindings(recipe, intent());
        for (String fault : List.of("CONFIDENTIAL", "RESOURCE", "CALLBACK", "REDIRECT_TARGET", "MISSING_REALM")) {
            if ("CONFIDENTIAL".equals(fault)) first.setClientType(OAuthClient.ClientType.CONFIDENTIAL);
            if ("RESOURCE".equals(fault)) first.setResourceUris(List.of("https://foreign.example/resource"));
            if ("CALLBACK".equals(fault)) first.setRedirectUris(List.of("https://foreign.example/callback"));
            if ("REDIRECT_TARGET".equals(fault)) binding.setReadPath("//foreign.example/read");
            if ("MISSING_REALM".equals(fault)) binding.setClientIds(Map.of("default", "cli-a"));
            assertThatThrownBy(() -> validateCliBindings(recipe, intent())).as(fault).hasMessage("XINGYUAN_TARGET_UNSAFE");
            first.setClientType(OAuthClient.ClientType.PUBLIC).setResourceUris(List.of(uri))
                    .setRedirectUris(List.of("https://127.0.0.1:28444/callback"));
            binding.setReadPath("/api/v1/tianshu/registry/services/page?pageNo=1&pageSize=10")
                    .setClientIds(Map.of("default", "cli-a", "tenant-b", "cli-b"));
        }
    }

    @Test
    void identityRecipeCannotUseAnUnlistedOrChangedPolicyFile() throws Exception {
        var manifest = intent();
        Path policies = Files.createDirectories(Path.of(manifest.getRuntimeRoot()).resolve("policies"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Path file = policies.resolve("identity-provisioning.json");
        Files.writeString(file, "{}");
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        var reference = FileReferenceDTO.builder().path(file.toString()).sha256("a".repeat(64)).build();
        assertThatThrownBy(() -> readIdentityRecipe(manifest, reference, temporaryRepository)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        manifest.setPolicyFiles(List.of(reference));
        assertThatThrownBy(() -> readIdentityRecipe(manifest, reference, temporaryRepository)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        reference.setSha256(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))));
        Files.writeString(file, "{\"rawToken\":\"fixture-private-value\"}");
        assertThatThrownBy(() -> readIdentityRecipe(manifest, reference, temporaryRepository)).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    static void guardedWrite(IsolatedIntegrationManifestDTO manifest, Path repository, Runnable writer) {
        validateTargets(manifest, repository);
        writer.run();
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

    public interface Prepare { }
    public interface Provision { }
    public interface Runtime { }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class IsolatedProvisioningBO {
        private int schemaVersion;
        private String operationId;
        private String runId;
        private String manifestSha256;
        private String status;
        private Instant completedAt;
        @Builder.Default private List<IsolatedTargetResultDTO> targets = List.of();
        private IsolatedCountsDTO counts;
        @Builder.Default private List<IsolatedStepResultDTO> steps = List.of();
        private IsolatedErrorDTO error;
    }

    @org.mapstruct.Mapper(componentModel = "spring", implementationName = "IsolatedProvisioningResultConverterImpl",
            unmappedTargetPolicy = org.mapstruct.ReportingPolicy.ERROR)
    public interface IsolatedProvisioningResultConverter extends
            top.egon.cola.component.common.core.converter.BaseForwardConverter<IsolatedProvisioningBO, IsolatedIntegrationResult> {
    }

    @Test
    void provisioningReportProjectionKeepsRequiredEmptyFieldsWithoutBusinessSecrets() throws Exception {
        var source = IsolatedProvisioningBO.builder().schemaVersion(1).operationId("CLI-002")
                .runId(UUID.randomUUID().toString()).manifestSha256("a".repeat(64)).status("COMMITTED")
                .completedAt(Instant.parse("2026-10-14T00:00:00Z")).counts(IsolatedCountsDTO.builder().build()).build();
        var converter = new IsolatedProvisioningResultConverterImpl();
        var tree = strictMapper().readTree(strictMapper().writeValueAsBytes(converter.toTarget(source)));
        assertThat(tree.get("completedAt").asText()).isEqualTo("2026-10-14T00:00:00Z");
        assertThat(tree.get("error").isNull()).isTrue();
        assertThat(tree.get("targets").isArray()).isTrue();
        assertThat(tree.get("steps").isArray()).isTrue();
        assertThat(tree.get("counts").get("clientCount").asInt()).isZero();
        assertThat(tree.fieldNames()).toIterable().doesNotContain("subject", "token", "password", "clientSecret", "privateKey");
    }

    public enum ProvisionStageEnum {
        IDENTITY, REALMS, SECURITY, MANAGEMENT_CLIENTS, RESOURCES, CLIENTS, GRANTS
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void observeBffFamilies() throws IOException, InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException {
        Path repository = Path.of(requiredOperationProperty("xingyuan.isolated.repository-root"));
        Path manifest = Path.of(requiredOperationProperty("xingyuan.isolated.manifest-file"));
        Path receipt = Path.of(requiredOperationProperty("xingyuan.isolated.receipt-file"));
        var inputs = readVerifiedDatabaseInputs(manifest, receipt, repository, "idp");
        Path root = Path.of(inputs.getManifest().getRuntimeRoot());
        requireMatchingCheckpoint(root.resolve("evidence/identity.json"), root.resolve("evidence"), inputs, "COMMITTED");
        var registry = strictMapper().readValue(readProtectedBytes(root.resolve("secrets/runtime-registry.json"), root.resolve("secrets"), 2 * 1024 * 1024), IsolatedRuntimeRegistryDTO.class);
        var bindings = bffOracleBindings(inputs, registry);
        Path evidence = bffEvidenceDirectory(inputs);
        if (Files.exists(evidence.resolve("oracle-READY.json"), LinkOption.NOFOLLOW_LINKS)) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
        try (var context = openOAuthContext(manifest, receipt, repository, registry.getTenantIds(), true)) {
            var guard = context.getBean("isolatedOwnershipGuard", Runnable.class);
            var clock = context.getBean("idpClock", Clock.class);
            var converter = context.getBean("bffFamilyProofConverterImpl", BffFamilyProofConverter.class);
            Instant deadline = clock.instant().plusSeconds(inputs.getManifest().getDeadlines().getOperationSeconds());
            var http = java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(inputs.getManifest().getDeadlines().getHttpSeconds()))
                    .followRedirects(java.net.http.HttpClient.Redirect.NEVER).build();
            publishBffOracleReceipt(inputs, "READY", "READY", List.of(), converter, clock);
            for (String realmCode : List.of("default", "tenant-b")) {
                String name = realmCode.equals("default") ? "TENANT_A" : "TENANT_B";
                var activeBindings = bindings.stream().filter(binding -> realmCode.equals(binding.getClient().getRealmCode())).toList();
                var begin = awaitBffMarker(inputs, name + "_BEGIN", deadline, guard, clock);
                for (String phase : List.of("REFRESH", "STEP_UP")) {
                    var marker = awaitBffMarker(inputs, name + "_" + phase, deadline, guard, clock);
                    var required = activeBindings.stream().filter(binding -> phase.equals("REFRESH")
                            ? binding.getKind().equals("RESOURCE") : binding.getKind().equals("IDENTITY")).toList();
                    var proofs = new java.util.ArrayList<BffHeldFamilyBO>();
                    try {
                        for (var binding : required) {
                            var held = captureBffFamily(context, inputs, binding, begin, marker, deadline);
                            if (phase.equals("REFRESH")) { requireActualBffRefresh(context, binding, marker); }
                            else {
                                var id = held.getAuthorization().getToken(org.springframework.security.oauth2.core.oidc.OidcIdToken.class);
                                if (id == null || !id.isActive() || id.getToken().getClaimAsInstant("auth_time") == null
                                        || id.getToken().getClaimAsInstant("auth_time").isBefore(marker.getNotBefore().truncatedTo(java.time.temporal.ChronoUnit.SECONDS))
                                        || !binding.getSubject().equals(id.getToken().getSubject())
                                        || id.getToken().getIssuer() == null || !binding.getRealm().getIssuer().toASCIIString().equals(id.getToken().getIssuer().toString())
                                        || id.getToken().getAudience() == null || !id.getToken().getAudience().contains(binding.getClient().getClientId())) {
                                    throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
                                }
                                held.setAuthenticationTime(id.getToken().getClaimAsInstant("auth_time"));
                            }
                            proofs.add(held);
                        }
                        publishBffOracleReceipt(inputs, name + "_" + phase, "PASSED", proofs, converter, clock);
                    } finally { proofs.forEach(value -> value.setAuthorization(null)); proofs.clear(); }
                }
                var marker = awaitBffMarker(inputs, name + "_HOLD", deadline, guard, clock);
                var held = new java.util.ArrayList<BffHeldFamilyBO>();
                try {
                    for (var binding : activeBindings) { held.add(captureBffFamily(context, inputs, binding, begin, marker, deadline)); }
                    replayAllBffFamilies(inputs, held, name, http, guard, clock, converter, deadline);
                } finally { held.forEach(value -> value.setAuthorization(null)); held.clear(); }
            }
            var faultBindings = bindings.stream().filter(binding -> "default".equals(binding.getClient().getRealmCode())).toList();
            var faultBegin = awaitBffMarker(inputs, "TENANT_A_FAULT_BEGIN", deadline, guard, clock);
            var faultMarker = awaitBffMarker(inputs, "TENANT_A_FAULT_HOLD", deadline, guard, clock);
            var faultHeld = new java.util.ArrayList<BffHeldFamilyBO>();
            try {
                for (var binding : faultBindings) { faultHeld.add(captureBffFamily(context, inputs, binding, faultBegin, faultMarker, deadline)); }
                replayAllBffFamilies(inputs, faultHeld, "TENANT_A_FAULT", http, guard, clock, converter, deadline);
            } finally { faultHeld.forEach(value -> value.setAuthorization(null)); faultHeld.clear(); }
            publishBffOracleReceipt(inputs, "COMPLETE", "PASSED", List.of(), converter, clock);
        }
    }

    private static BffHeldFamilyBO captureBffFamily(AnnotationConfigApplicationContext context, VerifiedDatabaseInputsBO inputs,
            BffOracleBindingBO binding, BffBrowserMarkerDTO begin, BffBrowserMarkerDTO marker, Instant operationDeadline) throws IOException, InterruptedException {
        var guard = context.getBean("isolatedOwnershipGuard", Runnable.class);
        var clock = context.getBean("idpClock", Clock.class);
        var audits = context.getBean("oAuthAuditRepository", top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository.class);
        var redis = context.getBean("rbac3RuntimeRedissonClient", org.redisson.api.RedissonClient.class);
        String stream = "xingyuan-it:" + inputs.getManifest().getRunId() + ":oauth:audit:v1";
        var properties = context.getBean(top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties.class);
        Instant deadline = clock.instant().plusSeconds(inputs.getManifest().getDeadlines().getHttpSeconds());
        if (operationDeadline.isBefore(deadline)) { deadline = operationDeadline; }
        top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO used = null;
        while (clock.instant().isBefore(deadline)) {
            var candidate = withHeldRealmStorageContext(binding.getRealm(), properties,
                    () -> findBffUsageAudit(binding.getRealm(), binding.getClient(), binding.getSubject(), marker, audits, guard));
            if (candidate.isPresent()) {
                guard.run();
                if (redis.<String>getBucket(stream + ":receipt:" + candidate.get().getEventId(), org.redisson.client.codec.StringCodec.INSTANCE).get() != null) {
                    used = candidate.get(); break;
                }
            }
            Thread.sleep(java.time.Duration.ofMillis(100));
        }
        if (used == null || begin.getLoginStartedAt() == null || !marker.getToExclusive().isAfter(begin.getLoginStartedAt())) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        var idp = context.getBean("oracleIdpProperties", top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.class);
        var resources = context.getBean("oracleResourceStates", top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader.class);
        var clients = context.getBean("oracleClientStates", top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader.class);
        var issuers = context.getBean("oracleTenantIssuerManager", top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent.class);
        var revocations = context.getBean("oracleRevocationReader", top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader.class);
        var principal = context.getBean("verifiedPrincipalConverterImpl", top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverter.class);
        var verifier = top.egon.cola.platform.tianquan.shoubing.starter.security.IdpJwtVerifier.createConfigured(null, resources, clients,
                binding.getResourceServerId(), URI.create(binding.getClient().getResourceUris().getFirst()), idp.getPlatformAudience(), clock, issuers, revocations, principal, null);
        var match = HeldAuthorizationMatchDTO.builder().clientPk(binding.getClient().getClientPk()).principalName(binding.getSubject())
                .resourceUri(binding.getClient().getResourceUris().getFirst()).fromInclusive(begin.getLoginStartedAt()).toExclusive(marker.getToExclusive())
                .eventId(used.getEventId()).correlationId(used.getTraceId()).build();
        var authorization = materializeHeldAuthorization(binding.getRealm(), match,
                context.getBean(top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthAuthorizationMapper.class),
                context.getBean("tenantAuthorizationService", org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService.class),
                context.getBean("oracleAuthorizationServerSettings", org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings.class),
                verifier, audits, context.getBean("oracleUsageReferences", top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent.class),
                redis, stream, guard, clock, properties);
        var token = authorization.getAccessToken().getToken();
        if (marker.getNotBefore() != null && binding.getKind().equals("RESOURCE") && token.getIssuedAt().isBefore(marker.getNotBefore())) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        return BffHeldFamilyBO.builder().binding(binding).authorization(authorization).issuedAt(token.getIssuedAt()).expiresAt(token.getExpiresAt())
                .authorizationReference(sha256Bytes(authorization.getId().getBytes(StandardCharsets.UTF_8)))
                .tokenProofReference(sha256Bytes(used.getTokenRef().getBytes(StandardCharsets.UTF_8)))
                .eventProofReference(sha256Bytes(used.getEventId().getBytes(StandardCharsets.UTF_8))).build();
    }

    private static void requireActualBffRefresh(AnnotationConfigApplicationContext context, BffOracleBindingBO binding, BffBrowserMarkerDTO marker) {
        var audits = context.getBean("oAuthAuditRepository", top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository.class);
        var properties = context.getBean(top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties.class);
        var guard = context.getBean("isolatedOwnershipGuard", Runnable.class);
        var rows = withHeldRealmStorageContext(binding.getRealm(), properties,
                () -> boundedBffAudits(binding.getRealm(), marker.getNotBefore(), marker.getToExclusive(), audits, guard));
        requireBffRefreshProof(binding, rows);
    }

    static void requireBffRefreshProof(BffOracleBindingBO binding,
            List<top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO> rows) {
        String clientRef = sha256Bytes(binding.getClient().getClientPk().toString().getBytes(StandardCharsets.UTF_8));
        String subjectRef = sha256Bytes(binding.getSubject().getBytes(StandardCharsets.UTF_8));
        long count = rows.stream().filter(row -> "OAUTH".equals(row.getAuditKind()) && "SUCCESS".equals(row.getResult())
                && "REFRESH_TOKEN_ROTATED".equals(row.getEventType()) && "REFRESH_TOKEN_ROTATED".equals(row.getReasonCode())
                && clientRef.equals(row.getClientRef()) && subjectRef.equals(row.getSubjectRef())
                && binding.getClient().getResourceUris().getFirst().equals(row.getResourceRef())).count();
        if (count != 1L) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
    }

    @Test
    void aFreshCodeOrUsedClientReferenceCannotSubstituteForTheActualRefreshRotation() {
        var binding = BffOracleBindingBO.builder().subject("101").client(IsolatedClientFactDTO.builder()
                .clientPk(51L).clientId("actual-bff-a").resourceUris(List.of("https://127.0.0.1:28150")).build()).build();
        var issued = top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO.builder()
                .auditKind("OAUTH").result("SUCCESS").eventType("AUTHORIZATION_CODE_TOKEN_ISSUED").reasonCode("AUTHORIZATION_CODE_TOKEN_ISSUED")
                .clientRef(sha256Bytes("51".getBytes(StandardCharsets.UTF_8))).subjectRef(sha256Bytes("101".getBytes(StandardCharsets.UTF_8)))
                .resourceRef("https://127.0.0.1:28150").build();
        assertThatThrownBy(() -> requireBffRefreshProof(binding, List.of(issued))).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        issued.setEventType("REFRESH_TOKEN_ROTATED").setReasonCode("REFRESH_TOKEN_ROTATED");
        assertThatCode(() -> requireBffRefreshProof(binding, List.of(issued))).doesNotThrowAnyException();
        assertThatThrownBy(() -> requireBffRefreshProof(binding, List.of(issued, issued))).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        issued.setClientRef(sha256Bytes("actual-bff-a".getBytes(StandardCharsets.UTF_8)));
        assertThatThrownBy(() -> requireBffRefreshProof(binding, List.of(issued))).hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

    static Map<String, Long> taskCounterResponse(com.fasterxml.jackson.databind.JsonNode value,
            XingyuanRuntimeProbeInitializer.ProbeCommandDTO command, Instant now) {
        if (value == null || !value.isObject()) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        Set<String> fields = new HashSet<>(); value.fieldNames().forEachRemaining(fields::add);
        if (!fields.equals(Set.of("schemaVersion", "runId", "manifestSha256", "role", "fixtureSha256", "artifactSha256", "nonce", "operation", "status", "completedAt", "result"))
                || !value.path("schemaVersion").isInt() || value.path("schemaVersion").intValue() != 1 || !"BUSINESS_COUNTS".equals(command.operation())
                || !"COMPLETED".equals(value.path("status").asText()) || command.entityId() != null) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        for (var entry : Map.of("runId", command.runId(), "manifestSha256", command.manifestSha256(), "role", command.role(), "fixtureSha256", command.fixtureSha256(),
                "artifactSha256", command.artifactSha256(), "nonce", command.nonce(), "operation", command.operation()).entrySet()) {
            if (!value.path(entry.getKey()).isTextual() || !entry.getValue().equals(value.path(entry.getKey()).asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        }
        try {
            String timestamp = value.path("completedAt").asText(); Instant completed = Instant.parse(timestamp);
            if (!timestamp.endsWith("Z") || completed.isBefore(command.issuedAt()) || completed.isAfter(command.expiresAt()) || completed.isAfter(now)) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
        } catch (java.time.format.DateTimeParseException invalid) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        String method = Map.of("shoubing", "list", "jianshen", "roles", "tianshu", "pageServices", "yuheng", "list").get(command.role());
        var result = value.path("result"); var counts = result.path("counts");
        if (method == null || !result.isObject() || result.size() != 1 || !counts.isObject() || counts.size() != 1
                || !counts.path(method).isIntegralNumber() || !counts.path(method).canConvertToLong() || counts.path(method).longValue() < 0L) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        return Map.of(method, counts.path(method).longValue());
    }

    private static com.fasterxml.jackson.databind.JsonNode taskCounterOwner(VerifiedDatabaseInputsBO inputs, String role, Path manifest, Path evidence)
            throws IOException, InterruptedException {
        Path root = Path.of(inputs.getManifest().getRuntimeRoot());
        Path status = evidence.resolve("oracle-process-status-" + UUID.randomUUID() + ".json");
        var process = new ProcessBuilder("python3", "-I", Path.of(inputs.getManifest().getRepoRoot()).resolve("scripts/unified-xingyuan/lib/isolated_integration.py").toString(),
                "process-state", "status", manifest.toString(), status.toString()).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            process.getOutputStream().close();
            if (!process.waitFor(inputs.getManifest().getDeadlines().getHttpSeconds(), java.util.concurrent.TimeUnit.SECONDS) || process.exitValue() != 0) {
                throw new IllegalStateException("XINGYUAN_DEPENDENCY_UNAVAILABLE");
            }
        } finally { if (process.isAlive()) { process.destroy(); process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS); } }
        var observed = strictMapper().readTree(readProtectedBytes(status, root.resolve("evidence"), 128 * 1024));
        if (!inputs.getManifest().getRunId().equals(observed.path("runId").asText()) || !inputs.getManifestSha256().equals(observed.path("manifestSha256").asText())
                || !"CLI-008".equals(observed.path("operationId").asText()) || !"RUNNING".equals(observed.path("status").asText())) {
            throw new IllegalStateException("XINGYUAN_TARGET_UNSAFE");
        }
        var targets = java.util.stream.StreamSupport.stream(observed.path("targets").spliterator(), false).filter(value -> role.equals(value.path("targetKey").asText())).toList();
        var ready = strictMapper().readTree(readProtectedBytes(root.resolve("evidence/runtime-probe/" + role + "/ready.json"), root.resolve("evidence"), 16 * 1024));
        if (targets.size() != 1 || !"RUNNING".equals(targets.getFirst().path("state").asText()) || !ready.path("pid").isIntegralNumber()
                || !ready.path("pid").canConvertToLong() || ready.path("pid").longValue() <= 0 || !ready.path("pid").asText().equals(targets.getFirst().path("identity").asText())
                || !ready.path("processStartedAt").equals(targets.getFirst().path("version")) || !role.equals(ready.path("role").asText())
                || !inputs.getManifest().getRunId().equals(ready.path("runId").asText()) || !inputs.getManifestSha256().equals(ready.path("manifestSha256").asText())
                || !"READY".equals(ready.path("status").asText()) || !inputs.getManifest().getArtifacts().get(role).getSha256().equals(ready.path("artifactSha256").asText())) {
            throw new IllegalStateException("XINGYUAN_TARGET_UNSAFE");
        }
        return ready;
    }

    private static Map<String, Long> taskBusinessCounters(VerifiedDatabaseInputsBO inputs, BffOracleBindingBO binding,
            Runnable guard, Clock clock, Instant deadline) throws IOException, InterruptedException {
        String role = Map.of("edge-shoubing", "shoubing", "edge-jianshen", "jianshen", "edge-tianshu", "tianshu", "edge-yuheng", "yuheng").get(binding.getClient().getAlias());
        if (!"RESOURCE".equals(binding.getKind()) || role == null) { throw new IllegalStateException("XINGYUAN_TARGET_UNSAFE"); }
        guard.run(); Path root = Path.of(inputs.getManifest().getRuntimeRoot()), evidence = bffEvidenceDirectory(inputs);
        Path manifest = Path.of(requiredOperationProperty("xingyuan.isolated.manifest-file"));
        Path fixture = root.resolve("artifacts/task-runtime-probe.jar");
        String fixtureSha = sha256Bytes(readProtectedBytes(fixture, root.resolve("artifacts"), 2 * 1024 * 1024));
        if (inputs.getManifest().getPolicyFiles().stream().filter(value -> fixture.toString().equals(value.getPath()) && fixtureSha.equals(value.getSha256())).count() != 1) {
            throw new IllegalStateException("XINGYUAN_TARGET_UNSAFE");
        }
        var ready = taskCounterOwner(inputs, role, manifest, evidence);
        if (!fixtureSha.equals(ready.path("fixtureSha256").asText())) { throw new IllegalStateException("XINGYUAN_TARGET_UNSAFE"); }
        Instant issued = clock.instant(), expires = issued.plusSeconds(60); if (deadline.isBefore(expires)) { expires = deadline; }
        if (!expires.isAfter(issued)) { throw new IllegalStateException("XINGYUAN_DEPENDENCY_UNAVAILABLE"); }
        var command = new XingyuanRuntimeProbeInitializer.ProbeCommandDTO(1, inputs.getManifest().getRunId(), inputs.getManifestSha256(), role,
                fixtureSha, inputs.getManifest().getArtifacts().get(role).getSha256(), UUID.randomUUID().toString(), "BUSINESS_COUNTS", binding.getClient().getRealmCode(), null, issued, expires);
        Path directory = root.resolve("evidence/runtime-probe/" + role);
        publishProtectedBytes(directory.resolve("requests/" + command.nonce() + ".json"), root.resolve("evidence"), XingyuanRuntimeProbeInitializer.mapper().writeValueAsBytes(command));
        Path output = directory.resolve("results/" + command.nonce() + ".json");
        while (!Files.exists(output, LinkOption.NOFOLLOW_LINKS)) {
            guard.run(); if (!expires.isAfter(clock.instant())) { throw new IllegalStateException("XINGYUAN_DEPENDENCY_UNAVAILABLE"); } Thread.sleep(100);
        }
        byte[] response = readProtectedBytes(output, root.resolve("evidence"), 16 * 1024);
        var result = taskCounterResponse(strictMapper().readTree(response), command, clock.instant());
        taskCounterOwner(inputs, role, manifest, evidence); guard.run();
        var proof = strictMapper().createObjectNode().put("schemaVersion", 1).put("runId", inputs.getManifest().getRunId()).put("manifestSha256", inputs.getManifestSha256())
                .put("role", role).put("alias", binding.getClient().getAlias()).put("nonce", command.nonce()).put("ownerProofSha256", sha256Bytes(response)).put("recordedAt", clock.instant().toString());
        proof.set("counts", strictMapper().valueToTree(result));
        publishProtectedBytes(evidence.resolve("oracle-counter-" + command.nonce() + ".json"), evidence, strictMapper().writeValueAsBytes(proof));
        return result;
    }

    @Test
    void bffCounterProofRequiresTheExactOwnerCommandAndCannotAcceptForeignOrResetCounts() {
        Instant now = Instant.parse("2026-10-15T00:00:00Z");
        var command = new XingyuanRuntimeProbeInitializer.ProbeCommandDTO(1, "run", "a".repeat(64), "shoubing", "b".repeat(64), "c".repeat(64),
                UUID.randomUUID().toString(), "BUSINESS_COUNTS", "default", null, now, now.plusSeconds(60));
        var envelope = strictMapper().createObjectNode().put("schemaVersion", 1).put("runId", command.runId()).put("manifestSha256", command.manifestSha256())
                .put("role", command.role()).put("fixtureSha256", command.fixtureSha256()).put("artifactSha256", command.artifactSha256()).put("nonce", command.nonce())
                .put("operation", command.operation()).put("status", "COMPLETED").put("completedAt", now.plusSeconds(1).toString());
        envelope.putObject("result").putObject("counts").put("list", 2L);
        assertThat(taskCounterResponse(envelope, command, now.plusSeconds(2))).containsExactlyEntriesOf(Map.of("list", 2L));
        for (String fault : List.of("runId", "role", "fixtureSha256", "artifactSha256", "nonce", "operation", "late", "negative", "float", "overflow", "extra", "secret")) {
            var invalid = envelope.deepCopy();
            if (Set.of("runId", "role", "fixtureSha256", "artifactSha256", "nonce", "operation").contains(fault)) { invalid.put(fault, "foreign"); }
            else if (fault.equals("late")) { invalid.put("completedAt", now.plusSeconds(61).toString()); }
            else if (fault.equals("negative")) { invalid.withObject("result").withObject("counts").put("list", -1); }
            else if (fault.equals("float")) { invalid.withObject("result").withObject("counts").put("list", 1.5); }
            else if (fault.equals("overflow")) { invalid.withObject("result").withObject("counts").put("list", new java.math.BigInteger("9223372036854775808")); }
            else if (fault.equals("extra")) { invalid.withObject("result").withObject("counts").put("other", 0); }
            else { invalid.put("accessToken", "forbidden"); }
            assertThatThrownBy(() -> taskCounterResponse(invalid, command, now.plusSeconds(2))).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void aSignedDeadSpoolReceiptMustBlockTheConfiguredStrictHandoff() throws Exception {
        Path key = temporaryRepository.resolve("controlled-dead-audit.key");
        Files.writeString(key, "0123456789abcdef0123456789abcdef"); Files.setPosixFilePermissions(key, PosixFilePermissions.fromString("rw-------"));
        Path spool = temporaryRepository.resolve("controlled-dead-spool");
        var audit = new top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties("xingyuan-it:controlled:oauth:audit:v1", spool, 1_048_576, key);
        var redis = org.mockito.Mockito.mock(org.redisson.api.RedissonClient.class);
        @SuppressWarnings("unchecked") org.redisson.api.RStream<String, String> stream = org.mockito.Mockito.mock(org.redisson.api.RStream.class);
        org.mockito.Mockito.doReturn(stream).when(redis).getStream(org.mockito.ArgumentMatchers.eq(audit.stream()), org.mockito.ArgumentMatchers.eq(org.redisson.client.codec.StringCodec.INSTANCE));
        org.mockito.Mockito.when(stream.add(org.mockito.ArgumentMatchers.any(org.redisson.api.stream.StreamAddArgs.class))).thenReturn(new org.redisson.api.StreamMessageId(1, 1));
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var readiness = new top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityObservationReadinessComponent(audit, validators.getValidator());
            try (var lease = readiness.acquireSpoolLease()) {
                var ready = readiness.readiness(lease);
                var producer = new top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent(redis, strictMapper(), audit,
                        new top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties(), Clock.systemUTC(), validators.getValidator());
                var properties = new top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties(true,
                        URI.create("https://127.0.0.1:28443/tenants/1001"), false,
                        top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityObservationReceiverEnum.APPLICATION);
                var receiver = new top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties(); receiver.setResourceUri(URI.create("https://127.0.0.1:28140"));
                var metricEvents = new java.util.ArrayList<top.egon.cola.platform.tianquan.shoubing.core.observability.SecurityMetricObservationBO>();
                new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                        .withUserConfiguration(top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterAutoConfiguration.UsageObservationConfiguration.class)
                        .withPropertyValues("egon.cola.platform.tianquan.shoubing.usage-observation.enabled=true")
                        .withBean("tokenUsageAuditComponent", top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent.class, () -> producer)
                        .withBean("tokenUsageAuditEventConverterImpl", top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter.class,
                                () -> org.mapstruct.factory.Mappers.getMapper(top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter.class))
                        .withBean("oauthUsageObservationProperties", top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties.class, () -> properties)
                        .withBean("idpSecurityMetricsRecorder", top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder.class,
                                () -> metricEvents::add)
                        .withBean("idpClock", Clock.class, Clock::systemUTC).withBean("defaultValidator", Validator.class, validators::getValidator)
                        .withBean("idpStarterProperties", top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.class, () -> receiver)
                        .withBean("idpSecurityAuditReady", java.util.function.BooleanSupplier.class, () -> ready)
                        .run(context -> {
                            assertThat(context).hasNotFailed(); assertThat(ready.getAsBoolean()).isTrue();
                            var event = new top.egon.cola.platform.tianquan.shoubing.core.audit.TokenUsageAuditEvent("controlled-dead-event", 1, properties.getFallbackIssuer(),
                                    receiver.getResourceUri().toASCIIString(), "a".repeat(64), "b".repeat(64), "c".repeat(64), "SUCCESS", "ACCESS_GRANTED", Instant.now(), "controlled-dead");
                            producer.appendStrict(event);
                            Path dead = spool.resolve(event.eventId() + ".evt.dead");
                            Files.writeString(dead, producer.encodeReceipt(producer.createReceipt(event, "DEAD", Instant.now())));
                            Files.setPosixFilePermissions(dead, PosixFilePermissions.fromString("rw-------"));
                            assertThat(ready.getAsBoolean()).isFalse(); org.mockito.Mockito.clearInvocations(stream);
                            var observations = context.getBean("idpObservedVerificationComponent", top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent.class);
                            assertThat(observations.authenticationFailure()).isFalse();
                            org.mockito.Mockito.verifyNoInteractions(stream);
                        });
            }
        }
    }

    private static void replayAllBffFamilies(VerifiedDatabaseInputsBO inputs, List<BffHeldFamilyBO> held, String name,
            java.net.http.HttpClient client, Runnable guard, Clock clock, BffFamilyProofConverter converter, Instant deadline)
            throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException, IOException {
        if (held.size() != 5) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        Instant latestReplay = held.stream().map(family -> family.getExpiresAt().minusSeconds(30)).min(Instant::compareTo).orElseThrow();
        Instant replayDeadline = latestReplay.isBefore(deadline) ? latestReplay : deadline;
        var baseline = new java.util.concurrent.CountDownLatch(5);
        var published = new java.util.concurrent.atomic.AtomicBoolean();
        var counterProofs = new java.util.concurrent.ConcurrentLinkedQueue<Map<String, Object>>();
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(5)) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (var family : held) {
                futures.add(executor.submit(() -> {
                    try {
                        boolean business = "RESOURCE".equals(family.getBinding().getKind());
                        var before = business ? taskBusinessCounters(inputs, family.getBinding(), guard, clock, replayDeadline) : Map.<String, Long>of();
                        var successfulCounts = new java.util.concurrent.atomic.AtomicReference<Map<String, Long>>();
                        replayHeldFamily(client, family.getBinding().getReadEndpoint(), family.getAuthorization().getAccessToken().getToken(), () -> {
                            family.setBeforeStatus(200);
                            try {
                                if (business) {
                                    var observed = taskBusinessCounters(inputs, family.getBinding(), guard, clock, replayDeadline);
                                    String method = before.keySet().iterator().next();
                                    if (!observed.equals(Map.of(method, Math.addExact(before.get(method), 1L)))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                                    successfulCounts.set(observed);
                                }
                                baseline.countDown();
                                if (!baseline.await(inputs.getManifest().getDeadlines().getHttpSeconds(), java.util.concurrent.TimeUnit.SECONDS)) {
                                    throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
                                }
                                if (published.compareAndSet(false, true)) { publishBffOracleReceipt(inputs, name + "_HOLD", "READY", held, converter, clock); }
                                awaitBffMarker(inputs, name + "_LOGOUT", replayDeadline, guard, clock);
                            } catch (IOException failure) { throw new java.io.UncheckedIOException(failure); }
                            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                        }, guard, clock, body -> successfulBffReadBody(family.getBinding(), body));
                        family.setAfterStatus(401);
                        if (business) {
                            var after = taskBusinessCounters(inputs, family.getBinding(), guard, clock, replayDeadline);
                            if (!after.equals(successfulCounts.get())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                            counterProofs.add(Map.of("alias", family.getBinding().getClient().getAlias(), "beforeRequest", before,
                                    "afterSuccess", successfulCounts.get(), "afterRevocation", after));
                        }
                    } catch (IOException | InterruptedException failure) {
                        if (failure instanceof InterruptedException) { Thread.currentThread().interrupt(); }
                        throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
                    }
                }));
            }
            try {
                for (var future : futures) {
                    long remaining = java.time.Duration.between(clock.instant(), replayDeadline).toMillis();
                    if (remaining <= 0L) { throw new java.util.concurrent.TimeoutException(); }
                    future.get(remaining, java.util.concurrent.TimeUnit.MILLISECONDS);
                }
            } finally { futures.forEach(future -> future.cancel(true)); executor.shutdownNow(); }
        }
        if (counterProofs.size() != 4) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        var counters = strictMapper().createObjectNode().put("schemaVersion", 1).put("runId", inputs.getManifest().getRunId()).put("manifestSha256", inputs.getManifestSha256())
                .put("scope", "ACTUAL_RESOURCE_HANDLER_COUNTS_ONLY").put("status", "PASSED").put("phase", name + "_REPLAY").put("recordedAt", clock.instant().toString());
        counters.set("proofs", strictMapper().valueToTree(counterProofs.stream().sorted(java.util.Comparator.comparing(value -> value.get("alias").toString())).toList()));
        Path evidence = bffEvidenceDirectory(inputs);
        publishProtectedBytes(evidence.resolve("oracle-" + name + "_COUNTERS.json"), evidence, strictMapper().writeValueAsBytes(counters));
        publishBffOracleReceipt(inputs, name + "_REPLAY", "PASSED", held, converter, clock);
    }

    private static boolean successfulBffReadBody(BffOracleBindingBO binding, String body) {
        try {
            var value = strictMapper().readTree(body);
            if (binding.getKind().equals("IDENTITY")) { return binding.getSubject().equals(value.path("sub").asText()); }
            String alias = binding.getClient().getAlias();
            if (alias.equals("edge-shoubing")) {
                return value.isArray() && java.util.stream.StreamSupport.stream(value.spliterator(), false)
                        .allMatch(row -> row.path("clientId").isTextual() && row.path("appId").isTextual());
            }
            if (alias.equals("edge-yuheng")) {
                return value.isArray() && java.util.stream.StreamSupport.stream(value.spliterator(), false)
                        .allMatch(row -> row.path("bindingId").isTextual() && (!row.has("connected") || row.path("connected").isBoolean()));
            }
            if (!value.path("success").isBoolean() || !value.path("success").booleanValue()) { return false; }
            if (alias.equals("edge-jianshen")) {
                return value.path("data").isArray() && java.util.stream.StreamSupport.stream(value.path("data").spliterator(), false)
                        .allMatch(row -> row.path("roleId").isTextual() && row.path("applicationId").isTextual());
            }
            return alias.equals("edge-tianshu") && value.path("records").isArray() && value.path("page").isObject()
                    && value.path("page").path("total").isIntegralNumber();
        } catch (IOException malformedBody) { return false; }
    }

    @Test
    void bffReplayKeepsTheOriginalListRecordAndPageResponseContracts() {
        var binding = BffOracleBindingBO.builder().kind("RESOURCE").client(IsolatedClientFactDTO.builder().alias("edge-shoubing").build()).build();
        assertThat(successfulBffReadBody(binding, "[{\"clientId\":\"actual-client\",\"appId\":\"actual-app\"}]")).isTrue();
        assertThat(successfulBffReadBody(binding, "{\"status\":\"UP\"}")).isFalse();
        binding.getClient().setAlias("edge-yuheng");
        assertThat(successfulBffReadBody(binding, "[{\"bindingId\":\"71\",\"connected\":true}]")).isTrue();
        binding.getClient().setAlias("edge-jianshen");
        assertThat(successfulBffReadBody(binding, "{\"success\":true,\"data\":[{\"roleId\":\"71\",\"applicationId\":\"61\"}]}")).isTrue();
        assertThat(successfulBffReadBody(binding, "{\"success\":false,\"data\":[]}")).isFalse();
        binding.getClient().setAlias("edge-tianshu");
        assertThat(successfulBffReadBody(binding, "{\"success\":true,\"records\":[],\"page\":{\"total\":0}}")).isTrue();
        assertThat(successfulBffReadBody(binding, "{\"success\":true,\"data\":[]}")).isFalse();
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FormalRevocationCoordinationDTO {
        @Min(1) @Max(1) private int schemaVersion;
        @NotBlank private String runId;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String manifestSha256;
        @NotBlank @Pattern(regexp = "READY|RUNNING|CONFIRMED") private String status;
        @Min(200) @Max(200) private int baselineStatus;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String tokenSha256;
        @NotNull private URI issuer;
        @NotBlank @Pattern(regexp = "[1-9][0-9]{0,18}") private String clientPk;
        @NotBlank @Pattern(regexp = "[1-9][0-9]{0,18}") private String tenantId;
        @NotNull private URI resourceUri;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String authorizationReference;
        @NotNull @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) private Instant recordedAt;
        private Boolean denyApplied;
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void revokeFormalFamily() throws IOException, InterruptedException {
        Path repository = Path.of(requiredOperationProperty("xingyuan.isolated.repository-root"));
        Path manifest = Path.of(requiredOperationProperty("xingyuan.isolated.manifest-file"));
        Path receipt = Path.of(requiredOperationProperty("xingyuan.isolated.receipt-file"));
        String expected = requiredOperationProperty("xingyuan.isolated.revocation-token-sha256");
        if (!expected.matches("[0-9a-f]{64}")) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
        var inputs = readVerifiedDatabaseInputs(manifest, receipt, repository, "idp");
        Path root = Path.of(inputs.getManifest().getRuntimeRoot());
        Path evidence = bffEvidenceDirectory(inputs);
        var request = strictMapper().readValue(readProtectedBytes(evidence.resolve("formal-revocation-request-" + expected + ".json"), evidence, 128 * 1024), FormalRevocationCoordinationDTO.class);
        validateFormalRevocationRequest(inputs, request, expected);
        var registry = strictMapper().readValue(readProtectedBytes(root.resolve("secrets/runtime-registry.json"), root.resolve("secrets"), 2 * 1024 * 1024), IsolatedRuntimeRegistryDTO.class);
        runtimeRegistryFacts(inputs, registry);
        var index = strictMapper().readTree(readProtectedBytes(root.resolve("secrets/token-index.json"), root.resolve("secrets"), 128 * 1024));
        if (!inputs.getManifest().getRunId().equals(index.path("runId").asText()) || !inputs.getManifestSha256().equals(index.path("manifestSha256").asText())
                || !index.path("tokens").isArray() || index.path("tokens").size() > 192) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var selected = java.util.stream.StreamSupport.stream(index.path("tokens").spliterator(), false)
                .filter(value -> expected.equals(value.path("tokenSha256").asText())).toList();
        if (selected.size() != 1) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"); }
        var tokenReference = selected.getFirst();
        String realmCode = tokenReference.path("tenantCode").asText();
        var realm = registry.getRealms().get(realmCode);
        var clients = registry.getClients().stream().filter(client -> realmCode.equals(client.getRealmCode())
                && request.getClientPk().equals(client.getClientPk().toString())).toList();
        if (realm == null || !Set.of("default", "tenant-b").contains(realmCode) || clients.size() != 1
                || clients.getFirst().getClientType() != OAuthClient.ClientType.PUBLIC || clients.getFirst().getPurpose() != OAuthClientPurposeEnum.USER
                || clients.getFirst().getStatus() != OAuthClient.Status.ACTIVE || !clients.getFirst().getResourceUris().equals(List.of(request.getResourceUri().toASCIIString()))
                || !realm.getIssuer().equals(request.getIssuer()) || !realm.getBusinessTenantId().toString().equals(request.getTenantId())
                || !request.getClientPk().equals(tokenReference.path("clientPk").asText())
                || !clients.getFirst().getClientId().equals(tokenReference.path("clientId").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        byte[] bytes = readProtectedBytes(Path.of(tokenReference.path("tokenFile").asText()), root.resolve("secrets"), 128 * 1024);
        if (!expected.equals(sha256Bytes(bytes))) { java.util.Arrays.fill(bytes, (byte) 0); throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        try (var context = openOAuthContext(manifest, receipt, repository, registry.getTenantIds(), true)) {
            var guard = context.getBean("isolatedOwnershipGuard", Runnable.class);
            var clock = context.getBean("idpClock", Clock.class);
            var verified = context.getBean("oracleTenantIssuerManager", top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent.class)
                    .authenticate(new String(bytes, StandardCharsets.UTF_8));
            var jwt = verified.getJwt();
            if (!"at+jwt".equals(String.valueOf(jwt.getHeaders().get("typ")))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            if (!request.getIssuer().toASCIIString().equals(jwt.getIssuer().toString())
                    || !List.of(request.getResourceUri().toASCIIString()).equals(jwt.getAudience())
                    || !request.getClientPk().equals(verified.getClientRegistrationId())
                    || !clients.getFirst().getClientId().equals(jwt.getClaimAsString("client_id"))
                    || !request.getTenantId().equals(jwt.getClaimAsString("tenant_id")) || !"USER".equals(jwt.getClaimAsString("principal_type"))
                    || !"TENANT".equals(jwt.getClaimAsString("scope_context"))
                    || !registry.getSubjects().get(tokenReference.path("userKey").asText()).equals(jwt.getSubject())
                    || !sha256Bytes(verified.getAuthorizationId().getBytes(StandardCharsets.UTF_8)).equals(request.getAuthorizationReference())
                    || jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(clock.instant().plusSeconds(30))) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
            var properties = context.getBean(top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties.class);
            var authorizations = context.getBean("tenantAuthorizationService", org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService.class);
            var deny = context.getBean("oracleRevocationReader", top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader.class);
            Path pending = evidence.resolve("formal-revocation-running-" + expected + ".json");
            if (!Files.exists(pending, LinkOption.NOFOLLOW_LINKS)) {
                var active = withHeldRealmStorageContext(realm, properties, () -> authorizations.findById(verified.getAuthorizationId()));
                if (active == null || active.getAccessToken() == null || !active.getAccessToken().isActive()
                        || !expected.equals(sha256Bytes(active.getAccessToken().getToken().getTokenValue().getBytes(StandardCharsets.UTF_8)))) {
                    throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
                }
                var command = withHeldRealmStorageContext(realm, properties, () -> formalFamilyCommand(realm, Long.valueOf(request.getClientPk()), Long.valueOf(verified.getAuthorizationId()),
                        context.getBean("oAuthTokenRepository", top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthTokenRepository.class), guard));
                request.setStatus("RUNNING").setRecordedAt(clock.instant()).setDenyApplied(false);
                publishProtectedBytes(pending, evidence, strictMapper().writeValueAsBytes(request));
                guard.run();
                withHeldRealmStorageContext(realm, properties, () -> {
                    context.getBean("tokenRevocationService", top.egon.cola.platform.tianquan.shoubing.admin.token.service.TokenRevocationService.class).revoke(realm, command);
                    return null;
                });
            } else {
                var prior = strictMapper().readValue(readProtectedBytes(pending, evidence, 128 * 1024), FormalRevocationCoordinationDTO.class);
                if (!prior.getAuthorizationReference().equals(request.getAuthorizationReference()) || !prior.getTokenSha256().equals(expected)
                        || !prior.getManifestSha256().equals(inputs.getManifestSha256()) || !prior.getRunId().equals(inputs.getManifest().getRunId())) {
                    throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
                }
            }
            Instant deadline = clock.instant().plusSeconds(inputs.getManifest().getDeadlines().getHttpSeconds());
            boolean confirmed = false;
            while (clock.instant().isBefore(deadline)) {
                guard.run();
                var state = deny.check(verified);
                var current = withHeldRealmStorageContext(realm, properties, () -> authorizations.findById(verified.getAuthorizationId()));
                if (state.status() == top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader.Status.REVOKED && current == null) {
                    confirmed = true; break;
                }
                Thread.sleep(java.time.Duration.ofMillis(100));
            }
            if (!confirmed || !jwt.getExpiresAt().isAfter(clock.instant())) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
            request.setStatus("CONFIRMED").setRecordedAt(clock.instant()).setDenyApplied(true);
            Path result = evidence.resolve("formal-revocation-" + expected + ".json");
            if (!Files.exists(result, LinkOption.NOFOLLOW_LINKS)) { publishProtectedBytes(result, evidence, strictMapper().writeValueAsBytes(request)); }
        } finally { java.util.Arrays.fill(bytes, (byte) 0); }
    }

    static void validateFormalRevocationRequest(VerifiedDatabaseInputsBO inputs, FormalRevocationCoordinationDTO request, String tokenHash) {
        try (var validators = Validation.buildDefaultValidatorFactory()) { new ValidationUtils(validators.getValidator()).validate(request); }
        if (!"READY".equals(request.getStatus()) || !inputs.getManifest().getRunId().equals(request.getRunId())
                || !inputs.getManifestSha256().equals(request.getManifestSha256()) || !tokenHash.equals(request.getTokenSha256())
                || !request.getIssuer().toASCIIString().equals(inputs.getManifest().getPublicOrigin() + "/tenants/" + request.getTenantId())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
    }

    @Test
    void formalFamilyRequestCannotCrossTheRunTokenOrExactTenantIssuer() {
        var manifest = intent();
        var inputs = VerifiedDatabaseInputsBO.builder().manifest(manifest).manifestSha256("a".repeat(64)).build();
        var request = FormalRevocationCoordinationDTO.builder().schemaVersion(1).runId(manifest.getRunId()).manifestSha256(inputs.getManifestSha256())
                .status("READY").baselineStatus(200).tokenSha256("b".repeat(64)).clientPk("51").tenantId("1001")
                .issuer(URI.create(manifest.getPublicOrigin() + "/tenants/1001")).resourceUri(URI.create("https://127.0.0.1:28310"))
                .authorizationReference("c".repeat(64)).recordedAt(Instant.parse("2026-10-15T00:00:00Z")).build();
        assertThatCode(() -> validateFormalRevocationRequest(inputs, request, "b".repeat(64))).doesNotThrowAnyException();
        request.setRunId(UUID.randomUUID().toString());
        assertThatThrownBy(() -> validateFormalRevocationRequest(inputs, request, "b".repeat(64))).hasMessage("XINGYUAN_TARGET_UNSAFE");
        request.setRunId(manifest.getRunId()).setIssuer(URI.create(manifest.getPublicOrigin() + "/tenants/1002"));
        assertThatThrownBy(() -> validateFormalRevocationRequest(inputs, request, "b".repeat(64))).hasMessage("XINGYUAN_TARGET_UNSAFE");
        request.setIssuer(URI.create(manifest.getPublicOrigin() + "/tenants/1001"));
        assertThatThrownBy(() -> validateFormalRevocationRequest(inputs, request, "d".repeat(64))).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    static top.egon.cola.platform.tianquan.shoubing.admin.token.domain.command.RevokeOAuthTokenCommand formalFamilyCommand(
            TrustedRealmBO realm, Long clientPk, Long authorizationId,
            top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthTokenRepository tokens, Runnable guard) {
        realm.validateTrustedPartition();
        if (clientPk == null || clientPk <= 0L || authorizationId == null || authorizationId <= 0L || realm.getRealmType() != RealmTypeEnum.TENANT) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        guard.run();
        var code = tokens.findByAuthorization(realm, authorizationId,
                top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum.CODE, 0L, 2);
        if (code == null || code.size() != 1 || code.getFirst().getTokenType() != top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum.CODE
                || !authorizationId.equals(code.getFirst().getAuthorizationPk()) || code.getFirst().getTokenDigest() == null
                || !code.getFirst().getTokenDigest().matches("[0-9a-f]{64}")) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        return top.egon.cola.platform.tianquan.shoubing.admin.token.domain.command.RevokeOAuthTokenCommand.builder()
                .clientPk(clientPk).tokenDigest(code.getFirst().getTokenDigest()).reasonCode("XINGYUAN_FORMAL_FAMILY_REVOKED").build();
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ServiceAccessRevocationDTO {
        @Min(1) @Max(1) private int schemaVersion;
        @NotBlank private String runId;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String manifestSha256;
        @NotBlank @Pattern(regexp = "READY|RUNNING|CONFIRMED") private String status;
        @NotBlank @Pattern(regexp = "native-web|native-service") private String consumerRole;
        @NotBlank @Pattern(regexp = "native-web|native-service") private String providerRole;
        @NotBlank @Pattern(regexp = "default|tenant-b") private String tenantCode;
        @NotBlank @Pattern(regexp = "[1-9][0-9]{0,18}") private String tenantId;
        @NotNull private URI issuer;
        @NotNull private URI resourceUri;
        @NotBlank private String clientId;
        @NotBlank @Pattern(regexp = "[1-9][0-9]{0,18}") private String clientPk;
        @NotBlank @Pattern(regexp = "[1-9][0-9]{0,18}") private String authorizationId;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String authorizationReference;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String tokenSha256;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String methodReference;
        @NotBlank @Pattern(regexp = "OK") private String baselineRpcStatus;
        @NotNull @Size(min = 1, max = 64) private Set<@NotBlank String> scopes;
        @NotNull @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) private Instant expiresAt;
        @NotNull @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) private Instant recordedAt;
        private Boolean denyApplied;
    }

    static top.egon.cola.platform.tianquan.shoubing.admin.token.domain.command.RevokeOAuthTokenCommand serviceAccessCommand(TrustedRealmBO realm, Long clientPk, Long authorizationId,
            top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthTokenRepository repository, Runnable guard) {
        realm.validateTrustedPartition();
        if (realm.getRealmType() != RealmTypeEnum.TENANT || clientPk == null || clientPk <= 0 || authorizationId == null || authorizationId <= 0) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        guard.run();
        var tokens = repository.findByAuthorization(realm, authorizationId, top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum.ACCESS, 0L, 2);
        if (tokens == null || tokens.size() != 1 || tokens.getFirst().getTokenType() != top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum.ACCESS
                || !authorizationId.equals(tokens.getFirst().getAuthorizationPk()) || tokens.getFirst().getTokenDigest() == null
                || !tokens.getFirst().getTokenDigest().matches("[0-9a-f]{64}")) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        return top.egon.cola.platform.tianquan.shoubing.admin.token.domain.command.RevokeOAuthTokenCommand.builder().clientPk(clientPk)
                .tokenDigest(tokens.getFirst().getTokenDigest()).reasonCode("XINGYUAN_SERVICE_PROBE_REVOKED").build();
    }

    @Test
    void serviceRevocationUsesOnlyTheUniqueOwnedAccessTokenAndKeepsCodeSemanticsSeparate() {
        var realm = TrustedRealmBO.builder().issuer(URI.create("https://127.0.0.1:28443/tenants/1001")).realmKey("tenant:1001")
                .realmType(RealmTypeEnum.TENANT).storagePartitionId(1001L).businessTenantId(1001L).trustVersion(0L).build();
        var repository = Mockito.mock(top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthTokenRepository.class);
        var token = top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthTokenBO.builder().authorizationPk(71L)
                .tokenType(top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum.ACCESS).tokenDigest("a".repeat(64)).build();
        Mockito.when(repository.findByAuthorization(realm, 71L, top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum.ACCESS, 0L, 2)).thenReturn(List.of(token));
        assertThat(serviceAccessCommand(realm, 51L, 71L, repository, () -> { }).getTokenDigest()).isEqualTo("a".repeat(64));
        token.setAuthorizationPk(72L);
        assertThatThrownBy(() -> serviceAccessCommand(realm, 51L, 71L, repository, () -> { })).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        token.setAuthorizationPk(71L).setTokenType(top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum.CODE);
        assertThatThrownBy(() -> serviceAccessCommand(realm, 51L, 71L, repository, () -> { })).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        token.setTokenType(top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum.ACCESS);
        Mockito.when(repository.findByAuthorization(realm, 71L, top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum.ACCESS, 0L, 2)).thenReturn(List.of(token, token));
        assertThatThrownBy(() -> serviceAccessCommand(realm, 51L, 71L, repository, () -> { })).hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

    static void requireServiceRevocationBinding(ServiceAccessRevocationDTO request, IsolatedClientFactDTO client,
            top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties consumer,
            top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties provider) {
        var bindings = consumer.getRpc().getConsumer().getBindings().values().stream()
                .filter(binding -> sha256Bytes(binding.getFullMethodName().getBytes(StandardCharsets.UTF_8)).equals(request.getMethodReference())).toList();
        if (bindings.size() != 1 || bindings.getFirst().getMode() != top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.RpcConsumerModeEnum.SERVICE
                || bindings.getFirst().getScopeContext() != top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext.TENANT
                || !bindings.getFirst().getScopes().equals(request.getScopes()) || !bindings.getFirst().getResourceUri().equals(request.getResourceUri())
                || !client.getRegistrationId().equals(bindings.getFirst().getTenantRegistrationIds().get(request.getTenantId()))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var binding = bindings.getFirst(); var policy = provider.getRpc().getProvider().getMethods().get(binding.getFullMethodName());
        if (!java.util.Objects.equals(client.getAppId(), binding.getAppId()) || !client.getAllowedScopes().containsAll(request.getScopes())
                || !client.getResourceUris().contains(request.getResourceUri().toASCIIString()) || !provider.getRpc().getProvider().isEnabled()
                || policy == null || !policy.isRequired()
                || !Set.of(top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.RpcPrincipalKindEnum.SERVICE,
                           top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.RpcPrincipalKindEnum.USER_OR_SERVICE).contains(policy.getPrincipalKind())
                || policy.getScopeContext() != binding.getScopeContext() || !request.getResourceUri().equals(policy.getExpectedResourceUri())
                || !request.getScopes().equals(policy.getRequiredScopes())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var validation = new ValidationUtils(validators.getValidator()); validation.validate(binding); validation.validate(policy);
        }
    }

    static Map<String, byte[]> serviceRevocationProfiles(Path root, String runId, String digest) throws IOException {
        var ready = strictMapper().readTree(readProtectedBytes(root.resolve("evidence/profiles-ready.json"), root.resolve("evidence"), 128 * 1024));
        if (!digest.equals(ready.path("manifestSha256").asText()) || !runId.equals(ready.path("runId").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var profiles = new java.util.LinkedHashMap<String, byte[]>();
        for (String role : List.of("native-web", "native-service")) {
            Path profile = root.resolve("profiles/" + role + ".properties"); byte[] bytes = readProtectedBytes(profile, root.resolve("profiles"), 2 * 1024 * 1024);
            if (!profile.toString().equals(ready.path("profiles").path(role).path("path").asText())
                    || !sha256Bytes(bytes).equals(ready.path("profiles").path(role).path("sha256").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            profiles.put(role, bytes);
        }
        return profiles;
    }

    @Test
    void serviceRevocationRejectsAnotherApplicationAndAnUnboundProviderMethod() {
        var consumer = new top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties();
        var provider = new top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties();
        String method = "reviewed.CourseFacade/GetCourse"; URI resource = URI.create("https://127.0.0.1:28443/resources/reviewed");
        var binding = top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.RpcConsumerBindingProperties.builder()
                .serviceName("course").group("default").version("v1").fullMethodName(method).appId("reviewed-app")
                .processIdentity(Map.of("applicationName", "reviewed-consumer", "env", "local"))
                .mode(top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.RpcConsumerModeEnum.SERVICE)
                .scopeContext(top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext.TENANT).resourceUri(resource)
                .tenantRegistrationIds(Map.of("1001", "reviewed-registration")).scopes(Set.of("reviewed:course:read")).build();
        consumer.getRpc().getConsumer().setBindings(Map.of("reviewed", binding));
        var policy = top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.RpcProviderMethodProperties.builder()
                .principalKind(top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.RpcPrincipalKindEnum.SERVICE)
                .scopeContext(top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext.TENANT).requiredScopes(binding.getScopes()).expectedResourceUri(resource).build();
        provider.getRpc().getProvider().setEnabled(true).setMethods(Map.of(method, policy));
        var client = IsolatedClientFactDTO.builder().appId("reviewed-app").registrationId("reviewed-registration")
                .allowedScopes(List.copyOf(binding.getScopes())).resourceUris(List.of(resource.toASCIIString())).build();
        var request = ServiceAccessRevocationDTO.builder().methodReference(sha256Bytes(method.getBytes(StandardCharsets.UTF_8)))
                .tenantId("1001").resourceUri(resource).scopes(binding.getScopes()).build();
        assertThatCode(() -> requireServiceRevocationBinding(request, client, consumer, provider)).doesNotThrowAnyException();
        client.setAppId("another-app");
        assertThatThrownBy(() -> requireServiceRevocationBinding(request, client, consumer, provider)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        client.setAppId("reviewed-app"); provider.getRpc().getProvider().setMethods(Map.of());
        assertThatThrownBy(() -> requireServiceRevocationBinding(request, client, consumer, provider)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        provider.getRpc().getProvider().setMethods(Map.of(method, policy));
        for (String fault : List.of("DISABLED", "OPTIONAL", "USER", "PLATFORM", "RESOURCE", "SCOPE")) {
            var changed = top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.RpcProviderMethodProperties.builder()
                    .required(!fault.equals("OPTIONAL"))
                    .principalKind(fault.equals("USER") ? top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.RpcPrincipalKindEnum.USER : policy.getPrincipalKind())
                    .scopeContext(fault.equals("PLATFORM") ? top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext.PLATFORM : policy.getScopeContext())
                    .requiredScopes(fault.equals("SCOPE") ? Set.of("reviewed:course:write") : policy.getRequiredScopes())
                    .expectedResourceUri(fault.equals("RESOURCE") ? URI.create("https://127.0.0.1:28443/resources/foreign") : resource).build();
            provider.getRpc().getProvider().setEnabled(!fault.equals("DISABLED")).setMethods(Map.of(method, changed));
            assertThatThrownBy(() -> requireServiceRevocationBinding(request, client, consumer, provider)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void revokeServiceAccessToken() throws Exception {
        Path repository = Path.of(requiredOperationProperty("xingyuan.isolated.repository-root")), manifest = Path.of(requiredOperationProperty("xingyuan.isolated.manifest-file"));
        String expected = requiredOperationProperty("xingyuan.isolated.revocation-token-sha256");
        if (!expected.matches("[0-9a-f]{64}")) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
        var inputs = readVerifiedDatabaseInputs(manifest, Path.of(requiredOperationProperty("xingyuan.isolated.receipt-file")), repository, "idp");
        Path root = Path.of(inputs.getManifest().getRuntimeRoot()), evidence = bffEvidenceDirectory(inputs);
        var request = strictMapper().readValue(readProtectedBytes(evidence.resolve("service-revocation-request-" + expected + ".json"), evidence, 128 * 1024), ServiceAccessRevocationDTO.class);
        try (var validators = Validation.buildDefaultValidatorFactory()) { new ValidationUtils(validators.getValidator()).validate(request); }
        if (!"READY".equals(request.getStatus()) || !inputs.getManifest().getRunId().equals(request.getRunId()) || !inputs.getManifestSha256().equals(request.getManifestSha256())
                || !expected.equals(request.getTokenSha256()) || request.getConsumerRole().equals(request.getProviderRole()) || request.getDenyApplied() != null
                || !sha256Bytes(request.getAuthorizationId().getBytes(StandardCharsets.UTF_8)).equals(request.getAuthorizationReference())
                || request.getRecordedAt().isAfter(Instant.now()) || request.getRecordedAt().isBefore(Instant.now().minusSeconds(inputs.getManifest().getDeadlines().getOperationSeconds()))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var registry = strictMapper().readValue(readProtectedBytes(root.resolve("secrets/runtime-registry.json"), root.resolve("secrets"), 2 * 1024 * 1024), IsolatedRuntimeRegistryDTO.class);
        runtimeRegistryFacts(inputs, registry); var realm = registry.getRealms().get(request.getTenantCode());
        var clients = registry.getClients().stream().filter(client -> request.getTenantCode().equals(client.getRealmCode()) && request.getClientPk().equals(client.getClientPk().toString())).toList();
        if (clients.size() != 1 || clients.getFirst().getPurpose() != OAuthClientPurposeEnum.SERVICE || clients.getFirst().getClientType() != OAuthClient.ClientType.CONFIDENTIAL
                || clients.getFirst().getStatus() != OAuthClient.Status.ACTIVE || !clients.getFirst().getClientId().equals(request.getClientId())
                || !realm.getIssuer().equals(request.getIssuer()) || !realm.getBusinessTenantId().toString().equals(request.getTenantId())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var profiles = serviceRevocationProfiles(root, inputs.getManifest().getRunId(), inputs.getManifestSha256());
        var sdks = new java.util.LinkedHashMap<String, top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties>();
        for (var entry : profiles.entrySet()) {
            var properties = new java.util.Properties(); properties.load(new java.io.ByteArrayInputStream(entry.getValue()));
            Map<String, Object> flat = new java.util.LinkedHashMap<>(); properties.forEach((key, value) -> flat.put(key.toString(), value));
            sdks.put(entry.getKey(), new org.springframework.boot.context.properties.bind.Binder(new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(flat))
                    .bind("egon.cola.platform.tianquan.shoubing", top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.class)
                    .orElseThrow(() -> new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE")));
        }
        requireServiceRevocationBinding(request, clients.getFirst(), sdks.get(request.getConsumerRole()), sdks.get(request.getProviderRole()));
        try (var context = openOAuthContext(manifest, Path.of(requiredOperationProperty("xingyuan.isolated.receipt-file")), repository, registry.getTenantIds(), true)) {
            var ownership = context.getBean("isolatedOwnershipGuard", Runnable.class); var clock = context.getBean("idpClock", Clock.class);
            Runnable guard = () -> {
                ownership.run();
                try {
                    var current = serviceRevocationProfiles(root, inputs.getManifest().getRunId(), inputs.getManifestSha256());
                    if (profiles.entrySet().stream().anyMatch(entry -> !java.util.Arrays.equals(entry.getValue(), current.get(entry.getKey())))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                } catch (IOException failure) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE", failure); }
            };
            var storage = context.getBean(top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties.class);
            var authorizations = context.getBean("tenantAuthorizationService", org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService.class);
            guard.run();
            var row = withHeldRealmStorageContext(realm, storage, () -> context.getBean(top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthAuthorizationMapper.class)
                    .selectActiveById(realm.getStoragePartitionId(), Long.valueOf(request.getAuthorizationId())));
            if (row == null || !request.getClientPk().equals(row.getClientPk().toString()) || !"SERVICE".equals(row.getPrincipalType()) || !"client_credentials".equals(row.getGrantType())
                    || !List.of(request.getResourceUri().toASCIIString()).equals(row.getAudiences())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            var authorization = withHeldRealmStorageContext(realm, storage, () -> authorizations.findById(request.getAuthorizationId()));
            if (authorization == null || authorization.getAccessToken() == null || !expected.equals(sha256Bytes(authorization.getAccessToken().getToken().getTokenValue().getBytes(StandardCharsets.UTF_8)))) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
            var verified = context.getBean("oracleTenantIssuerManager", top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent.class).authenticate(authorization.getAccessToken().getToken().getTokenValue());
            var jwt = verified.getJwt();
            if (!"SERVICE".equals(jwt.getClaimAsString("principal_type")) || !"TENANT".equals(jwt.getClaimAsString("scope_context")) || !request.getAuthorizationId().equals(verified.getAuthorizationId())
                    || !request.getClientPk().equals(verified.getClientRegistrationId()) || !request.getClientId().equals(jwt.getClaimAsString("client_id"))
                    || !request.getTenantId().equals(jwt.getClaimAsString("tenant_id")) || !List.of(request.getResourceUri().toASCIIString()).equals(jwt.getAudience())
                    || !Set.copyOf(List.of(jwt.getClaimAsString("scope").split(" "))).equals(request.getScopes()) || !request.getExpiresAt().equals(jwt.getExpiresAt())
                    || !request.getExpiresAt().isAfter(clock.instant().plusSeconds(15))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            Path pending = evidence.resolve("service-revocation-running-" + expected + ".json");
            if (!Files.exists(pending, LinkOption.NOFOLLOW_LINKS)) {
                if (!authorization.getAccessToken().isActive()) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                var command = withHeldRealmStorageContext(realm, storage, () -> serviceAccessCommand(realm, clients.getFirst().getClientPk(), row.getId(),
                        context.getBean("oAuthTokenRepository", top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthTokenRepository.class), guard));
                request.setStatus("RUNNING").setRecordedAt(clock.instant()).setDenyApplied(false); publishProtectedBytes(pending, evidence, strictMapper().writeValueAsBytes(request));
                guard.run(); withHeldRealmStorageContext(realm, storage, () -> { context.getBean("tokenRevocationService", top.egon.cola.platform.tianquan.shoubing.admin.token.service.TokenRevocationService.class).revoke(realm, command); return null; });
            } else {
                var previous = strictMapper().readValue(readProtectedBytes(pending, evidence, 128 * 1024), ServiceAccessRevocationDTO.class);
                if (!previous.getAuthorizationReference().equals(request.getAuthorizationReference()) || !previous.getTokenSha256().equals(expected) || !previous.getManifestSha256().equals(inputs.getManifestSha256())) {
                    throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
                }
            }
            var deny = context.getBean("oracleRevocationReader", top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader.class);
            Instant deadline = clock.instant().plusSeconds(inputs.getManifest().getDeadlines().getHttpSeconds()); boolean applied = false;
            while (clock.instant().isBefore(deadline)) {
                guard.run();
                if (deny.check(verified).status() == top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader.Status.REVOKED) {
                    var state = withHeldRealmStorageContext(realm, storage, () -> context.getBean("oAuthRevocationRepository", top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthRevocationRepository.class)
                            .findByTarget(realm, top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthSecurityTargetEnum.JTI, jwt.getId()).orElse(null));
                    if (state != null && "APPLIED".equals(state.getProjectionStatus())) { applied = true; break; }
                }
                Thread.sleep(100);
            }
            if (!applied || !request.getExpiresAt().isAfter(clock.instant())) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
            guard.run();
            var revoked = withHeldRealmStorageContext(realm, storage, () -> context.getBean("oAuthRevocationRepository", top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthRevocationRepository.class)
                    .findByTarget(realm, top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthSecurityTargetEnum.JTI, jwt.getId()).orElseThrow());
            String eventId = sha256Bytes(("oauth-security:" + realm.getRealmKey() + ":JTI:" + jwt.getId() + ':' + revoked.getRevision()).getBytes(StandardCharsets.UTF_8));
            var audit = withHeldRealmStorageContext(realm, storage, () -> context.getBean("oAuthAuditRepository", top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository.class)
                    .findByEventId(realm, eventId).orElseThrow());
            requireServiceRevocationAudit(revoked, audit, jwt.getId(), eventId);
            var revocationProof = strictMapper().createObjectNode().put("schemaVersion", 1).put("runId", inputs.getManifest().getRunId()).put("manifestSha256", inputs.getManifestSha256())
                    .put("status", "PASSED").put("scope", "SERVICE_ACCESS_JTI_ORIGINAL_PG_REVOCATION_AUDIT_AND_APPLIED_DENY")
                    .put("tokenReference", expected).put("authorizationReference", request.getAuthorizationReference()).put("eventReference", sha256Bytes(eventId.getBytes(StandardCharsets.UTF_8)))
                    .put("revision", revoked.getRevision()).put("occurredAt", audit.getOccurredAt().toString()).put("denyApplied", true).put("recordedAt", clock.instant().toString());
            publishProtectedBytes(evidence.resolve("service-revocation-audit-" + expected + ".json"), evidence, strictMapper().writeValueAsBytes(revocationProof));
            request.setStatus("CONFIRMED").setDenyApplied(true).setRecordedAt(clock.instant());
            publishProtectedBytes(evidence.resolve("service-revocation-" + expected + ".json"), evidence, strictMapper().writeValueAsBytes(request));
        }
    }

    static void requireServiceRevocationAudit(top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthRevocationBO revocation,
            top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO audit, String jti, String eventId) {
        if (revocation == null || audit == null || revocation.getTargetType() != top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthSecurityTargetEnum.JTI
                || !jti.equals(revocation.getTargetKey()) || revocation.getRevision() <= 0
                || !"APPLIED".equals(revocation.getProjectionStatus()) || !"XINGYUAN_SERVICE_PROBE_REVOKED".equals(revocation.getReasonCode())
                || !eventId.equals(audit.getEventId()) || !eventId.equals(audit.getPublicId()) || !"OAUTH".equals(audit.getAuditKind())
                || !"TOKEN_REVOKED".equals(audit.getEventType()) || !"SUCCESS".equals(audit.getResult())
                || !revocation.getReasonCode().equals(audit.getReasonCode()) || !Objects.equals(revocation.getRevokedAt(), audit.getOccurredAt())) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
    }

    @Test
    void serviceRevocationAuditMustMatchTheOriginalAppliedJtiRevisionAndPgEvent() {
        var now = Instant.parse("2026-10-15T00:00:00Z");
        var revocation = top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthRevocationBO.builder()
                .targetType(top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthSecurityTargetEnum.JTI).targetKey("owned-jti").revision(2L)
                .reasonCode("XINGYUAN_SERVICE_PROBE_REVOKED").projectionStatus("APPLIED").revokedAt(now).build();
        var audit = top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO.builder().eventId("owned-event").publicId("owned-event")
                .auditKind("OAUTH").eventType("TOKEN_REVOKED").result("SUCCESS").reasonCode("XINGYUAN_SERVICE_PROBE_REVOKED").occurredAt(now).build();
        assertThatCode(() -> requireServiceRevocationAudit(revocation, audit, "owned-jti", "owned-event")).doesNotThrowAnyException();
        assertThatThrownBy(() -> requireServiceRevocationAudit(revocation, audit, "other-jti", "owned-event")).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        revocation.setProjectionStatus("PENDING");
        assertThatThrownBy(() -> requireServiceRevocationAudit(revocation, audit, "owned-jti", "owned-event")).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        revocation.setProjectionStatus("APPLIED"); audit.setEventType("TOKEN_USE_VERIFIED");
        assertThatThrownBy(() -> requireServiceRevocationAudit(revocation, audit, "owned-jti", "owned-event")).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        audit.setEventType("TOKEN_REVOKED").setOccurredAt(now.plusNanos(1000));
        assertThatThrownBy(() -> requireServiceRevocationAudit(revocation, audit, "owned-jti", "owned-event")).hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

    @Test
    void publicFamilyRevocationRequiresTheUniqueOwnedCodeAndNeverAnAccessOrAnotherFamily() {
        var tokens = Mockito.mock(top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthTokenRepository.class);
        var realm = TrustedRealmBO.builder().issuer(URI.create("https://127.0.0.1:28443/tenants/1001"))
                .realmKey("tenant:1001").realmType(RealmTypeEnum.TENANT).storagePartitionId(1001L).businessTenantId(1001L).trustVersion(0L).build();
        var code = top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthTokenBO.builder()
                .id(81L).authorizationPk(71L).tokenType(top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum.CODE)
                .tokenDigest("a".repeat(64)).build();
        Mockito.when(tokens.findByAuthorization(realm, 71L, top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum.CODE, 0L, 2))
                .thenReturn(List.of(code));
        assertThatCode(() -> formalFamilyCommand(realm, 51L, 71L, tokens, () -> { })).doesNotThrowAnyException();
        assertThat(formalFamilyCommand(realm, 51L, 71L, tokens, () -> { }).getTokenDigest()).isEqualTo("a".repeat(64));
        code.setTokenType(top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum.ACCESS);
        assertThatThrownBy(() -> formalFamilyCommand(realm, 51L, 71L, tokens, () -> { })).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        code.setTokenType(top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum.CODE);
        Mockito.when(tokens.findByAuthorization(realm, 71L, top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum.CODE, 0L, 2))
                .thenReturn(List.of(code, code));
        assertThatThrownBy(() -> formalFamilyCommand(realm, 51L, 71L, tokens, () -> { })).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        Mockito.when(tokens.findByAuthorization(realm, 71L, top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum.CODE, 0L, 2))
                .thenReturn(List.of(code));
        code.setAuthorizationPk(72L);
        assertThatThrownBy(() -> formalFamilyCommand(realm, 51L, 71L, tokens, () -> { })).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        Mockito.clearInvocations(tokens);
        assertThatThrownBy(() -> formalFamilyCommand(realm, 51L, 71L, tokens,
                () -> { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); })).hasMessage("XINGYUAN_TARGET_UNSAFE");
        Mockito.verifyNoInteractions(tokens);
    }

    static Map<String, Object> bffOracleRuntimeProperties(VerifiedDatabaseInputsBO inputs) throws IOException {
        Path root = Path.of(inputs.getManifest().getRuntimeRoot());
        var ready = strictMapper().readValue(readProtectedBytes(root.resolve("evidence/profiles-ready.json"), root.resolve("evidence"), 128 * 1024), RuntimeProfilesReceiptDTO.class);
        var reference = ready.getProfiles() == null ? null : ready.getProfiles().get("shoubing");
        Path profile = root.resolve("profiles/shoubing.properties");
        byte[] bytes = readProtectedBytes(profile, root.resolve("profiles"), 2 * 1024 * 1024);
        byte[] registry = readProtectedBytes(root.resolve("secrets/runtime-registry.json"), root.resolve("secrets"), 2 * 1024 * 1024);
        if (ready.getSchemaVersion() != 1 || !inputs.getManifest().getRunId().equals(ready.getRunId())
                || !inputs.getManifestSha256().equals(ready.getManifestSha256()) || !sha256Bytes(registry).equals(ready.getRegistrySha256())
                || reference == null || !profile.toString().equals(reference.getPath()) || !sha256Bytes(bytes).equals(reference.getSha256())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var properties = new java.util.Properties();
        properties.load(new java.io.StringReader(new String(bytes, StandardCharsets.UTF_8)));
        Map<String, Object> selected = new java.util.LinkedHashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("egon.cola.platform.tianquan.shoubing.")) { selected.put(key, properties.getProperty(key)); }
        }
        return Map.copyOf(selected);
    }

    /** Read-only original SAS/SDK owners; this NONE context starts no provider, security chain or scheduler. */
    @lombok.extern.slf4j.Slf4j
    @TestConfiguration(value = "isolatedBffOracleConfiguration", proxyBeanMethods = false)
    @Import({top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.TenantAuthorizationServiceImpl.class,
            top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthTokenIssueServiceImpl.class,
            top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverterImpl.class, BffFamilyProofConverterImpl.class})
    public static class BffOracleConfiguration {
        @Bean("clientCredentialsAccessPolicy")
        top.egon.cola.platform.tianquan.shoubing.core.resource.ClientCredentialsAccessPolicy clientPolicy(
                @Qualifier("resourceServerStore") top.egon.cola.platform.tianquan.shoubing.core.port.ResourceServerStore resources) {
            return new top.egon.cola.platform.tianquan.shoubing.core.resource.ClientCredentialsAccessPolicy(resources);
        }

        @Bean("tenantMembershipPort")
        top.egon.cola.platform.tianquan.shoubing.core.port.TenantMembershipPort memberships(
                @Qualifier("tenantMembershipServiceImpl") TenantMembershipService memberships) {
            return new top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.LocalTenantMembershipPort(memberships);
        }

        @Bean("oracleIdpProperties")
        @org.springframework.boot.context.properties.ConfigurationProperties("egon.cola.platform.tianquan.shoubing")
        top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties verifierProperties() {
            return new top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties();
        }

        @Bean("oracleTrustedIssuerRegistry")
        top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry issuers(
                @Qualifier("oracleIdpProperties") top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties properties) {
            properties.validate();
            return new top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry(properties);
        }

        @Bean("oracleTenantIssuerManager")
        top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent issuerManager(
                @Qualifier("oracleTrustedIssuerRegistry") top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry issuers,
                @Qualifier("oracleIdpProperties") top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties properties,
                @Qualifier("idpClock") Clock clock, org.springframework.beans.factory.config.ConfigurableListableBeanFactory beans) {
            return new top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterAutoConfiguration()
                    .tenantIssuerAuthenticationManagerComponent(issuers, properties, clock,
                            beans.getBeanProvider(top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder.class));
        }

        @Bean("oracleResourceStates")
        top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader resourceStates(
                @Qualifier("rbac3RuntimeRedissonClient") org.redisson.api.RedissonClient redis,
                @Qualifier("jacksonObjectMapper") ObjectMapper mapper,
                @Qualifier("oracleIdpProperties") top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties properties) {
            return new top.egon.cola.platform.tianquan.shoubing.starter.state.RedisIdentityResourceServerStateReader(redis, mapper, properties.getResourceStateKeyPrefix());
        }

        @Bean("oracleClientStates")
        top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader clientStates(
                @Qualifier("rbac3RuntimeRedissonClient") org.redisson.api.RedissonClient redis,
                @Qualifier("jacksonObjectMapper") ObjectMapper mapper,
                @Qualifier("oracleIdpProperties") top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties properties) {
            return new top.egon.cola.platform.tianquan.shoubing.starter.state.RedisIdentityOAuthClientStateReader(redis, mapper, properties.getOauthClientStateKeyPrefix());
        }

        @Bean("oracleRevocationReader")
        top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader revocations(
                @Qualifier("oracleTrustedIssuerRegistry") top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry issuers,
                @Qualifier("oracleIdpProperties") top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties properties,
                org.springframework.beans.factory.config.ConfigurableListableBeanFactory beans) {
            return new top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterAutoConfiguration().oauthRevocationReader(
                    beans.getBeanProvider(org.redisson.api.RedissonClient.class), beans, issuers, beans.getBeanProvider(ObjectMapper.class), properties);
        }

        @Bean("oracleAuthorizationServerSettings")
        org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings settings() {
            return org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings.builder()
                    .multipleIssuersAllowed(true).jwkSetEndpoint("/.well-known/jwks.json").build();
        }

        @Bean("oracleUsageReferences")
        top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent references(
                @Qualifier("isolatedVerifiedDatabaseInputs") VerifiedDatabaseInputsBO inputs,
                @Qualifier("rbac3RuntimeRedissonClient") org.redisson.api.RedissonClient redis,
                @Qualifier("jacksonObjectMapper") ObjectMapper mapper,
                @Qualifier("idpClock") Clock clock, @Qualifier("defaultValidator") Validator validator) throws IOException {
            Path root = Path.of(inputs.getManifest().getRuntimeRoot());
            String reference = inputs.getManifest().getSecretFiles().get("audit-hmac");
            if (reference == null) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
            byte[] key = readProtectedBytes(Path.of(reference), root.resolve("secrets"), 32);
            try { if (key.length != 32) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); } }
            finally { Arrays.fill(key, (byte) 0); }
            String attempt = bffEvidenceDirectory(inputs).getFileName().toString();
            var properties = new top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties(
                    "xingyuan-it:" + inputs.getManifest().getRunId() + ":oauth:audit:v1", root.resolve("spool/oracle/" + attempt), 1_048_576, Path.of(reference));
            return new top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent(redis, mapper, properties,
                    new top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties(), clock, validator);
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class BffOracleBindingBO {
        @NotNull @Valid private IsolatedClientFactDTO client;
        @NotNull @Valid private TrustedRealmBO realm;
        @com.fasterxml.jackson.annotation.JsonIgnore @lombok.ToString.Exclude @NotBlank private String subject;
        @NotBlank @Pattern(regexp = "IDENTITY|RESOURCE") private String kind;
        @NotBlank @Size(max = 128) private String resourceServerId;
        @NotNull private URI readEndpoint;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class BffHeldFamilyBO {
        @NotNull @Valid private BffOracleBindingBO binding;
        @com.fasterxml.jackson.annotation.JsonIgnore @lombok.ToString.Exclude
        @NotNull private org.springframework.security.oauth2.server.authorization.OAuth2Authorization authorization;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String authorizationReference;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String tokenProofReference;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String eventProofReference;
        @NotNull private Instant issuedAt;
        @NotNull private Instant expiresAt;
        private Instant authenticationTime;
        @Min(0) @Max(200) private int beforeStatus;
        @Min(0) @Max(401) private int afterStatus;
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record BffFamilyProofVO(
            @NotBlank @Pattern(regexp = "edge-(ui|shoubing|jianshen|yuheng|tianshu)") String alias,
            @NotBlank @Size(max = 2048) String issuer,
            @NotBlank @Size(max = 128) String registrationId,
            @NotBlank @Pattern(regexp = "[1-9][0-9]{0,18}") String tenantId,
            @NotBlank @Size(max = 2048) String resourceUri,
            @NotBlank @Pattern(regexp = "IDENTITY|RESOURCE") String kind,
            @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String authorizationReference,
            @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String tokenProofReference,
            @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String eventProofReference,
            @NotNull @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant issuedAt,
            @NotNull @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant expiresAt,
            @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant authenticationTime,
            int beforeStatus, int afterStatus) { }

    @org.mapstruct.Mapper(componentModel = "spring", implementationName = "BffFamilyProofConverterImpl", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.ERROR)
    public interface BffFamilyProofConverter extends top.egon.cola.component.common.core.converter.BaseForwardConverter<BffHeldFamilyBO, BffFamilyProofVO> {
        @Override
        @org.mapstruct.BeanMapping(ignoreByDefault = true)
        @org.mapstruct.Mapping(target = "alias", source = "binding.client.alias")
        @org.mapstruct.Mapping(target = "issuer", source = "binding.client.issuer")
        @org.mapstruct.Mapping(target = "registrationId", source = "binding.client.registrationId")
        @org.mapstruct.Mapping(target = "tenantId", source = "binding.realm.businessTenantId")
        @org.mapstruct.Mapping(target = "resourceUri", expression = "java(source.getBinding().getClient().getResourceUris().getFirst())")
        @org.mapstruct.Mapping(target = "kind", source = "binding.kind")
        @org.mapstruct.Mapping(target = "authorizationReference", source = "authorizationReference")
        @org.mapstruct.Mapping(target = "tokenProofReference", source = "tokenProofReference")
        @org.mapstruct.Mapping(target = "eventProofReference", source = "eventProofReference")
        @org.mapstruct.Mapping(target = "issuedAt", source = "issuedAt")
        @org.mapstruct.Mapping(target = "expiresAt", source = "expiresAt")
        @org.mapstruct.Mapping(target = "authenticationTime", source = "authenticationTime")
        @org.mapstruct.Mapping(target = "beforeStatus", source = "beforeStatus")
        @org.mapstruct.Mapping(target = "afterStatus", source = "afterStatus")
        BffFamilyProofVO toTarget(BffHeldFamilyBO source);
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class BffOracleReceiptDTO {
        @Min(1) @Max(1) private int schemaVersion;
        @NotBlank private String runId;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String manifestSha256;
        @NotBlank @Pattern(regexp = "READY|PASSED|FAILED") private String status;
        @NotBlank @Pattern(regexp = "[A-Z_]{1,64}") private String phase;
        @NotNull @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) private Instant recordedAt;
        @NotNull @Size(max = 5) @Valid private List<@NotNull BffFamilyProofVO> proofs;
    }

    @Test
    void bffProofProjectionCannotSerializeTheOriginalMaterializedTokensOrSubject() throws Exception {
        Instant now = Instant.parse("2026-10-15T00:00:00Z");
        var client = org.springframework.security.oauth2.server.authorization.client.RegisteredClient.withId("51")
                .clientId("actual-bff").clientSecret("controlled-client-secret")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://127.0.0.1:28443/login/oauth2/code/actual-bff-a").scope("openid").build();
        var authorization = org.springframework.security.oauth2.server.authorization.OAuth2Authorization.withRegisteredClient(client)
                .id("71").principalName("private-subject").authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .accessToken(new org.springframework.security.oauth2.core.OAuth2AccessToken(org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER,
                        "controlled-access-token", now, now.plusSeconds(300))).build();
        var source = BffHeldFamilyBO.builder().authorization(authorization).binding(BffOracleBindingBO.builder().kind("IDENTITY").subject("private-subject")
                        .realm(TrustedRealmBO.builder().businessTenantId(1001L).build())
                        .client(IsolatedClientFactDTO.builder().alias("edge-ui").issuer("https://127.0.0.1:28443/tenants/1001")
                                .registrationId("actual-bff-a").resourceUris(List.of("https://127.0.0.1:28200")).build()).build())
                .authorizationReference("a".repeat(64)).tokenProofReference("b".repeat(64)).eventProofReference("c".repeat(64))
                .issuedAt(now).expiresAt(now.plusSeconds(300)).beforeStatus(200).afterStatus(401).build();
        var proof = new BffFamilyProofConverterImpl().toTarget(source);
        assertThat(proof.tenantId()).isEqualTo("1001");
        assertThat(proof.beforeStatus()).isEqualTo(200);
        assertThat(proof.afterStatus()).isEqualTo(401);
        assertThat(strictMapper().writeValueAsString(proof)).doesNotContain("private-subject", "controlled-access-token", "controlled-client-secret")
                .contains("2026-10-15T00:00:00Z");
        assertThat(source.toString()).doesNotContain("controlled-access-token", "private-subject");
    }

    private static BffBrowserMarkerDTO awaitBffMarker(VerifiedDatabaseInputsBO inputs, String name, Instant deadline,
            Runnable guard, Clock clock) throws IOException, InterruptedException {
        Path expected = bffEvidenceDirectory(inputs).resolve("browser-" + name + ".json");
        guard.run();
        while (clock.instant().isBefore(deadline)) {
            if (Files.exists(expected, LinkOption.NOFOLLOW_LINKS)) {
                guard.run();
                return readBffBrowserMarker(inputs, name, clock);
            }
            Thread.sleep(java.time.Duration.ofMillis(100));
        }
        throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
    }

    private static void publishBffOracleReceipt(VerifiedDatabaseInputsBO inputs, String phase, String status,
            List<BffHeldFamilyBO> held, BffFamilyProofConverter converter, Clock clock) throws IOException {
        if (!phase.matches("READY|COMPLETE|TENANT_(A|B)_(REFRESH|STEP_UP|HOLD|REPLAY)|TENANT_A_FAULT_(HOLD|REPLAY)")) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
        BffOracleReceiptDTO receipt;
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var validation = new ValidationUtils(validators.getValidator());
            var proofs = held.stream().map(value -> converter.toTarget(validation.validate(value))).toList();
            receipt = validation.validate(BffOracleReceiptDTO.builder().schemaVersion(1).runId(inputs.getManifest().getRunId())
                    .manifestSha256(inputs.getManifestSha256()).status(status).phase(phase).recordedAt(clock.instant()).proofs(proofs).build());
        }
        Path evidence = bffEvidenceDirectory(inputs);
        publishProtectedBytes(evidence.resolve("oracle-" + phase + ".json"), evidence, strictMapper().writeValueAsBytes(receipt));
    }

    static List<BffOracleBindingBO> bffOracleBindings(VerifiedDatabaseInputsBO inputs, IsolatedRuntimeRegistryDTO registry) {
        runtimeRegistryFacts(inputs, registry);
        Map<String, String> paths = Map.of("edge-shoubing", "/api/v1/tianquan-shoubing/clients",
                "edge-jianshen", "/api/tianquan-jianshen/v1/iam/roles", "edge-yuheng", "/api/v1/yuheng/admin/scopes",
                "edge-tianshu", "/api/v1/tianshu/registry/services/page");
        var bindings = new java.util.ArrayList<BffOracleBindingBO>();
        for (String realmCode : List.of("default", "tenant-b")) {
            var realm = registry.getRealms().get(realmCode);
            realm.validateTrustedPartition();
            if (realm.getRealmType() != RealmTypeEnum.TENANT || !realm.getBusinessTenantId().toString().equals(registry.getTenantIds().get(realmCode))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            for (String alias : List.of("edge-ui", "edge-shoubing", "edge-jianshen", "edge-yuheng", "edge-tianshu")) {
                var selected = registry.getClients().stream().filter(client -> realmCode.equals(client.getRealmCode()) && alias.equals(client.getAlias())).toList();
                if (selected.size() != 1) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                var client = selected.getFirst();
                if (client.getClientType() != OAuthClient.ClientType.CONFIDENTIAL || client.getPurpose() != OAuthClientPurposeEnum.USER
                        || client.getStatus() != OAuthClient.Status.ACTIVE || !realm.getIssuer().toASCIIString().equals(client.getIssuer())
                        || client.getResourceUris().size() != 1) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                var resources = registry.getResources().stream().filter(resource -> client.getResourceUris().getFirst().equals(resource.getResourceUri())
                        && "ACTIVE".equals(resource.getStatus())).toList();
                if (resources.size() != 1) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                String endpoint = alias.equals("edge-ui") ? realm.getIssuer().toASCIIString() + "/userinfo"
                        : inputs.getManifest().getPublicOrigin() + paths.get(alias);
                bindings.add(BffOracleBindingBO.builder().client(client).realm(realm).subject(registry.getSubjects().get("admin"))
                        .kind(alias.equals("edge-ui") ? "IDENTITY" : "RESOURCE").resourceServerId(resources.getFirst().getResourceServerId()).readEndpoint(URI.create(endpoint)).build());
            }
        }
        return List.copyOf(bindings);
    }

    private static List<top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO> boundedBffAudits(
            TrustedRealmBO realm, Instant from, Instant to,
            top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository audits, Runnable guard) {
        realm.validateTrustedPartition();
        if (from == null || to == null || !to.isAfter(from) || java.time.Duration.between(from, to).compareTo(java.time.Duration.ofMinutes(15)) > 0) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
        guard.run();
        var rows = audits.findByOccurredAt(realm, from, to, null, 0L, 500);
        if (rows == null || rows.size() > 500 || rows.stream().anyMatch(row -> row == null || row.getId() == null || row.getOccurredAt() == null)) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        if (rows.size() == 500) {
            var last = rows.stream().max(java.util.Comparator
                    .comparing(top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO::getOccurredAt)
                    .thenComparing(top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO::getId)).orElseThrow();
            guard.run();
            if (!audits.findByOccurredAt(realm, from, to, last.getOccurredAt(), last.getId(), 1).isEmpty()) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
        }
        return List.copyOf(rows);
    }

    static Optional<top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO> findBffUsageAudit(
            TrustedRealmBO realm, IsolatedClientFactDTO client, String subject, BffBrowserMarkerDTO marker,
            top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository audits, Runnable guard) {
        realm.validateTrustedPartition();
        if (realm.getRealmType() != RealmTypeEnum.TENANT || client == null || client.getClientPk() == null || client.getClientPk() <= 0L
                || client.getClientType() != OAuthClient.ClientType.CONFIDENTIAL || client.getPurpose() != OAuthClientPurposeEnum.USER
                || client.getStatus() != OAuthClient.Status.ACTIVE || !realm.getIssuer().toASCIIString().equals(client.getIssuer())
                || client.getClientId() == null || client.getClientId().isBlank() || subject == null || subject.isBlank()
                || client.getResourceUris() == null || client.getResourceUris().size() != 1
                || marker.getFromInclusive() == null || marker.getToExclusive() == null
                || !marker.getToExclusive().isAfter(marker.getFromInclusive())
                || java.time.Duration.between(marker.getFromInclusive(), marker.getToExclusive()).compareTo(java.time.Duration.ofSeconds(32)) > 0) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var rows = boundedBffAudits(realm, marker.getFromInclusive(), marker.getToExclusive(), audits, guard);
        String subjectRef = sha256Bytes(subject.getBytes(StandardCharsets.UTF_8));
        String clientRef = sha256Bytes(client.getClientId().getBytes(StandardCharsets.UTF_8));
        var matched = rows.stream().filter(row -> "OAUTH".equals(row.getAuditKind()) && "SUCCESS".equals(row.getResult())
                && "TOKEN_USE_VERIFIED".equals(row.getReasonCode()) && subjectRef.equals(row.getSubjectRef())
                && clientRef.equals(row.getClientRef()) && client.getResourceUris().getFirst().equals(row.getResourceRef())
                && row.getOccurredAt() != null && !row.getOccurredAt().isBefore(marker.getFromInclusive())
                && row.getOccurredAt().isBefore(marker.getToExclusive()) && row.getTokenRef() != null
                && row.getTokenRef().matches("[0-9a-f]{64}") && row.getEventId() != null
                && row.getEventId().matches("[A-Za-z0-9._~-]{1,64}") && row.getTraceId() != null
                && row.getTraceId().matches("[A-Za-z0-9._~-]{1,128}")).toList();
        if (matched.size() > 1) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        return matched.stream().findFirst();
    }

    @Test
    void bffUsageSelectionRequiresOneExactConfirmedActorClientResourceAndActionWindow() {
        var audits = Mockito.mock(top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository.class);
        var realm = TrustedRealmBO.builder().issuer(URI.create("https://127.0.0.1:28443/tenants/1001"))
                .realmKey("tenant:1001").realmType(RealmTypeEnum.TENANT).storagePartitionId(1001L).businessTenantId(1001L).trustVersion(0L).build();
        var client = IsolatedClientFactDTO.builder().clientPk(51L).clientId("actual-bff-a").issuer(realm.getIssuer().toASCIIString())
                .resourceUris(List.of("https://127.0.0.1:28150")).clientType(OAuthClient.ClientType.CONFIDENTIAL)
                .purpose(OAuthClientPurposeEnum.USER).status(OAuthClient.Status.ACTIVE).build();
        Instant start = Instant.parse("2026-10-15T00:00:00Z");
        var marker = BffBrowserMarkerDTO.builder().fromInclusive(start).toExclusive(start.plusSeconds(2)).build();
        var owned = top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO.builder()
                .id(1L).auditKind("OAUTH").result("SUCCESS").reasonCode("TOKEN_USE_VERIFIED").eventId("real-used-event")
                .subjectRef(sha256Bytes("101".getBytes(StandardCharsets.UTF_8))).clientRef(sha256Bytes(client.getClientId().getBytes(StandardCharsets.UTF_8)))
                .resourceRef(client.getResourceUris().getFirst()).tokenRef("a".repeat(64)).traceId("actual-action")
                .occurredAt(start.plusSeconds(1)).build();
        var foreign = top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO.builder()
                .id(2L).auditKind("OAUTH").result("SUCCESS").reasonCode("TOKEN_USE_VERIFIED").eventId("other-event")
                .subjectRef(owned.getSubjectRef()).clientRef("foreign-client").resourceRef(owned.getResourceRef())
                .tokenRef("b".repeat(64)).traceId("other-action").occurredAt(start.plusSeconds(1)).build();
        Mockito.when(audits.findByOccurredAt(realm, start, start.plusSeconds(2), null, 0L, 500)).thenReturn(List.of(owned, foreign));
        assertThat(findBffUsageAudit(realm, client, "101", marker, audits, () -> { })).contains(owned);
        Mockito.when(audits.findByOccurredAt(realm, start, start.plusSeconds(2), null, 0L, 500)).thenReturn(List.of(owned, owned));
        assertThatThrownBy(() -> findBffUsageAudit(realm, client, "101", marker, audits, () -> { })).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        Mockito.clearInvocations(audits);
        assertThatThrownBy(() -> findBffUsageAudit(realm, client, "101", marker, audits,
                () -> { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); })).hasMessage("XINGYUAN_TARGET_UNSAFE");
        Mockito.verifyNoInteractions(audits);
    }

    static Path bffEvidenceDirectory(VerifiedDatabaseInputsBO inputs) throws IOException {
        Path root = Path.of(inputs.getManifest().getRuntimeRoot());
        Path normal = root.resolve("evidence");
        Path selected = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", normal.toString()));
        if (!selected.equals(normal) && (!normal.equals(selected.getParent())
                || !selected.getFileName().toString().matches("verify-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var user = selected.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name"));
        for (Path directory = selected; directory != null; directory = directory.getParent()) {
            if (Files.isSymbolicLink(directory)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            if (directory.startsWith(root) && (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
                    || !Files.getOwner(directory, LinkOption.NOFOLLOW_LINKS).equals(user)
                    || !Files.getPosixFilePermissions(directory, LinkOption.NOFOLLOW_LINKS).equals(PosixFilePermissions.fromString("rwx------")))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
        }
        return selected;
    }

    static BffBrowserMarkerDTO readBffBrowserMarker(VerifiedDatabaseInputsBO inputs, String name, Clock clock) throws IOException {
        if (!name.matches("TENANT_(A|B)_(BEGIN|REFRESH|STEP_UP|HOLD|LOGOUT)|TENANT_A_FAULT_(BEGIN|HOLD|LOGOUT)")) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
        Path evidence = bffEvidenceDirectory(inputs);
        var tree = strictMapper().readTree(readProtectedBytes(evidence.resolve("browser-" + name + ".json"), evidence, 128 * 1024));
        for (String field : List.of("completedAt", "loginStartedAt", "notBefore", "fromInclusive", "toExclusive")) {
            if (tree.has(field) && (!tree.get(field).isTextual() || !tree.get(field).textValue().endsWith("Z"))) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
            }
        }
        var marker = strictMapper().treeToValue(tree, BffBrowserMarkerDTO.class);
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            if (!validators.getValidator().validate(marker).isEmpty()) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
        }
        boolean logout = name.endsWith("_LOGOUT");
        String realm = name.startsWith("TENANT_A_") ? "default" : "tenant-b";
        if (!inputs.getManifest().getRunId().equals(marker.getRunId()) || !inputs.getManifestSha256().equals(marker.getManifestSha256())
                || !(logout ? "CONFIRMED" : "READY").equals(marker.getStatus()) || !logout && !realm.equals(marker.getRealmCode())
                || marker.getCompletedAt().isAfter(clock.instant().plusSeconds(5))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        if (name.endsWith("_BEGIN")) {
            if (marker.getLoginStartedAt() == null || marker.getLoginStartedAt().isAfter(marker.getCompletedAt())) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
            }
        } else if (!logout) {
            if (marker.getFromInclusive() == null || marker.getToExclusive() == null
                    || !marker.getToExclusive().isAfter(marker.getFromInclusive())
                    || marker.getFromInclusive().isAfter(marker.getCompletedAt())
                    || marker.getToExclusive().isAfter(marker.getCompletedAt().plusSeconds(2))
                    || java.time.Duration.between(marker.getFromInclusive(), marker.getToExclusive())
                            .compareTo(java.time.Duration.ofSeconds(inputs.getManifest().getDeadlines().getHttpSeconds() + 2L)) > 0
                    || !name.endsWith("_HOLD") && (marker.getNotBefore() == null || marker.getNotBefore().isAfter(marker.getCompletedAt()))) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
            }
        }
        if (name.endsWith("_HOLD") && (marker.getAliases() == null || marker.getAliases().size() != 5
                || !new java.util.HashSet<>(marker.getAliases()).equals(Set.of("edge-ui", "edge-shoubing", "edge-jianshen", "edge-yuheng", "edge-tianshu")))) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
        return marker;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BffBrowserMarkerDTO {
        @Min(1) @Max(1) private int schemaVersion;
        @NotBlank private String runId;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String manifestSha256;
        @NotBlank @Pattern(regexp = "READY|CONFIRMED") private String status;
        @NotNull @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) private Instant completedAt;
        @Pattern(regexp = "default|tenant-b") private String realmCode;
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) private Instant loginStartedAt;
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) private Instant notBefore;
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) private Instant fromInclusive;
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) private Instant toExclusive;
        @Size(max = 5) private List<@Pattern(regexp = "edge-(ui|shoubing|jianshen|yuheng|tianshu)") String> aliases;
    }

    @Test
    void browserOracleMarkersCannotCrossRunsRealmsOrPrivateAttempts() throws Exception {
        var manifest = intent();
        Path root = Path.of(manifest.getRuntimeRoot());
        Path evidence = root.resolve("evidence");
        Files.createDirectories(evidence);
        Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
        Files.setPosixFilePermissions(evidence, PosixFilePermissions.fromString("rwx------"));
        var inputs = VerifiedDatabaseInputsBO.builder().manifest(manifest).manifestSha256("a".repeat(64)).build();
        Instant now = Instant.parse("2026-10-15T00:00:00Z");
        var marker = BffBrowserMarkerDTO.builder().schemaVersion(1).runId(manifest.getRunId()).manifestSha256(inputs.getManifestSha256())
                .status("READY").realmCode("default").completedAt(now).fromInclusive(now.minusSeconds(1)).toExclusive(now.plusSeconds(1))
                .aliases(List.of("edge-ui", "edge-shoubing", "edge-jianshen", "edge-yuheng", "edge-tianshu")).build();
        Path file = evidence.resolve("browser-TENANT_A_HOLD.json");
        publishProtectedBytes(file, evidence, strictMapper().writeValueAsBytes(marker));
        assertThat(readBffBrowserMarker(inputs, "TENANT_A_HOLD", Clock.fixed(now, java.time.ZoneOffset.UTC)).getRealmCode()).isEqualTo("default");
        Path faultFile = evidence.resolve("browser-TENANT_A_FAULT_HOLD.json");
        publishProtectedBytes(faultFile, evidence, strictMapper().writeValueAsBytes(marker));
        assertThat(readBffBrowserMarker(inputs, "TENANT_A_FAULT_HOLD", Clock.fixed(now, java.time.ZoneOffset.UTC)).getRealmCode()).isEqualTo("default");
        assertThatThrownBy(() -> readBffBrowserMarker(inputs, "TENANT_B_FAULT_HOLD", Clock.fixed(now, java.time.ZoneOffset.UTC)))
                .hasMessage("XINGYUAN_INPUT_INVALID");
        marker.setRealmCode("tenant-b");
        Files.write(file, strictMapper().writeValueAsBytes(marker), StandardOpenOption.TRUNCATE_EXISTING);
        assertThatThrownBy(() -> readBffBrowserMarker(inputs, "TENANT_A_HOLD", Clock.fixed(now, java.time.ZoneOffset.UTC)))
                .hasMessage("XINGYUAN_TARGET_UNSAFE");
        String previous = System.getProperty("xingyuan.isolated.evidence-directory");
        try {
            System.setProperty("xingyuan.isolated.evidence-directory", root.resolve("secrets").toString());
            assertThatThrownBy(() -> bffEvidenceDirectory(inputs)).hasMessage("XINGYUAN_TARGET_UNSAFE");
            Path attempt = evidence.resolve("verify-" + UUID.randomUUID());
            Files.createDirectory(attempt, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            System.setProperty("xingyuan.isolated.evidence-directory", attempt.toString());
            assertThat(bffEvidenceDirectory(inputs)).isEqualTo(attempt);
            assertThatThrownBy(() -> readBffBrowserMarker(inputs, "TENANT_A_HOLD", Clock.fixed(now, java.time.ZoneOffset.UTC)))
                    .hasMessage("XINGYUAN_INPUT_UNAVAILABLE");
        } finally {
            if (previous == null) { System.clearProperty("xingyuan.isolated.evidence-directory"); }
            else { System.setProperty("xingyuan.isolated.evidence-directory", previous); }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class HeldAuthorizationMatchDTO {
        @NotNull @jakarta.validation.constraints.Positive private Long clientPk;
        @com.fasterxml.jackson.annotation.JsonIgnore
        @lombok.ToString.Exclude
        @NotBlank @Size(max = 128) private String principalName;
        @NotBlank @Size(max = 2048) private String resourceUri;
        @NotNull private Instant fromInclusive;
        @NotNull private Instant toExclusive;
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._~-]{1,64}") private String eventId;
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._~-]{1,128}") private String correlationId;
    }

    static List<top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthAuthorizationPO> boundedHeldCandidates(
            TrustedRealmBO realm, HeldAuthorizationMatchDTO match,
            top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthAuthorizationMapper mapper, Runnable ownershipGuard) {
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            if (!validators.getValidator().validate(match).isEmpty()
                    || !match.getToExclusive().isAfter(match.getFromInclusive())
                    || java.time.Duration.between(match.getFromInclusive(), match.getToExclusive()).compareTo(java.time.Duration.ofMinutes(15)) > 0) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
            }
        }
        realm.validateTrustedPartition();
        ownershipGuard.run();
        var rows = mapper.selectByPrincipalName(realm.getStoragePartitionId(), match.getPrincipalName(), "ACTIVE", 0L, 500);
        if (rows == null || rows.size() > 500) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        if (rows.size() == 500) {
            Long after = rows.stream().map(top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthAuthorizationPO::getId)
                    .filter(Objects::nonNull).max(Long::compareTo).orElseThrow(() -> new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"));
            ownershipGuard.run();
            if (!mapper.selectByPrincipalName(realm.getStoragePartitionId(), match.getPrincipalName(), "ACTIVE", after, 1).isEmpty()) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
        }
        return rows.stream().filter(row -> row.getId() != null && row.getId() > 0L
                && Objects.equals(row.getTenantId(), realm.getStoragePartitionId())
                && match.getPrincipalName().equals(row.getPrincipalName()) && "USER".equals(row.getPrincipalType())
                && "ACTIVE".equals(row.getStatus()) && "authorization_code".equals(row.getGrantType())
                && match.getClientPk().equals(row.getClientPk()) && List.of(match.getResourceUri()).equals(row.getAudiences())
                && row.getCreateTime() != null && !row.getCreateTime().isBefore(match.getFromInclusive())
                && row.getCreateTime().isBefore(match.getToExclusive())).toList();
    }

    static org.springframework.security.oauth2.core.OAuth2AccessToken materializeHeldAccessToken(
            TrustedRealmBO realm, HeldAuthorizationMatchDTO match,
            top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthAuthorizationMapper candidates,
            org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService authorizations,
            org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings settings,
            top.egon.cola.platform.tianquan.shoubing.starter.security.IdpJwtVerifier verifier,
            top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository audits,
            top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent references,
            org.redisson.api.RedissonClient redis, String auditStream, Runnable guard, Clock clock,
            top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties properties) throws IOException {
        return materializeHeldAuthorization(realm, match, candidates, authorizations, settings, verifier, audits, references,
                redis, auditStream, guard, clock, properties).getAccessToken().getToken();
    }

    static org.springframework.security.oauth2.server.authorization.OAuth2Authorization materializeHeldAuthorization(
            TrustedRealmBO realm, HeldAuthorizationMatchDTO match,
            top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthAuthorizationMapper candidates,
            org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService authorizations,
            org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings settings,
            top.egon.cola.platform.tianquan.shoubing.starter.security.IdpJwtVerifier verifier,
            top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository audits,
            top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent references,
            org.redisson.api.RedissonClient redis, String auditStream, Runnable guard, Clock clock,
            top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties properties) throws IOException {
        try {
            return withHeldRealmStorageContext(realm, properties, () -> {
                try {
                    return materializeHeldInStorageContext(realm, match, candidates, authorizations, settings, verifier,
                            audits, references, redis, auditStream, guard, clock);
                } catch (IOException unavailableReceipt) {
                    throw new java.io.UncheckedIOException(unavailableReceipt);
                }
            });
        } catch (java.io.UncheckedIOException unavailableReceipt) {
            throw unavailableReceipt.getCause();
        }
    }

    static top.egon.cola.platform.tianquan.shoubing.core.audit.TokenUsageAuditEvent confirmedStoredUsageEvent(TrustedRealmBO realm,
            top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO row, Map<String, String> fields,
            String receiptJson, top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent references) throws IOException {
        Set<String> required = Set.of("eventId", "schemaVersion", "issuer", "resource", "result", "reasonCode", "eventType", "auditKind", "occurredAt", "signature");
        Set<String> allowed = Set.of("eventId", "schemaVersion", "issuer", "resource", "subjectRef", "clientRef", "tokenRef", "result", "reasonCode", "eventType", "auditKind", "occurredAt", "traceId", "signature");
        if (fields == null || !fields.keySet().containsAll(required) || !allowed.containsAll(fields.keySet())
                || fields.values().stream().anyMatch(value -> value == null || value.length() > 4096)
                || !"1".equals(fields.get("schemaVersion")) || !"OAUTH".equals(fields.get("auditKind"))
                || !Objects.equals(fields.get("reasonCode"), fields.get("eventType")) || receiptJson == null || receiptJson.length() > 16 * 1024) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        Map<String, Object> projection = new java.util.LinkedHashMap<>(fields);
        projection.remove("auditKind"); projection.remove("eventType"); projection.remove("signature"); projection.put("schemaVersion", 1);
        var event = strictMapper().convertValue(projection, top.egon.cola.platform.tianquan.shoubing.core.audit.TokenUsageAuditEvent.class);
        var jdbc = new org.postgresql.jdbc.TimestampUtils(false, () -> java.util.TimeZone.getTimeZone("UTC"));
        Instant pgOccurredAt;
        try { pgOccurredAt = jdbc.toOffsetDateTime(jdbc.toString(event.occurredAt().atOffset(java.time.ZoneOffset.UTC))).toInstant(); }
        catch (java.sql.SQLException invalidTimestamp) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED", invalidTimestamp); }
        if (!realm.getIssuer().equals(event.issuer()) || !realm.getIssuer().toASCIIString().equals(fields.get("issuer"))
                || !event.occurredAt().toString().equals(fields.get("occurredAt")) || !Objects.equals(row.getEventId(), event.eventId())
                || !"OAUTH".equals(row.getAuditKind()) || !Objects.equals(row.getEventType(), event.reasonCode())
                || !Objects.equals(row.getResourceRef(), event.resource()) || !Objects.equals(row.getResult(), event.result())
                || !Objects.equals(row.getReasonCode(), event.reasonCode()) || !Objects.equals(row.getTraceId(), event.traceId())
                || !Objects.equals(row.getSubjectRef(), event.subjectRef()) || !Objects.equals(row.getClientRef(), event.clientRef())
                || !Objects.equals(row.getTokenRef(), event.tokenRef()) || !pgOccurredAt.equals(row.getOccurredAt())
                || row.getPayload() == null || !row.getPayload().isObject() || !row.getPayload().isEmpty()
                || !references.verifySignature(event.issuer().toASCIIString(), event.tokenRef(), fields.get("signature"))) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        var receipt = strictMapper().readValue(receiptJson, top.egon.cola.platform.tianquan.shoubing.starter.audit.domain.dto.TokenUsageAuditReceiptDTO.class);
        if (!"STORED".equals(receipt.getOutcome()) || receipt.getConfirmedAt() == null
                || receipt.getConfirmedAt().isBefore(event.occurredAt())
                || !receipt.equals(references.createReceipt(event, "STORED", receipt.getConfirmedAt()))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        return event;
    }

    static Map<String, String> confirmedAuditStreamFields(org.redisson.api.RedissonClient redis, String stream, String eventId, Runnable guard) {
        if (!stream.matches("xingyuan-it:[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}:oauth:audit:v1")
                || eventId == null || !eventId.matches("[A-Za-z0-9._~-]{1,64}")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        guard.run();
        var rows = redis.<String, String>getStream(stream, org.redisson.client.codec.StringCodec.INSTANCE)
                .range(4097, org.redisson.api.StreamMessageId.MIN, org.redisson.api.StreamMessageId.MAX);
        if (rows == null || rows.size() > 4096) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        Map<String, String> selected = null;
        for (var fields : rows.values()) {
            if (fields == null || fields.size() > 16 || fields.values().stream().anyMatch(value -> value == null || value.length() > 4096)) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
            if (!eventId.equals(fields.get("eventId"))) { continue; }
            if (selected != null && !selected.equals(fields)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            selected = fields;
        }
        if (selected == null) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        guard.run(); return Map.copyOf(selected);
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class HttpAuditObservationDTO {
        @NotBlank @Pattern(regexp = "(?!0{32})[0-9a-f]{32}") private String traceId;
        @Min(100) @Max(599) private int status;
        @NotNull @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) private Instant startedAt;
        @NotNull @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) private Instant completedAt;
    }

    private static void requireHttpAuditFields(com.fasterxml.jackson.databind.JsonNode document, Set<String> expected) {
        if (document == null) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
        var actual = new HashSet<String>(); document.fieldNames().forEachRemaining(actual::add);
        if (!document.isObject() || !actual.equals(expected)) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
    }

    private static Instant httpAuditTime(com.fasterxml.jackson.databind.JsonNode document, String field) {
        var value = document.path(field);
        if (!value.isTextual() || value.textValue().length() > 64 || !value.textValue().endsWith("Z")) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
        try { return Instant.parse(value.textValue()); }
        catch (java.time.format.DateTimeParseException invalid) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID", invalid); }
    }

    static void requireHttpTraceAuditRows(List<top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO> rows,
            HttpAuditObservationDTO request, String resource) {
        Set<String> expected = request.getStatus() == 200 ? Set.of("SUCCESS:TOKEN_USE_VERIFIED")
                : request.getStatus() == 403 ? Set.of("SUCCESS:TOKEN_USE_VERIFIED", "DENIED:AUTHORIZATION_DENIED")
                : request.getStatus() == 401 ? Set.of("FAILED:AUTHENTICATION_FAILED") : Set.of();
        var decisions = new HashSet<String>(); var events = new HashSet<String>(); List<String> proofReferences = null;
        if (expected.isEmpty() || rows == null || rows.size() != expected.size()) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        for (var row : rows) {
            if (row == null || !"OAUTH".equals(row.getAuditKind()) || !request.getTraceId().equals(row.getTraceId()) || !resource.equals(row.getResourceRef())
                    || row.getOccurredAt() == null || row.getOccurredAt().isBefore(request.getStartedAt().minusMillis(1))
                    || row.getOccurredAt().isAfter(request.getCompletedAt().plusMillis(1)) || row.getEventId() == null || !events.add(row.getEventId())
                    || !Objects.equals(row.getEventType(), row.getReasonCode()) || !decisions.add(row.getResult() + ':' + row.getReasonCode())) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
            if (request.getStatus() == 401) {
                if (row.getSubjectRef() != null || row.getClientRef() != null || row.getTokenRef() != null) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            } else {
                if (row.getTokenRef() == null || !row.getTokenRef().matches("[0-9a-f]{64}") || row.getSubjectRef() == null || !row.getSubjectRef().matches("[0-9a-f]{64}")
                        || row.getClientRef() == null || !row.getClientRef().matches("[0-9a-f]{64}")) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                var actual = List.of(row.getSubjectRef(), row.getClientRef(), row.getTokenRef());
                if (proofReferences != null && !proofReferences.equals(actual)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                proofReferences = actual;
            }
        }
        if (!decisions.equals(expected)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
    }

    @Test
    void httpTraceAuditRequiresOneOriginalUseAndOneDenyWithoutUntrustedProofLeakage() throws Exception {
        Instant now = Instant.parse("2026-10-15T00:00:00Z"); String resource = "https://127.0.0.1:28140";
        var request = HttpAuditObservationDTO.builder().traceId("a".repeat(32)).status(403).startedAt(now).completedAt(now.plusSeconds(1)).build();
        var used = top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO.builder().id(1L).eventId("used").auditKind("OAUTH")
                .eventType("TOKEN_USE_VERIFIED").result("SUCCESS").reasonCode("TOKEN_USE_VERIFIED").traceId(request.getTraceId()).resourceRef(resource)
                .tokenRef("b".repeat(64)).subjectRef("c".repeat(64)).clientRef("d".repeat(64)).occurredAt(now.plusMillis(100)).build();
        var denied = top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO.builder().id(2L).eventId("denied").auditKind("OAUTH")
                .eventType("AUTHORIZATION_DENIED").result("DENIED").reasonCode("AUTHORIZATION_DENIED").traceId(request.getTraceId()).resourceRef(resource)
                .tokenRef(used.getTokenRef()).subjectRef(used.getSubjectRef()).clientRef(used.getClientRef()).occurredAt(used.getOccurredAt()).build();
        assertThatCode(() -> requireHttpTraceAuditRows(List.of(used, denied), request, resource)).doesNotThrowAnyException();
        for (String fault : List.of("duplicateDeny", "missingUse", "differentToken", "differentSubject", "differentClient", "otherTrace", "otherResource", "outsideWindow")) {
            var originalToken = denied.getTokenRef(); var originalTrace = denied.getTraceId(); var originalResource = denied.getResourceRef(); var originalTime = denied.getOccurredAt();
            var originalSubject = denied.getSubjectRef(); var originalClient = denied.getClientRef();
            if (fault.equals("differentToken")) { denied.setTokenRef("e".repeat(64)); }
            if (fault.equals("differentSubject")) { denied.setSubjectRef("e".repeat(64)); }
            if (fault.equals("differentClient")) { denied.setClientRef("e".repeat(64)); }
            if (fault.equals("otherTrace")) { denied.setTraceId("f".repeat(32)); }
            if (fault.equals("otherResource")) { denied.setResourceRef("https://foreign.invalid"); }
            if (fault.equals("outsideWindow")) { denied.setOccurredAt(now.minusSeconds(1)); }
            var rows = fault.equals("duplicateDeny") ? List.of(used, denied, denied) : fault.equals("missingUse") ? List.of(denied, denied) : List.of(used, denied);
            assertThatThrownBy(() -> requireHttpTraceAuditRows(rows, request, resource)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
            denied.setTokenRef(originalToken).setSubjectRef(originalSubject).setClientRef(originalClient).setTraceId(originalTrace).setResourceRef(originalResource).setOccurredAt(originalTime);
        }
        request.setStatus(401);
        denied.setEventType("AUTHENTICATION_FAILED").setResult("FAILED").setReasonCode("AUTHENTICATION_FAILED");
        assertThatThrownBy(() -> requireHttpTraceAuditRows(List.of(denied), request, resource)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        denied.setTokenRef(null).setClientRef(null).setSubjectRef(null);
        assertThatCode(() -> requireHttpTraceAuditRows(List.of(denied), request, resource)).doesNotThrowAnyException();
        request.setStatus(200);
        denied.setResult("SUCCESS").setEventType("TOKEN_USE_VERIFIED").setReasonCode("TOKEN_USE_VERIFIED")
                .setTokenRef(used.getTokenRef()).setClientRef(used.getClientRef()).setSubjectRef(used.getSubjectRef());
        assertThatCode(() -> requireMcpRelayAuditRows(List.of(used, denied), request, resource)).doesNotThrowAnyException();
        assertThatThrownBy(() -> requireMcpRelayAuditRows(List.of(used, used), request, resource)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        denied.setTokenRef("e".repeat(64));
        assertThatThrownBy(() -> requireMcpRelayAuditRows(List.of(used, denied), request, resource)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void observePlatformHttpAudits() throws Exception {
        Path repository = Path.of(requiredOperationProperty("xingyuan.isolated.repository-root")), manifest = Path.of(requiredOperationProperty("xingyuan.isolated.manifest-file"));
        Path receipt = Path.of(requiredOperationProperty("xingyuan.isolated.receipt-file"));
        var inputs = readVerifiedDatabaseInputs(manifest, receipt, repository, "idp"); Path root = Path.of(inputs.getManifest().getRuntimeRoot());
        requireMatchingCheckpoint(root.resolve("evidence/identity.json"), root.resolve("evidence"), inputs, "COMMITTED");
        Path evidence = bffEvidenceDirectory(inputs), source = evidence.resolve("resource-platform-http-boundaries.json");
        byte[] sourceBytes = readProtectedBytes(source, evidence, 128 * 1024), registryBytes = readProtectedBytes(root.resolve("secrets/runtime-registry.json"), root.resolve("secrets"), 2 * 1024 * 1024);
        var document = strictMapper().readTree(sourceBytes); var registry = strictMapper().readValue(registryBytes, IsolatedRuntimeRegistryDTO.class);
        requireHttpAuditFields(document, Set.of("schemaVersion", "runId", "manifestSha256", "scope", "observations", "status", "recordedAt"));
        if (!document.path("schemaVersion").isIntegralNumber() || !document.path("schemaVersion").canConvertToInt() || document.path("schemaVersion").intValue() != 1
                || !inputs.getManifest().getRunId().equals(document.path("runId").asText()) || !inputs.getManifestSha256().equals(document.path("manifestSha256").asText())
                || !"PASSED".equals(document.path("status").asText()) || !"FOUR_PLATFORM_DIRECT_HTTP_AUTHENTICATION_AND_COUNTERS".equals(document.path("scope").asText())
                || !document.path("observations").isArray() || document.path("observations").size() != 8) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        Instant recorded = httpAuditTime(document, "recordedAt");
        Instant now = Instant.now(); long seconds = inputs.getManifest().getDeadlines().getOperationSeconds();
        if (recorded.isAfter(now) || recorded.isBefore(now.minusSeconds(seconds))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        var bindings = bffOracleBindings(inputs, registry); var roles = Set.of("shoubing", "jianshen", "yuheng", "tianshu");
        var scopes = new HashSet<String>(); var traces = new HashSet<String>(); var frames = new java.util.LinkedHashSet<Path>();
        var output = strictMapper().createObjectNode().put("schemaVersion", 1).put("runId", inputs.getManifest().getRunId()).put("manifestSha256", inputs.getManifestSha256())
                .put("scope", "FOUR_PLATFORM_HTTP_PG_STREAM_SIGNED_RECEIPTS_ONLY").put("sourceSha256", sha256Bytes(sourceBytes)); var proofs = output.putArray("proofs");
        try (var context = openOAuthContext(manifest, receipt, repository, registry.getTenantIds(), true); var validators = Validation.buildDefaultValidatorFactory()) {
            Runnable ownership = context.getBean("isolatedOwnershipGuard", Runnable.class);
            Runnable guard = () -> {
                ownership.run();
                try {
                    if (!MessageDigest.isEqual(sourceBytes, readProtectedBytes(source, evidence, 128 * 1024))
                            || !MessageDigest.isEqual(registryBytes, readProtectedBytes(root.resolve("secrets/runtime-registry.json"), root.resolve("secrets"), 2 * 1024 * 1024))) {
                        throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
                    }
                } catch (IOException changed) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE", changed); }
            };
            var validation = new ValidationUtils(validators.getValidator()); Instant deadline = context.getBean("idpClock", Clock.class).instant().plusSeconds(seconds);
            for (var observation : document.path("observations")) {
                requireHttpAuditFields(observation, Set.of("role", "realmCode", "tenantId", "tokenReference", "viewerReference", "foreignAudienceReference", "successStatus", "pdpDeniedStatus",
                        "authenticationDeniedStatus", "tamperedRequestCount", "before", "successful", "denied", "recovered", "requests", "identityOnlyTokenReference", "identityOnlyScopes", "identityOnlyExpiresAt"));
                String role = observation.path("role").asText(), realmCode = observation.path("realmCode").asText();
                if (!roles.contains(role) || !Set.of("default", "tenant-b").contains(realmCode) || !scopes.add(realmCode + ':' + role)
                        || !registry.getTenantIds().get(realmCode).equals(observation.path("tenantId").asText())
                        || !observation.path("identityOnlyTokenReference").asText().matches("[0-9a-f]{64}")
                        || !observation.path("identityOnlyScopes").isArray() || observation.path("identityOnlyScopes").size() != 1 || !"openid".equals(observation.path("identityOnlyScopes").get(0).asText())
                        || !httpAuditTime(observation, "identityOnlyExpiresAt").isAfter(recorded)
                        || !observation.path("requests").isArray() || observation.path("requests").size() != 10) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                var selected = bindings.stream().filter(value -> realmCode.equals(value.getClient().getRealmCode()) && ("edge-" + role).equals(value.getClient().getAlias())).toList();
                if (selected.size() != 1) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                URI resource = URI.create(selected.getFirst().getClient().getResourceUris().getFirst());
                int sequence = 0;
                for (var requestNode : observation.path("requests")) {
                    requireHttpAuditFields(requestNode, Set.of("traceId", "status", "startedAt", "completedAt"));
                    var request = validation.validate(strictMapper().treeToValue(requestNode, HttpAuditObservationDTO.class));
                    Instant start = httpAuditTime(requestNode, "startedAt"), end = httpAuditTime(requestNode, "completedAt");
                    int status = sequence == 0 || sequence == 9 ? 200 : sequence == 1 || sequence == 2 ? 403 : 401; sequence++;
                    if (request.getStatus() != status || !traces.add(request.getTraceId()) || start.isBefore(recorded.minusSeconds(seconds)) || end.isAfter(recorded)
                            || end.isBefore(start) || java.time.Duration.between(start, end).compareTo(java.time.Duration.ofSeconds(inputs.getManifest().getDeadlines().getHttpSeconds())) > 0) {
                        throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
                    }
                    var item = observeOneHttpTrace(context, inputs, registry, request, realmCode, role, resource, guard, deadline, frames, null);
                    proofs.add(item);
                }
            }
            awaitOwnedAuditFrameCleanup(inputs, frames, guard, deadline);
            guard.run();
        }
        if (scopes.size() != 8 || traces.size() != 80 || frames.size() != 96) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        output.put("status", "PASSED").put("requestCount", traces.size()).put("storedEventCount", frames.size()).put("spoolAbsentAfterReceiptCount", frames.size()).put("recordedAt", Instant.now().toString());
        publishProtectedBytes(evidence.resolve("protocol-platform-http-audits.json"), evidence, strictMapper().writeValueAsBytes(output));
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void observeMcpHttpAudits() throws Exception {
        Path repository = Path.of(requiredOperationProperty("xingyuan.isolated.repository-root")), manifestFile = Path.of(requiredOperationProperty("xingyuan.isolated.manifest-file"));
        Path database = Path.of(requiredOperationProperty("xingyuan.isolated.receipt-file")); var inputs = readVerifiedDatabaseInputs(manifestFile, database, repository, "idp");
        Path root = Path.of(inputs.getManifest().getRuntimeRoot()), evidence = bffEvidenceDirectory(inputs), source = evidence.resolve("resource-mcp-boundaries.json");
        Path registryFile = root.resolve("secrets/runtime-registry.json"); byte[] raw = readProtectedBytes(source, evidence, 128 * 1024), registryBytes = readProtectedBytes(registryFile, root.resolve("secrets"), 2 * 1024 * 1024);
        var document = strictMapper().readTree(raw); var registry = strictMapper().readValue(registryBytes, IsolatedRuntimeRegistryDTO.class); runtimeRegistryFacts(inputs, registry);
        Instant recorded = httpAuditTime(document, "recordedAt"), now = Instant.now(); long seconds = inputs.getManifest().getDeadlines().getOperationSeconds();
        if (!document.path("schemaVersion").isInt() || document.path("schemaVersion").intValue() != 1 || !inputs.getManifest().getRunId().equals(document.path("runId").asText())
                || !inputs.getManifestSha256().equals(document.path("manifestSha256").asText()) || !"PASSED".equals(document.path("status").asText())
                || !"TASK_MCP_REAL_RESOURCE_READ_SCOPE_AND_FINE_PERMISSION_ONLY".equals(document.path("scope").asText()) || !document.path("observations").isArray() || document.path("observations").size() != 2
                || recorded.isAfter(now) || recorded.isBefore(now.minusSeconds(seconds))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var output = strictMapper().createObjectNode().put("schemaVersion", 1).put("runId", inputs.getManifest().getRunId()).put("manifestSha256", inputs.getManifestSha256())
                .put("scope", "MCP_HTTP_PG_STREAM_SIGNED_RECEIPTS_ONLY").put("sourceSha256", sha256Bytes(raw)); var proofs = output.putArray("proofs");
        var realms = new HashSet<String>(); var traces = new HashSet<String>(); var frames = new java.util.LinkedHashSet<Path>();
        try (var context = openOAuthContext(manifestFile, database, repository, registry.getTenantIds(), true); var validators = Validation.buildDefaultValidatorFactory()) {
            var ownership = context.getBean("isolatedOwnershipGuard", Runnable.class); var validation = new ValidationUtils(validators.getValidator());
            Runnable guard = () -> {
                ownership.run();
                try { if (!MessageDigest.isEqual(raw, readProtectedBytes(source, evidence, 128 * 1024)) || !MessageDigest.isEqual(registryBytes, readProtectedBytes(registryFile, root.resolve("secrets"), 2 * 1024 * 1024))) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); } }
                catch (IOException changed) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE", changed); }
            };
            Instant deadline = now.plusSeconds(seconds);
            for (var group : document.path("observations")) {
                String realm = group.path("realmCode").asText();
                if (!Set.of("default", "tenant-b").contains(realm) || !realms.add(realm) || !registry.getTenantIds().get(realm).equals(group.path("tenantId").asText())
                        || !group.path("requests").isArray() || group.path("requests").size() != 6) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                var bindings = bffOracleBindings(inputs, registry).stream().filter(value -> realm.equals(value.getClient().getRealmCode()) && "edge-yuheng".equals(value.getClient().getAlias())).toList();
                if (bindings.size() != 1 || !bindings.getFirst().getClient().getResourceUris().equals(List.of(group.path("resourceUri").asText()))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                int ordinal = 0;
                for (var observation : group.path("requests")) {
                    String kind = List.of("SUCCESS", "FINE_DENIED", "SCOPE_DENIED", "WRONG_AUDIENCE", "MISSING", "RECOVERY").get(ordinal);
                    int status = ordinal == 0 || ordinal == 5 ? 200 : ordinal == 1 || ordinal == 2 ? 403 : 401; ordinal++;
                    var request = validation.validate(strictMapper().treeToValue(observation.path("request"), HttpAuditObservationDTO.class));
                    requireHttpAuditFields(observation.path("request"), Set.of("traceId", "status", "startedAt", "completedAt"));
                    if (!kind.equals(observation.path("kind").asText()) || request.getStatus() != status || !traces.add(request.getTraceId())
                            || !observation.path("responseSha256").asText().matches("[0-9a-f]{64}") || request.getStartedAt().isBefore(recorded.minusSeconds(seconds))
                            || request.getCompletedAt().isBefore(request.getStartedAt()) || request.getCompletedAt().isAfter(recorded)
                            || java.time.Duration.between(request.getStartedAt(), request.getCompletedAt()).compareTo(java.time.Duration.ofSeconds(inputs.getManifest().getDeadlines().getHttpSeconds())) > 0) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                    proofs.add(observeOneHttpTrace(context, inputs, registry, request, realm, "mcp", URI.create(group.path("resourceUri").asText()), guard, deadline, frames, null));
                }
            }
            awaitOwnedAuditFrameCleanup(inputs, frames, guard, deadline); guard.run();
        }
        int events = java.util.stream.StreamSupport.stream(proofs.spliterator(), false).mapToInt(value -> value.path("events").size()).sum();
        if (realms.size() != 2 || traces.size() != 12 || events != 20 || frames.size() != 28) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        output.put("status", "PASSED").put("requestCount", 12).put("storedEventCount", 20).put("spoolAbsentAfterReceiptCount", 20).put("checkedSpoolCandidateCount", frames.size()).put("recordedAt", Instant.now().toString());
        publishProtectedBytes(evidence.resolve("protocol-mcp-audits.json"), evidence, strictMapper().writeValueAsBytes(output));
    }

    record ExpectedUsageReferencesVO(@NotBlank @Pattern(regexp = "[0-9a-f]{64}") String subjectRef,
                                     @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String clientRef,
                                     @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String tokenRef) { }

    static void requireExpectedUsageReferences(List<top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO> rows, ExpectedUsageReferencesVO expected) {
        for (var row : rows) {
            if (!expected.subjectRef().equals(row.getSubjectRef()) || !expected.clientRef().equals(row.getClientRef()) || !expected.tokenRef().equals(row.getTokenRef())) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
        }
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void observeNativeUserInfoAudits() throws Exception {
        Path repository = Path.of(requiredOperationProperty("xingyuan.isolated.repository-root")), manifest = Path.of(requiredOperationProperty("xingyuan.isolated.manifest-file"));
        Path receipt = Path.of(requiredOperationProperty("xingyuan.isolated.receipt-file")); var inputs = readVerifiedDatabaseInputs(manifest, receipt, repository, "idp");
        Path root = Path.of(inputs.getManifest().getRuntimeRoot()), evidence = bffEvidenceDirectory(inputs), source = evidence.resolve("resource-native-userinfo.json");
        byte[] sourceBytes = readProtectedBytes(source, evidence, 128 * 1024), registryBytes = readProtectedBytes(root.resolve("secrets/runtime-registry.json"), root.resolve("secrets"), 2 * 1024 * 1024);
        byte[] indexBytes = readProtectedBytes(root.resolve("secrets/token-index.json"), root.resolve("secrets"), 128 * 1024);
        var registry = strictMapper().readValue(registryBytes, IsolatedRuntimeRegistryDTO.class); runtimeRegistryFacts(inputs, registry);
        var document = strictMapper().readTree(sourceBytes); var index = strictMapper().readTree(indexBytes);
        requireHttpAuditFields(document, Set.of("schemaVersion", "runId", "manifestSha256", "scope", "status", "recordedAt", "observations"));
        if (!document.path("schemaVersion").isInt() || document.path("schemaVersion").intValue() != 1 || !inputs.getManifest().getRunId().equals(document.path("runId").asText())
                || !inputs.getManifestSha256().equals(document.path("manifestSha256").asText()) || !"PASSED".equals(document.path("status").asText())
                || !"NATIVE_USERINFO_HTTP_REQUESTS_ONLY".equals(document.path("scope").asText()) || !document.path("observations").isArray() || document.path("observations").size() != 2
                || !index.path("schemaVersion").isInt() || index.path("schemaVersion").intValue() != 1 || !index.path("tokens").isArray() || index.path("tokens").isEmpty() || index.path("tokens").size() > 128
                || !inputs.getManifest().getRunId().equals(index.path("runId").asText()) || !inputs.getManifestSha256().equals(index.path("manifestSha256").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        Instant recorded = httpAuditTime(document, "recordedAt"), now = Instant.now(); long seconds = inputs.getManifest().getDeadlines().getOperationSeconds();
        if (recorded.isAfter(now) || recorded.isBefore(now.minusSeconds(seconds))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var bindings = bffOracleBindings(inputs, registry); var realms = new HashSet<String>(); var traces = new HashSet<String>(); var frames = new java.util.LinkedHashSet<Path>();
        var output = strictMapper().createObjectNode().put("schemaVersion", 1).put("runId", inputs.getManifest().getRunId()).put("manifestSha256", inputs.getManifestSha256())
                .put("scope", "NATIVE_USERINFO_PG_STREAM_SIGNED_RECEIPTS_ONLY").put("sourceSha256", sha256Bytes(sourceBytes)); var proofs = output.putArray("proofs");
        try (var context = openOAuthContext(manifest, receipt, repository, registry.getTenantIds(), true); var validators = Validation.buildDefaultValidatorFactory()) {
            var ownership = context.getBean("isolatedOwnershipGuard", Runnable.class); var validation = new ValidationUtils(validators.getValidator());
            Runnable guard = () -> {
                ownership.run();
                try {
                    if (!MessageDigest.isEqual(sourceBytes, readProtectedBytes(source, evidence, 128 * 1024))
                            || !MessageDigest.isEqual(indexBytes, readProtectedBytes(root.resolve("secrets/token-index.json"), root.resolve("secrets"), 128 * 1024))
                            || !MessageDigest.isEqual(registryBytes, readProtectedBytes(root.resolve("secrets/runtime-registry.json"), root.resolve("secrets"), 2 * 1024 * 1024))) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
                } catch (IOException failure) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE", failure); }
            };
            Instant deadline = now.plusSeconds(seconds);
            for (var observation : document.path("observations")) {
                requireHttpAuditFields(observation, Set.of("realmCode", "issuer", "resourceUri", "tokenReference", "requests")); String realm = observation.path("realmCode").asText();
                if (!Set.of("default", "tenant-b").contains(realm) || !realms.add(realm) || !registry.getRealms().get(realm).getIssuer().toASCIIString().equals(observation.path("issuer").asText())
                        || !observation.path("tokenReference").asText().matches("[0-9a-f]{64}") || !observation.path("requests").isArray() || observation.path("requests").size() != 3) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                var selected = bindings.stream().filter(binding -> realm.equals(binding.getClient().getRealmCode()) && "edge-shoubing".equals(binding.getClient().getAlias())).toList();
                if (selected.size() != 1 || !selected.getFirst().getClient().getResourceUris().getFirst().equals(observation.path("resourceUri").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                var references = validation.validate(nativeUserInfoReferences(context, root, registry, index, realm, observation.path("tokenReference").asText(), observation.path("resourceUri").asText(), guard));
                int ordinal = 0;
                for (var requestNode : observation.path("requests")) {
                    requireHttpAuditFields(requestNode, Set.of("traceId", "status", "startedAt", "completedAt"));
                    var request = validation.validate(strictMapper().treeToValue(requestNode, HttpAuditObservationDTO.class));
                    Instant started = httpAuditTime(requestNode, "startedAt"), completed = httpAuditTime(requestNode, "completedAt");
                    if (request.getStatus() != (ordinal++ == 1 ? 401 : 200) || !traces.add(request.getTraceId()) || started.isBefore(recorded.minusSeconds(seconds))
                            || completed.isAfter(recorded) || completed.isBefore(started)
                            || java.time.Duration.between(started, completed).compareTo(java.time.Duration.ofSeconds(inputs.getManifest().getDeadlines().getHttpSeconds())) > 0) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                    proofs.add(observeOneHttpTrace(context, inputs, registry, request, realm, "shoubing", URI.create(observation.path("resourceUri").asText()), guard, deadline, frames,
                            request.getStatus() == 200 ? references : null));
                }
            }
            awaitOwnedAuditFrameCleanup(inputs, frames, guard, deadline); guard.run();
        }
        if (realms.size() != 2 || traces.size() != 6 || frames.size() != 6) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        output.put("status", "PASSED").put("requestCount", 6).put("storedEventCount", 6).put("spoolAbsentAfterReceiptCount", 6).put("recordedAt", Instant.now().toString());
        publishProtectedBytes(evidence.resolve("protocol-native-userinfo-audits.json"), evidence, strictMapper().writeValueAsBytes(output));
    }

    private static ExpectedUsageReferencesVO nativeUserInfoReferences(AnnotationConfigApplicationContext context, Path root, IsolatedRuntimeRegistryDTO registry,
            com.fasterxml.jackson.databind.JsonNode index, String realm, String tokenHash, String resource, Runnable guard) throws Exception {
        var candidates = java.util.stream.StreamSupport.stream(index.path("tokens").spliterator(), false)
                .filter(value -> realm.equals(value.path("tenantCode").asText()) && "admin".equals(value.path("userKey").asText()) && tokenHash.equals(value.path("tokenSha256").asText())).toList();
        if (candidates.size() != 1) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); } var candidate = candidates.getFirst();
        if (!resource.equals(candidate.path("resourceUri").asText()) || !registry.getRealms().get(realm).getIssuer().toASCIIString().equals(candidate.path("issuer").asText())
                || !registry.getTenantIds().get(realm).equals(candidate.path("tenantId").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var clients = registry.getClients().stream().filter(value -> realm.equals(value.getRealmCode()) && value.getClientId().equals(candidate.path("clientId").asText())
                && value.getClientPk().toString().equals(candidate.path("clientPk").asText()) && value.getPurpose() == OAuthClientPurposeEnum.USER && value.getClientType() == OAuthClient.ClientType.PUBLIC
                && value.getStatus() == OAuthClient.Status.ACTIVE && value.getResourceUris().size() == 1 && value.getResourceUris().getFirst().equals(candidate.path("resourceUri").asText())).toList();
        if (clients.size() != 1) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        guard.run(); byte[] bytes = readProtectedBytes(Path.of(candidate.path("tokenFile").asText()), root.resolve("secrets"), 128 * 1024);
        try {
            if (!tokenHash.equals(sha256Bytes(bytes))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            // Authentication belongs to the actual UserInfo request; parse the same SHA-bound bytes only to correlate its signed audit references.
            var claims = com.nimbusds.jwt.SignedJWT.parse(new String(bytes, StandardCharsets.UTF_8)).getJWTClaimsSet();
            var issuer = registry.getRealms().get(realm).getIssuer(); String subject = registry.getSubjects().get("admin"), clientId = clients.getFirst().getClientId();
            if (!issuer.toASCIIString().equals(claims.getIssuer()) || !subject.equals(claims.getSubject()) || !clientId.equals(String.valueOf(claims.getClaim("client_id")))
                    || !clients.getFirst().getClientPk().toString().equals(String.valueOf(claims.getClaim("client_registration_id")))
                    || !List.of(resource).equals(claims.getAudience()) || !"USER".equals(claims.getClaim("principal_type")) || !"TENANT".equals(claims.getClaim("scope_context"))
                    || !registry.getTenantIds().get(realm).equals(String.valueOf(claims.getClaim("tenant_id")))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            return new ExpectedUsageReferencesVO(sha256Bytes(subject.getBytes(StandardCharsets.UTF_8)), sha256Bytes(clientId.getBytes(StandardCharsets.UTF_8)),
                    context.getBean("oracleUsageReferences", top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent.class).reference(issuer, claims.getJWTID()));
        } finally { java.util.Arrays.fill(bytes, (byte) 0); }
    }

    @Test
    void nativeUserInfoAuditCorrelationCannotSwapTheTokenClientOrSubject() {
        var expected = new ExpectedUsageReferencesVO("a".repeat(64), "b".repeat(64), "c".repeat(64));
        var row = top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO.builder()
                .subjectRef(expected.subjectRef()).clientRef(expected.clientRef()).tokenRef(expected.tokenRef()).build();
        assertThatCode(() -> requireExpectedUsageReferences(List.of(row), expected)).doesNotThrowAnyException();
        for (String field : List.of("subject", "client", "token")) {
            var different = top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO.builder()
                    .subjectRef(field.equals("subject") ? "d".repeat(64) : expected.subjectRef()).clientRef(field.equals("client") ? "d".repeat(64) : expected.clientRef())
                    .tokenRef(field.equals("token") ? "d".repeat(64) : expected.tokenRef()).build();
            assertThatThrownBy(() -> requireExpectedUsageReferences(List.of(different), expected)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        }
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void observeNativeRpcAudits() throws Exception {
        Path repository = Path.of(requiredOperationProperty("xingyuan.isolated.repository-root")), manifest = Path.of(requiredOperationProperty("xingyuan.isolated.manifest-file"));
        var inputs = readVerifiedDatabaseInputs(manifest, Path.of(requiredOperationProperty("xingyuan.isolated.receipt-file")), repository, "idp");
        Path root = Path.of(inputs.getManifest().getRuntimeRoot()), evidence = bffEvidenceDirectory(inputs), source = evidence.resolve("native-rpc-contract-probe.json");
        Path registryFile = root.resolve("secrets/runtime-registry.json"); byte[] sourceBytes = readProtectedBytes(source, evidence, 128 * 1024);
        byte[] registryBytes = readProtectedBytes(registryFile, root.resolve("secrets"), 2 * 1024 * 1024);
        var frozen = new java.util.LinkedHashMap<Path, byte[]>(); frozen.put(source, sourceBytes); frozen.put(registryFile, registryBytes);
        var registry = strictMapper().readValue(registryBytes, IsolatedRuntimeRegistryDTO.class); runtimeRegistryFacts(inputs, registry);
        var document = strictMapper().readTree(sourceBytes); Instant recorded = httpAuditTime(document, "recordedAt"), now = Instant.now();
        long seconds = inputs.getManifest().getDeadlines().getOperationSeconds();
        if (!document.path("schemaVersion").isInt() || document.path("schemaVersion").intValue() != 1 || !inputs.getManifest().getRunId().equals(document.path("runId").asText())
                || !inputs.getManifestSha256().equals(document.path("manifestSha256").asText()) || !"PASSED".equals(document.path("status").asText())
                || !"STANDALONE_NOT_FOUND_PROTOCOL".equals(document.path("scope").asText()) || !document.path("proofs").isArray() || document.path("proofs").size() != 4
                || recorded.isAfter(now) || recorded.isBefore(now.minusSeconds(seconds))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var output = strictMapper().createObjectNode().put("schemaVersion", 1).put("runId", inputs.getManifest().getRunId()).put("manifestSha256", inputs.getManifestSha256())
                .put("scope", "NATIVE_RPC_PG_STREAM_SIGNED_RECEIPTS_ONLY").put("sourceSha256", sha256Bytes(sourceBytes)); var proofs = output.putArray("proofs");
        var groups = new HashSet<String>(); var traces = new HashSet<String>(); var frames = new java.util.LinkedHashSet<Path>();
        try (var context = openOAuthContext(manifest, Path.of(requiredOperationProperty("xingyuan.isolated.receipt-file")), repository, registry.getTenantIds(), true);
             var validators = Validation.buildDefaultValidatorFactory()) {
            var validation = new ValidationUtils(validators.getValidator()); var ownership = context.getBean("isolatedOwnershipGuard", Runnable.class);
            Runnable guard = () -> {
                ownership.run();
                for (var entry : frozen.entrySet()) {
                    try { if (!MessageDigest.isEqual(entry.getValue(), readProtectedBytes(entry.getKey(), root, 2 * 1024 * 1024))) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); } }
                    catch (IOException failure) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE", failure); }
                }
            };
            Instant deadline = now.plusSeconds(seconds);
            for (var group : document.path("proofs")) {
                String role = group.path("providerRole").asText(), hash = group.path("serviceSecurity").path("tokenReference").asText();
                if (!Set.of("native-web", "native-service").contains(role) || !hash.matches("[0-9a-f]{64}") || role.equals(group.path("consumerRole").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                var realmCodes = registry.getTenantIds().entrySet().stream().filter(entry -> entry.getValue().equals(group.path("tenantId").asText())).map(Map.Entry::getKey).toList();
                if (realmCodes.size() != 1 || !groups.add(role + ':' + realmCodes.getFirst())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                String realm = realmCodes.getFirst(); Path requestFile = evidence.resolve("service-revocation-request-" + hash + ".json");
                byte[] requestBytes = readProtectedBytes(requestFile, evidence, 128 * 1024); frozen.put(requestFile, requestBytes);
                var request = validation.validate(strictMapper().readValue(requestBytes, ServiceAccessRevocationDTO.class));
                if (!"READY".equals(request.getStatus()) || !inputs.getManifest().getRunId().equals(request.getRunId()) || !inputs.getManifestSha256().equals(request.getManifestSha256())
                        || !hash.equals(request.getTokenSha256()) || !role.equals(request.getProviderRole()) || !realm.equals(request.getTenantCode())
                        || !group.path("consumerRole").asText().equals(request.getConsumerRole()) || !group.path("methodReference").asText().equals(request.getMethodReference())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                var references = validation.validate(serviceRpcAuditReferences(context, registry, request, guard));
                var requests = List.of(group.path("successfulRequest"), group.path("missingTokenRequest"), group.path("serviceSecurity").path("scopeDeniedRequest"), group.path("serviceSecurity").path("revokedRequest"));
                var statuses = List.of("OK", "UNAUTHENTICATED", "PERMISSION_DENIED", "UNAUTHENTICATED");
                for (int ordinal = 0; ordinal < requests.size(); ordinal++) {
                    var observed = validation.validate(rpcAuditRequest(requests.get(ordinal), statuses.get(ordinal)));
                    if (!traces.add(observed.getTraceId()) || observed.getStartedAt().isBefore(recorded.minusSeconds(seconds)) || observed.getCompletedAt().isAfter(recorded)
                            || observed.getCompletedAt().isBefore(observed.getStartedAt())
                            || java.time.Duration.between(observed.getStartedAt(), observed.getCompletedAt()).compareTo(java.time.Duration.ofSeconds(inputs.getManifest().getDeadlines().getHttpSeconds())) > 0) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                    var item = observeOneHttpTrace(context, inputs, registry, observed, realm, role, request.getResourceUri(), guard, deadline, frames, observed.getStatus() == 401 ? null : references);
                    item.remove("status"); item.put("rpcStatus", statuses.get(ordinal)); proofs.add(item);
                }
            }
            awaitOwnedAuditFrameCleanup(inputs, frames, guard, deadline); guard.run();
        }
        if (groups.size() != 4 || traces.size() != 16 || frames.size() != 20) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        output.put("status", "PASSED").put("requestCount", 16).put("storedEventCount", 20).put("spoolAbsentAfterReceiptCount", 20).put("recordedAt", Instant.now().toString());
        publishProtectedBytes(evidence.resolve("protocol-native-rpc-audits.json"), evidence, strictMapper().writeValueAsBytes(output));
    }

    static HttpAuditObservationDTO rpcAuditRequest(com.fasterxml.jackson.databind.JsonNode node, String expected) {
        requireHttpAuditFields(node, Set.of("traceId", "status", "startedAt", "completedAt"));
        if (!expected.equals(node.path("status").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        // Normalize only the expected usage-audit decision; the evidence retains the actual gRPC status.
        int decision = switch (expected) { case "OK" -> 200; case "PERMISSION_DENIED" -> 403; case "UNAUTHENTICATED" -> 401; default -> throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); };
        return HttpAuditObservationDTO.builder().traceId(node.path("traceId").asText()).status(decision)
                .startedAt(httpAuditTime(node, "startedAt")).completedAt(httpAuditTime(node, "completedAt")).build();
    }

    private static ExpectedUsageReferencesVO serviceRpcAuditReferences(AnnotationConfigApplicationContext context, IsolatedRuntimeRegistryDTO registry,
            ServiceAccessRevocationDTO request, Runnable guard) {
        var realm = registry.getRealms().get(request.getTenantCode());
        var clients = registry.getClients().stream().filter(value -> request.getTenantCode().equals(value.getRealmCode()) && request.getClientPk().equals(value.getClientPk().toString())
                && request.getClientId().equals(value.getClientId()) && value.getPurpose() == OAuthClientPurposeEnum.SERVICE && value.getClientType() == OAuthClient.ClientType.CONFIDENTIAL
                && value.getStatus() == OAuthClient.Status.ACTIVE && value.getResourceUris().equals(List.of(request.getResourceUri().toASCIIString()))).toList();
        if (clients.size() != 1 || !realm.getIssuer().equals(request.getIssuer()) || !realm.getBusinessTenantId().toString().equals(request.getTenantId())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var storage = context.getBean(top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties.class); guard.run();
        var authorization = withHeldRealmStorageContext(realm, storage, () -> context.getBean("tenantAuthorizationService", org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService.class).findById(request.getAuthorizationId()));
        if (authorization == null || authorization.getAccessToken() == null || !request.getTokenSha256().equals(sha256Bytes(authorization.getAccessToken().getToken().getTokenValue().getBytes(StandardCharsets.UTF_8)))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        var verified = context.getBean("oracleTenantIssuerManager", top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent.class).authenticate(authorization.getAccessToken().getToken().getTokenValue());
        var jwt = verified.getJwt();
        if (!"SERVICE".equals(jwt.getClaimAsString("principal_type")) || !"TENANT".equals(jwt.getClaimAsString("scope_context")) || !request.getAuthorizationId().equals(verified.getAuthorizationId())
                || !request.getClientPk().equals(verified.getClientRegistrationId()) || !request.getClientId().equals(jwt.getClaimAsString("client_id"))
                || !request.getTenantId().equals(jwt.getClaimAsString("tenant_id")) || !List.of(request.getResourceUri().toASCIIString()).equals(jwt.getAudience())
                || !Set.copyOf(List.of(jwt.getClaimAsString("scope").split(" "))).equals(request.getScopes())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        return new ExpectedUsageReferencesVO(sha256Bytes(jwt.getSubject().getBytes(StandardCharsets.UTF_8)), sha256Bytes(request.getClientId().getBytes(StandardCharsets.UTF_8)),
                context.getBean("oracleUsageReferences", top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent.class).reference(request.getIssuer(), jwt.getId()));
    }

    @Test
    void rpcUsageAuditNeverAcceptsAnHttpStatusOrSubstitutesAnotherGrpcDecision() {
        var node = strictMapper().createObjectNode().put("traceId", "a".repeat(32)).put("status", "PERMISSION_DENIED")
                .put("startedAt", "2026-10-15T00:00:00Z").put("completedAt", "2026-10-15T00:00:01Z");
        assertThat(rpcAuditRequest(node, "PERMISSION_DENIED").getStatus()).isEqualTo(403);
        assertThatThrownBy(() -> rpcAuditRequest(node, "UNAUTHENTICATED")).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> rpcAuditRequest(node.deepCopy().put("status", 403), "PERMISSION_DENIED")).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> rpcAuditRequest(node.deepCopy().put("token", "untrusted"), "PERMISSION_DENIED")).hasMessage("XINGYUAN_INPUT_INVALID");
    }

    private static com.fasterxml.jackson.databind.node.ObjectNode observeOneHttpTrace(AnnotationConfigApplicationContext context, VerifiedDatabaseInputsBO inputs,
            IsolatedRuntimeRegistryDTO registry, HttpAuditObservationDTO request, String realmCode, String role, URI resource, Runnable guard, Instant operationDeadline,
            Set<Path> frames, ExpectedUsageReferencesVO expectedReferences) throws Exception {
        var clock = context.getBean("idpClock", Clock.class); var properties = context.getBean(top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties.class);
        var audits = context.getBean("oAuthAuditRepository", top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository.class);
        var redis = context.getBean("rbac3RuntimeRedissonClient", org.redisson.api.RedissonClient.class);
        var references = context.getBean("oracleUsageReferences", top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent.class);
        var expectedRealm = registry.getRealms().get(request.getStatus() == 401 ? "default" : realmCode);
        Instant deadline = clock.instant().plusSeconds(inputs.getManifest().getDeadlines().getHttpSeconds()); if (operationDeadline.isBefore(deadline)) { deadline = operationDeadline; }
        while (clock.instant().isBefore(deadline)) {
            var selected = new java.util.ArrayList<top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO>();
            for (String realm : List.of("default", "tenant-b")) {
                var trusted = registry.getRealms().get(realm);
                var rows = withHeldRealmStorageContext(trusted, properties, () -> boundedBffAudits(trusted, request.getStartedAt().minusMillis(1), request.getCompletedAt().plusMillis(1), audits, guard));
                for (var row : rows) {
                    if (!request.getTraceId().equals(row.getTraceId())) { continue; }
                    if (!trusted.getIssuer().equals(expectedRealm.getIssuer())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                    selected.add(row);
                }
            }
            boolean mcpRelay = role.equals("mcp") && request.getStatus() == 200;
            int expectedCount = request.getStatus() == 403 || mcpRelay ? 2 : 1;
            if (selected.size() < expectedCount) { Thread.sleep(100); continue; }
            if (mcpRelay) { requireMcpRelayAuditRows(selected, request, resource.toASCIIString()); }
            else { requireHttpTraceAuditRows(selected, request, resource.toASCIIString()); }
            if (expectedReferences != null) { requireExpectedUsageReferences(selected, expectedReferences); }
            var item = strictMapper().createObjectNode().put("role", role).put("realmCode", realmCode).put("traceId", request.getTraceId()).put("status", request.getStatus());
            var events = item.putArray("events"); boolean ready = true;
            for (var row : selected) {
                guard.run(); String stream = "xingyuan-it:" + inputs.getManifest().getRunId() + ":oauth:audit:v1";
                String receiptJson = redis.<String>getBucket(stream + ":receipt:" + row.getEventId(), org.redisson.client.codec.StringCodec.INSTANCE).get();
                if (receiptJson == null) { ready = false; break; }
                var event = confirmedStoredUsageEvent(expectedRealm, row, confirmedAuditStreamFields(redis, stream, row.getEventId(), guard), receiptJson, references);
                Path frame = Path.of(inputs.getManifest().getRuntimeRoot()).resolve("evidence/oauth-spool-" + role).resolve(event.eventId() + ".evt");
                if (frames.contains(frame)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                events.addObject().put("eventReference", sha256Bytes(event.eventId().getBytes(StandardCharsets.UTF_8))).put("fingerprint", references.fingerprint(event))
                        .put("receiptSha256", sha256Bytes(receiptJson.getBytes(StandardCharsets.UTF_8))).put("decision", event.result() + ':' + event.reasonCode());
            }
            if (ready) {
                for (var row : selected) {
                    frames.add(Path.of(inputs.getManifest().getRuntimeRoot()).resolve("evidence/oauth-spool-" + role).resolve(row.getEventId() + ".evt"));
                    // The signed protocol omits receiver identity; check both actual relay spools without inventing attribution.
                    if (mcpRelay) { frames.add(Path.of(inputs.getManifest().getRuntimeRoot()).resolve("evidence/oauth-spool-yuheng").resolve(row.getEventId() + ".evt")); }
                }
                return item;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
    }

    static void requireMcpRelayAuditRows(List<top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO> rows,
            HttpAuditObservationDTO request, String resource) {
        if (request.getStatus() != 200 || rows == null || rows.size() != 2 || Objects.equals(rows.get(0).getEventId(), rows.get(1).getEventId())) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        for (var row : rows) { requireHttpTraceAuditRows(List.of(row), request, resource); }
        var first = rows.getFirst();
        requireExpectedUsageReferences(rows, new ExpectedUsageReferencesVO(first.getSubjectRef(), first.getClientRef(), first.getTokenRef()));
    }

    private static void awaitOwnedAuditFrameCleanup(VerifiedDatabaseInputsBO inputs, Set<Path> frames, Runnable guard, Instant operationDeadline) throws Exception {
        Path root = Path.of(inputs.getManifest().getRuntimeRoot()); Instant deadline = Instant.now().plusSeconds(inputs.getManifest().getDeadlines().getHttpSeconds());
        if (operationDeadline.isBefore(deadline)) { deadline = operationDeadline; }
        while (Instant.now().isBefore(deadline)) {
            guard.run(); boolean absent = true;
            for (Path frame : frames) {
                if (!frame.startsWith(root.resolve("evidence")) || !frame.getParent().getFileName().toString().matches("oauth-spool-(shoubing|jianshen|yuheng|tianshu|mcp|native-web|native-service)")) {
                    throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                }
                for (Path directory = frame.getParent(); directory.startsWith(root); directory = directory.getParent()) {
                    if (Files.isSymbolicLink(directory) || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
                            || !Files.getOwner(root).equals(Files.getOwner(directory, LinkOption.NOFOLLOW_LINKS))
                            || !Files.getPosixFilePermissions(directory).equals(PosixFilePermissions.fromString("rwx------"))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                }
                for (String suffix : List.of("", ".sent", ".dead")) {
                    Path candidate = frame.resolveSibling(frame.getFileName() + suffix);
                    if (Files.isSymbolicLink(candidate) || suffix.equals(".dead") && Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                    if (Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) { absent = false; }
                }
            }
            if (absent) { return; }
            Thread.sleep(200);
        }
        throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
    }

    @Test
    void auditStreamLookupRejectsForeignQueuesConflictingRedeliveryAndUnboundedHistory() {
        var redis = Mockito.mock(org.redisson.api.RedissonClient.class);
        var stream = Mockito.mock(org.redisson.api.RStream.class); String key = "xingyuan-it:" + UUID.randomUUID() + ":oauth:audit:v1";
        var guards = new java.util.concurrent.atomic.AtomicInteger();
        assertThatThrownBy(() -> confirmedAuditStreamFields(redis, "egon:oauth:audit:v1", "owned-event", guards::incrementAndGet)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThat(guards.get()).isZero(); Mockito.verifyNoInteractions(redis);
        Mockito.when(redis.getStream(key, org.redisson.client.codec.StringCodec.INSTANCE)).thenReturn(stream);
        var original = Map.of("eventId", "owned-event", "result", "SUCCESS");
        var messages = new java.util.LinkedHashMap<org.redisson.api.StreamMessageId, Map<String, String>>();
        messages.put(new org.redisson.api.StreamMessageId(1, 0), original); messages.put(new org.redisson.api.StreamMessageId(2, 0), original);
        Mockito.when(stream.range(4097, org.redisson.api.StreamMessageId.MIN, org.redisson.api.StreamMessageId.MAX)).thenReturn(messages);
        assertThat(confirmedAuditStreamFields(redis, key, "owned-event", guards::incrementAndGet)).isEqualTo(original);
        assertThat(guards.get()).isEqualTo(2);
        messages.put(new org.redisson.api.StreamMessageId(2, 0), Map.of("eventId", "owned-event", "result", "DENIED"));
        assertThatThrownBy(() -> confirmedAuditStreamFields(redis, key, "owned-event", guards::incrementAndGet)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        messages.clear();
        for (int index = 0; index < 4097; index++) { messages.put(new org.redisson.api.StreamMessageId(index + 1, 0), original); }
        assertThatThrownBy(() -> confirmedAuditStreamFields(redis, key, "owned-event", guards::incrementAndGet)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        messages.clear();
        assertThatThrownBy(() -> confirmedAuditStreamFields(redis, key, "owned-event", guards::incrementAndGet)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

    @Test
    void signedUsageReceiptRetainsWireNanosWhileTheActualJdbcRoundsPgMicros(@org.junit.jupiter.api.io.TempDir Path temporary) throws Exception {
        Path root = temporary.toRealPath(); Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
        Path key = root.resolve("audit-hmac"); Files.write(key, new byte[32]); Files.setPosixFilePermissions(key, PosixFilePermissions.fromString("rw-------"));
        var realm = TrustedRealmBO.builder().issuer(URI.create("https://127.0.0.1:28443/tenants/1001")).realmKey("tenant:1001")
                .realmType(RealmTypeEnum.TENANT).storagePartitionId(1001L).businessTenantId(1001L).trustVersion(0L).build();
        var clock = Clock.fixed(Instant.parse("2026-10-15T00:00:02Z"), java.time.ZoneOffset.UTC);
        var wireAt = Instant.parse("2026-10-15T00:00:01.123456789Z");
        var jdbc = new org.postgresql.jdbc.TimestampUtils(false, () -> java.util.TimeZone.getTimeZone("UTC"));
        Instant pgAt = jdbc.toOffsetDateTime(jdbc.toString(wireAt.atOffset(java.time.ZoneOffset.UTC))).toInstant();
        assertThat(pgAt).isEqualTo(Instant.parse("2026-10-15T00:00:01.123457Z"));
        var row = top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO.builder().id(1L).version(0L).eventId("owned-audit-event")
                .eventType("TOKEN_USE_VERIFIED").auditKind("OAUTH").publicId("owned-audit-event").subjectRef("a".repeat(64)).clientRef("b".repeat(64))
                .tokenRef("c".repeat(64)).resourceRef("https://127.0.0.1:28140").result("SUCCESS").reasonCode("TOKEN_USE_VERIFIED").traceId("d".repeat(32)).occurredAt(pgAt).build();
        var event = new top.egon.cola.platform.tianquan.shoubing.core.audit.TokenUsageAuditEvent(row.getEventId(), 1, realm.getIssuer(), row.getResourceRef(),
                row.getSubjectRef(), row.getClientRef(), row.getTokenRef(), row.getResult(), row.getReasonCode(), wireAt, row.getTraceId());
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var references = new top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent(Mockito.mock(org.redisson.api.RedissonClient.class), strictMapper(),
                    new top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties("task:audit", root.resolve("oracle-spool"), 1048576, key),
                    new top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties(), clock, validators.getValidator());
            // Use the original writer's signature through the actual publish path, without Redis transport.
            var redis = Mockito.mock(org.redisson.api.RedissonClient.class); var stream = Mockito.mock(org.redisson.api.RStream.class);
            Mockito.when(redis.getStream("task:audit", org.redisson.client.codec.StringCodec.INSTANCE)).thenReturn(stream);
            Mockito.when(stream.add(Mockito.any(org.redisson.api.stream.StreamAddArgs.class))).thenReturn(new org.redisson.api.StreamMessageId(1, 0));
            var writer = new top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent(redis, strictMapper(),
                    new top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties("task:audit", root.resolve("writer-spool"), 1048576, key),
                    new top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties(), clock, validators.getValidator());
            writer.appendStrict(event);
            var captured = org.mockito.ArgumentCaptor.forClass(org.redisson.api.stream.StreamAddArgs.class);
            Mockito.verify(stream).add(captured.capture());
            var entries = ((org.redisson.api.stream.StreamAddParams<String, String>) captured.getValue()).getEntries();
            var receipt = references.encodeReceipt(references.createReceipt(event, "STORED", clock.instant()));
            assertThatCode(() -> confirmedStoredUsageEvent(realm, row, entries, receipt, references)).doesNotThrowAnyException();
            assertThat(confirmedStoredUsageEvent(realm, row, entries, receipt, references).occurredAt()).isEqualTo(wireAt);
            for (String changed : List.of("signature", "occurredAt", "result", "resource", "accessToken")) {
                var invalid = new java.util.LinkedHashMap<>(entries);
                invalid.put(changed, switch (changed) { case "signature" -> "0".repeat(64); case "occurredAt" -> wireAt.plusNanos(1).toString(); case "result" -> "DENIED"; case "resource" -> "https://foreign.invalid"; default -> "forbidden"; });
                assertThatThrownBy(() -> confirmedStoredUsageEvent(realm, row, invalid, receipt, references)).isInstanceOf(IllegalStateException.class);
            }
            var offsetTime = new java.util.LinkedHashMap<>(entries);
            offsetTime.put("occurredAt", wireAt.atOffset(java.time.ZoneOffset.ofHours(1)).toString());
            assertThatThrownBy(() -> confirmedStoredUsageEvent(realm, row, offsetTime, receipt, references)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        }
    }

    private static org.springframework.security.oauth2.server.authorization.OAuth2Authorization materializeHeldInStorageContext(
            TrustedRealmBO realm, HeldAuthorizationMatchDTO match,
            top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthAuthorizationMapper candidates,
            org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService authorizations,
            org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings settings,
            top.egon.cola.platform.tianquan.shoubing.starter.security.IdpJwtVerifier verifier,
            top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository audits,
            top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent references,
            org.redisson.api.RedissonClient redis, String auditStream, Runnable guard, Clock clock) throws IOException {
        if (realm.getRealmType() != RealmTypeEnum.TENANT || realm.getBusinessTenantId() == null) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var selected = boundedHeldCandidates(realm, match, candidates, guard);
        guard.run();
        var used = audits.findByEventId(realm, match.getEventId())
                .orElseThrow(() -> new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"));
        if (!"OAUTH".equals(used.getAuditKind()) || !"SUCCESS".equals(used.getResult())
                || !"TOKEN_USE_VERIFIED".equals(used.getReasonCode()) || !match.getCorrelationId().equals(used.getTraceId())
                || !match.getResourceUri().equals(used.getResourceRef()) || used.getTokenRef() == null
                || used.getOccurredAt().isBefore(match.getFromInclusive()) || !used.getOccurredAt().isBefore(match.getToExclusive())) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        guard.run();
        String receiptJson = redis.<String>getBucket(auditStream + ":receipt:" + match.getEventId(),
                org.redisson.client.codec.StringCodec.INSTANCE).get();
        if (receiptJson == null || receiptJson.length() > 16 * 1024) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        var originalEvent = confirmedStoredUsageEvent(realm, used, confirmedAuditStreamFields(redis, auditStream, match.getEventId(), guard), receiptJson, references);
        var proof = new top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO(realm.getIssuer(),
                URI.create(originalEvent.resource()), originalEvent.subjectRef(), originalEvent.clientRef(), originalEvent.tokenRef());
        var auditConverter = new top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverterImpl();
        var previous = org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder.getContext();
        org.springframework.security.oauth2.server.authorization.OAuth2Authorization held = null;
        int matched = 0;
        try {
            org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder.setContext(
                    new org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext() {
                        @Override
                        public String getIssuer() { return realm.getIssuer().toASCIIString(); }
                        @Override
                        public org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings getAuthorizationServerSettings() {
                            return settings;
                        }
                    });
            for (var row : selected) {
                guard.run();
                var authorization = authorizations.findById(Long.toString(row.getId()));
                if (authorization == null || authorization.getAccessToken() == null || !authorization.getAccessToken().isActive()) {
                    continue;
                }
                var token = authorization.getAccessToken().getToken();
                if (token.getExpiresAt() == null || !token.getExpiresAt().isAfter(clock.instant().plusSeconds(90))) {
                    continue;
                }
                var capture = new java.util.concurrent.atomic.AtomicReference<top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO>();
                var verification = verifier.verifyAny(token.getTokenValue(), capture::set);
                if (!(verification instanceof top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification.Valid<?> valid)
                        || !(valid.principal() instanceof top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal principal)
                        || capture.get() == null || !match.getPrincipalName().equals(principal.subject())
                        || !Objects.equals(realm.getBusinessTenantId().toString(), principal.tenantId())
                        || !Set.of(match.getResourceUri()).equals(principal.audience())
                        || !Long.toString(row.getId()).equals(capture.get().getAuthorizationId())
                        || !match.getClientPk().toString().equals(capture.get().getClientRegistrationId())) {
                    continue;
                }
                var actualProof = auditConverter.toProof(capture.get(), URI.create(match.getResourceUri()), references);
                if (!proof.equals(actualProof)) {
                    continue;
                }
                held = authorization;
                matched++;
            }
        } finally {
            if (previous == null) {
                org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder.resetContext();
            } else {
                org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder.setContext(previous);
            }
        }
        if (matched != 1) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        return held;
    }

    static void replayHeldFamily(java.net.http.HttpClient client, URI endpoint,
                                 org.springframework.security.oauth2.core.OAuth2AccessToken held,
                                 Runnable awaitConfirmedLogout, Runnable ownershipGuard, Clock clock,
                                 java.util.function.Predicate<String> successfulBusinessBody)
            throws IOException, InterruptedException {
        if (endpoint == null || endpoint.getHost() == null || !"https".equals(endpoint.getScheme()) || !Set.of("127.0.0.1", "localhost").contains(endpoint.getHost())
                || endpoint.getUserInfo() != null || endpoint.getQuery() != null || endpoint.getFragment() != null
                || !endpoint.equals(endpoint.normalize()) || held == null || held.getExpiresAt() == null) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        ownershipGuard.run();
        if (!held.getExpiresAt().isAfter(clock.instant().plusSeconds(30))) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        var request = java.net.http.HttpRequest.newBuilder(endpoint).timeout(java.time.Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + held.getTokenValue()).GET().build();
        var body = new java.io.ByteArrayOutputStream();
        var before = client.send(request, boundedBffBody(body));
        String content = body.toString(StandardCharsets.UTF_8);
        if (before.statusCode() != 200 || content.isBlank() || !successfulBusinessBody.test(content)) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        // The callback waits for the real browser's confirmed CSRF logout; the same request retains this AT only in memory.
        awaitConfirmedLogout.run();
        ownershipGuard.run();
        if (!held.getExpiresAt().isAfter(clock.instant())) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        var after = client.send(request, java.net.http.HttpResponse.BodyHandlers.discarding());
        if (after.statusCode() != 401 || !held.getExpiresAt().isAfter(clock.instant())) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
    }

    static java.net.http.HttpResponse.BodyHandler<Void> boundedBffBody(java.io.ByteArrayOutputStream target) {
        return java.net.http.HttpResponse.BodyHandlers.ofByteArrayConsumer(part -> part.ifPresent(bytes -> {
            if (bytes.length > 128 * 1024 - target.size()) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            target.writeBytes(bytes);
        }));
    }

    @Test
    void stockReactiveRefreshProviderKeepsTheSixtySecondWindowUsedByTheBrowserProbe() {
        var now = Instant.parse("2026-10-15T00:00:00Z");
        var registration = org.springframework.security.oauth2.client.registration.ClientRegistration.withRegistrationId("controlled-refresh-window")
                .clientId("controlled-client").clientSecret("controlled-not-live")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://127.0.0.1:28443/login/oauth2/code/controlled")
                .authorizationUri("https://127.0.0.1:28443/tenants/101/oauth2/authorize")
                .tokenUri("https://127.0.0.1:28443/tenants/101/oauth2/token").scope("openid", "profile").build();
        var provider = new org.springframework.security.oauth2.client.RefreshTokenReactiveOAuth2AuthorizedClientProvider();
        provider.setClock(Clock.fixed(now, java.time.ZoneOffset.UTC));
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        provider.setAccessTokenResponseClient(grant -> {
            calls.incrementAndGet();
            return reactor.core.publisher.Mono.just(org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse.withToken("controlled-next")
                    .tokenType(org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER).expiresIn(300)
                    .refreshToken("controlled-next-refresh").scopes(Set.of("openid", "profile")).build());
        });
        for (long seconds : List.of(61L, 59L)) {
            var access = new org.springframework.security.oauth2.core.OAuth2AccessToken(org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER,
                    "controlled-not-live", now.minusSeconds(60), now.plusSeconds(seconds), Set.of("openid", "profile"));
            var client = new org.springframework.security.oauth2.client.OAuth2AuthorizedClient(registration, "controlled", access,
                    new org.springframework.security.oauth2.core.OAuth2RefreshToken("controlled-refresh", now.minusSeconds(60)));
            var context = org.springframework.security.oauth2.client.OAuth2AuthorizationContext.withAuthorizedClient(client)
                    .principal(new org.springframework.security.authentication.TestingAuthenticationToken("controlled", "controlled")).build();
            var renewed = provider.authorize(context).block(java.time.Duration.ofSeconds(1));
            assertThat(renewed != null).isEqualTo(seconds == 59);
        }
        assertThat(calls.get()).isEqualTo(1);
    }

    private static java.net.http.HttpResponse.ResponseInfo bffResponseInfo() {
        return new java.net.http.HttpResponse.ResponseInfo() {
            @Override public int statusCode() { return 200; }
            @Override public java.net.http.HttpHeaders headers() { return java.net.http.HttpHeaders.of(Map.of(), (name, value) -> true); }
            @Override public java.net.http.HttpClient.Version version() { return java.net.http.HttpClient.Version.HTTP_1_1; }
        };
    }

    @Test
    void bffReplayResponseIsBoundedByTheOriginalJdkSubscriberBeforeAccumulation() {
        var body = new java.io.ByteArrayOutputStream();
        var subscriber = boundedBffBody(body).apply(bffResponseInfo());
        subscriber.onSubscribe(new java.util.concurrent.Flow.Subscription() {
            @Override public void request(long count) { }
            @Override public void cancel() { }
        });
        assertThatThrownBy(() -> subscriber.onNext(List.of(ByteBuffer.wrap(new byte[128 * 1024 + 1]))))
                .isInstanceOf(IllegalStateException.class).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        assertThat(body.size()).isZero();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void familyReplayKeepsTheSameUnexpiredAtAcrossConfirmedLogout() throws Exception {
        var client = Mockito.mock(java.net.http.HttpClient.class);
        var before = Mockito.mock(java.net.http.HttpResponse.class);
        var after = Mockito.mock(java.net.http.HttpResponse.class);
        Mockito.when(before.statusCode()).thenReturn(200);
        Mockito.when(after.statusCode()).thenReturn(401);
        var count = new AtomicInteger();
        var loggedOut = new java.util.concurrent.atomic.AtomicBoolean();
        var request = new java.util.concurrent.atomic.AtomicReference<java.net.http.HttpRequest>();
        Mockito.doAnswer(call -> {
            java.net.http.HttpRequest current = call.getArgument(0);
            assertThat(current.headers().firstValue("Authorization")).contains("Bearer controlled-held-at");
            if (count.getAndIncrement() == 0) {
                assertThat(loggedOut).isFalse();
                request.set(current);
                java.net.http.HttpResponse.BodyHandler<?> handler = call.getArgument(1);
                var subscriber = handler.apply(bffResponseInfo());
                subscriber.onSubscribe(new java.util.concurrent.Flow.Subscription() {
                    @Override public void request(long ignored) { }
                    @Override public void cancel() { }
                });
                subscriber.onNext(List.of(ByteBuffer.wrap("{\"success\":true}".getBytes(StandardCharsets.UTF_8))));
                subscriber.onComplete();
                subscriber.getBody().toCompletableFuture().join();
                return before;
            }
            assertThat(loggedOut).isTrue();
            assertThat(current).isSameAs(request.get());
            return after;
        }).when(client).send(ArgumentMatchers.any(java.net.http.HttpRequest.class), ArgumentMatchers.any(java.net.http.HttpResponse.BodyHandler.class));
        Instant now = Instant.parse("2026-10-14T00:00:00Z");
        var held = new org.springframework.security.oauth2.core.OAuth2AccessToken(
                org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER, "controlled-held-at", now.minusSeconds(5), now.plusSeconds(300));
        replayHeldFamily(client, URI.create("https://127.0.0.1:28443/resource/read"), held,
                () -> loggedOut.set(true), () -> { }, Clock.fixed(now, java.time.ZoneOffset.UTC), body -> body.contains("\"success\":true"));
        assertThat(count).hasValue(2);
        Mockito.clearInvocations(client);
        var expired = new org.springframework.security.oauth2.core.OAuth2AccessToken(
                org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER, "expired-controlled-at", now.minusSeconds(100), now.minusSeconds(1));
        assertThatThrownBy(() -> replayHeldFamily(client, URI.create("https://127.0.0.1:28443/resource/read"), expired,
                () -> { }, () -> { }, Clock.fixed(now, java.time.ZoneOffset.UTC), body -> true)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        Mockito.verifyNoInteractions(client);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void familyReplayDoesNotCountANaturallyExpiredResponseAsRevocation() throws Exception {
        var client = Mockito.mock(java.net.http.HttpClient.class);
        var before = Mockito.mock(java.net.http.HttpResponse.class);
        var after = Mockito.mock(java.net.http.HttpResponse.class);
        Mockito.when(before.statusCode()).thenReturn(200);
        Mockito.when(after.statusCode()).thenReturn(401);
        Instant now = Instant.parse("2026-10-15T00:00:00Z");
        var wall = new java.util.concurrent.atomic.AtomicReference<>(now);
        var clock = Mockito.mock(Clock.class);
        Mockito.when(clock.instant()).thenAnswer(call -> wall.get());
        var count = new AtomicInteger();
        Mockito.doAnswer(call -> {
            if (count.getAndIncrement() == 0) {
                java.net.http.HttpResponse.BodyHandler<?> handler = call.getArgument(1);
                var subscriber = handler.apply(bffResponseInfo());
                subscriber.onSubscribe(new java.util.concurrent.Flow.Subscription() {
                    @Override public void request(long ignored) { }
                    @Override public void cancel() { }
                });
                subscriber.onNext(List.of(ByteBuffer.wrap("{\"success\":true}".getBytes(StandardCharsets.UTF_8))));
                subscriber.onComplete();
                subscriber.getBody().toCompletableFuture().join();
                return before;
            }
            wall.set(now.plusSeconds(301));
            return after;
        }).when(client).send(ArgumentMatchers.any(java.net.http.HttpRequest.class), ArgumentMatchers.any(java.net.http.HttpResponse.BodyHandler.class));
        var held = new org.springframework.security.oauth2.core.OAuth2AccessToken(org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER,
                "controlled-expiring-at", now.minusSeconds(1), now.plusSeconds(300));
        assertThatThrownBy(() -> replayHeldFamily(client, URI.create("https://127.0.0.1:28443/resource/read"), held,
                () -> { }, () -> { }, clock, body -> true)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

    static <T> T withHeldRealmStorageContext(TrustedRealmBO realm,
            top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties properties,
            java.util.function.Supplier<T> operation) {
        realm.validateTrustedPartition();
        String tenantKey = properties.getTenantId().getMdcKey();
        String actorKey = properties.getAudit().getUserIdMdcKey();
        if (tenantKey == null || actorKey == null || tenantKey.equals(actorKey)) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
        Map<String, String> previous = org.slf4j.MDC.getCopyOfContextMap();
        org.slf4j.MDC.put(tenantKey, realm.getStoragePartitionId().toString());
        org.slf4j.MDC.put(actorKey, "service:tianquan-shoubing");
        try {
            return operation.get();
        } finally {
            if (previous == null) {
                org.slf4j.MDC.clear();
            } else {
                org.slf4j.MDC.setContextMap(previous);
            }
        }
    }

    @Test
    void heldRealmQueryRestoresAllMdcFieldsAfterFailure() {
        var realm = TrustedRealmBO.builder().issuer(URI.create("https://127.0.0.1:28443/tenants/1001"))
                .realmKey("tenant:1001").realmType(RealmTypeEnum.TENANT).storagePartitionId(1001L).businessTenantId(1001L).trustVersion(0L).build();
        var properties = new top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties();
        Map<String, String> original = org.slf4j.MDC.getCopyOfContextMap();
        try {
            org.slf4j.MDC.clear();
            org.slf4j.MDC.put("traceId", "caller-trace");
            org.slf4j.MDC.put(properties.getTenantId().getMdcKey(), "777");
            org.slf4j.MDC.put(properties.getAudit().getUserIdMdcKey(), "caller");
            Map<String, String> expected = org.slf4j.MDC.getCopyOfContextMap();
            assertThatThrownBy(() -> withHeldRealmStorageContext(realm, properties, () -> {
                assertThat(org.slf4j.MDC.get(properties.getTenantId().getMdcKey())).isEqualTo("1001");
                assertThat(org.slf4j.MDC.get(properties.getAudit().getUserIdMdcKey())).isEqualTo("service:tianquan-shoubing");
                throw new IllegalStateException("controlled-query-failure");
            })).hasMessage("controlled-query-failure");
            assertThat(org.slf4j.MDC.getCopyOfContextMap()).isEqualTo(expected);
        } finally {
            if (original == null) { org.slf4j.MDC.clear(); } else { org.slf4j.MDC.setContextMap(original); }
        }
    }

    @Test
    void absentConfirmedUseProofNeverLoadsOrExportsAnAuthorizationToken() {
        Instant now = Instant.parse("2026-10-14T00:00:00Z");
        var realm = TrustedRealmBO.builder().issuer(URI.create("https://127.0.0.1:28443/tenants/1001"))
                .realmKey("tenant:1001").realmType(RealmTypeEnum.TENANT).storagePartitionId(1001L).businessTenantId(1001L).trustVersion(0L).build();
        var match = HeldAuthorizationMatchDTO.builder().clientPk(51L).principalName("101")
                .resourceUri("https://resources.example/read").fromInclusive(now).toExclusive(now.plusSeconds(120))
                .eventId("used-event-1").correlationId("action-1").build();
        var mapper = Mockito.mock(top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthAuthorizationMapper.class);
        Mockito.when(mapper.selectByPrincipalName(1001L, "101", "ACTIVE", 0L, 500)).thenReturn(List.of());
        var authorizations = Mockito.mock(org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService.class);
        var audits = Mockito.mock(top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository.class);
        var verifier = Mockito.mock(top.egon.cola.platform.tianquan.shoubing.starter.security.IdpJwtVerifier.class);
        var references = Mockito.mock(top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent.class);
        var redis = Mockito.mock(org.redisson.api.RedissonClient.class);
        var properties = new top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties();
        Map<String, String> previous = org.slf4j.MDC.getCopyOfContextMap();
        assertThatThrownBy(() -> materializeHeldAccessToken(realm, match, mapper, authorizations,
                org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings.builder().multipleIssuersAllowed(true).build(),
                verifier, audits, references, redis, "xingyuan-it:run:oauth:audit:v1", () -> { }, Clock.fixed(now, java.time.ZoneOffset.UTC), properties))
                .hasMessage("XINGYUAN_VERIFICATION_FAILED");
        assertThat(org.slf4j.MDC.getCopyOfContextMap()).isEqualTo(previous);
        Mockito.verifyNoInteractions(authorizations, verifier, references, redis);
    }

    @Test
    void heldFamilySelectionIsBoundedAndUsesExactClientAudienceAndWindow() {
        var mapper = Mockito.mock(top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthAuthorizationMapper.class);
        Instant start = Instant.parse("2026-10-14T00:00:00Z");
        var match = HeldAuthorizationMatchDTO.builder().clientPk(51L).principalName("101")
                .resourceUri("https://resources.example/read").fromInclusive(start).toExclusive(start.plusSeconds(120))
                .eventId("used-event-1").correlationId("action-1").build();
        var realm = TrustedRealmBO.builder().issuer(URI.create("https://127.0.0.1:28443/tenants/1001"))
                .realmKey("tenant:1001").realmType(RealmTypeEnum.TENANT).storagePartitionId(1001L).businessTenantId(1001L).trustVersion(0L).build();
        var held = top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthAuthorizationPO.builder()
                .id(71L).tenantId(1001L).clientPk(51L).principalName("101").principalType("USER").status("ACTIVE")
                .grantType("authorization_code").audiences(List.of(match.getResourceUri())).createTime(start.plusSeconds(1)).build();
        var foreign = top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthAuthorizationPO.builder()
                .id(72L).tenantId(1001L).clientPk(52L).principalName("101").principalType("USER").status("ACTIVE")
                .grantType("authorization_code").audiences(List.of(match.getResourceUri())).createTime(start.plusSeconds(1)).build();
        Mockito.when(mapper.selectByPrincipalName(1001L, "101", "ACTIVE", 0L, 500)).thenReturn(List.of(held, foreign));
        assertThat(boundedHeldCandidates(realm, match, mapper, () -> { })).containsExactly(held);
        match.setToExclusive(start.plusSeconds(600));
        assertThatCode(() -> boundedHeldCandidates(realm, match, mapper, () -> { })).doesNotThrowAnyException();
        assertThat(boundedHeldCandidates(realm, match, mapper, () -> { })).containsExactly(held);
        match.setToExclusive(start.plusSeconds(901));
        assertThatThrownBy(() -> boundedHeldCandidates(realm, match, mapper, () -> { })).isInstanceOf(IllegalArgumentException.class);
        match.setToExclusive(start.plusSeconds(120));
        held.setAudiences(List.of(match.getResourceUri(), "https://resources.example/other"));
        assertThat(boundedHeldCandidates(realm, match, mapper, () -> { })).isEmpty();
        held.setAudiences(List.of(match.getResourceUri())).setCreateTime(start.plusSeconds(120));
        assertThat(boundedHeldCandidates(realm, match, mapper, () -> { })).isEmpty();
        Mockito.when(mapper.selectByPrincipalName(1001L, "101", "ACTIVE", 0L, 500))
                .thenReturn(java.util.Collections.nCopies(501, foreign));
        assertThatThrownBy(() -> boundedHeldCandidates(realm, match, mapper, () -> { })).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        Mockito.clearInvocations(mapper);
        assertThatThrownBy(() -> boundedHeldCandidates(realm, match, mapper,
                () -> { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); })).hasMessage("XINGYUAN_TARGET_UNSAFE");
        Mockito.verifyNoInteractions(mapper);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class IsolatedClientFactBO {
        @com.fasterxml.jackson.annotation.JsonIgnore
        @lombok.ToString.Exclude
        private OAuthClientBO client;
        private String realmCode;
        private String issuer;
        private String credentialFile;
        private String registrationId;
        private String alias;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class IsolatedClientFactDTO {
        private Long clientPk;
        private String clientId;
        private String appId;
        private OAuthClient.ClientType clientType;
        private OAuthClientPurposeEnum purpose;
        private OAuthClient.Status status;
        private Long credentialVersion;
        private long version;
        private List<String> redirectUris;
        private List<String> resourceUris;
        private List<String> allowedScopes;
        private Integer accessTokenTtlSeconds;
        private Integer refreshTokenTtlSeconds;
        private String realmCode;
        private String issuer;
        private String credentialFile;
        private String registrationId;
        private String alias;
    }

    @org.mapstruct.Mapper(componentModel = "spring", implementationName = "IsolatedClientFactConverterImpl",
            unmappedTargetPolicy = org.mapstruct.ReportingPolicy.ERROR)
    public interface IsolatedClientFactConverter extends
            top.egon.cola.component.common.core.converter.BaseForwardConverter<IsolatedClientFactBO, IsolatedClientFactDTO> {
        @org.mapstruct.Mapping(target = "clientPk", source = "client.id")
        @org.mapstruct.Mapping(target = "clientId", source = "client.clientId")
        @org.mapstruct.Mapping(target = "appId", source = "client.appId")
        @org.mapstruct.Mapping(target = "clientType", source = "client.clientType")
        @org.mapstruct.Mapping(target = "purpose", source = "client.purpose")
        @org.mapstruct.Mapping(target = "status", source = "client.status")
        @org.mapstruct.Mapping(target = "credentialVersion", source = "client.credentialVersion")
        @org.mapstruct.Mapping(target = "version", source = "client.version")
        @org.mapstruct.Mapping(target = "redirectUris", source = "client.redirectUris")
        @org.mapstruct.Mapping(target = "resourceUris", source = "client.resourceUris")
        @org.mapstruct.Mapping(target = "allowedScopes", source = "client.allowedScopes")
        @org.mapstruct.Mapping(target = "accessTokenTtlSeconds", source = "client.accessTtlSeconds")
        @org.mapstruct.Mapping(target = "refreshTokenTtlSeconds", source = "client.refreshTtlSeconds")
        @Override
        IsolatedClientFactDTO toTarget(IsolatedClientFactBO source);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class IsolatedResourceAliasDTO {
        private String defaultRegistrationId;
        private Map<String, String> issuerRegistrationIds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class IsolatedRuntimeRegistryDTO {
        private int schemaVersion;
        private String runId;
        private String manifestSha256;
        private String publicOrigin;
        private String platformStoragePartitionId;
        private Map<String, String> tenantIds;
        private Map<String, TrustedRealmBO> realms;
        @lombok.ToString.Exclude
        private Map<String, String> subjects;
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}") @lombok.ToString.Exclude
        private String provisioningSubject;
        private List<IsolatedClientFactDTO> clients;
        private List<top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ResourceServerVO> resources;
        private List<CliResourcePreparationDTO> cliResources;
        private Map<String, IsolatedResourceAliasDTO> aliases;
    }

    static Map<String, Object> artifactRuntimeDefaults(ArtifactReferenceDTO artifact, Path repository, String profile) throws IOException {
        if (artifact == null || artifact.getPath() == null || artifact.getSha256() == null
                || !artifact.getSha256().matches("[0-9a-f]{64}") || !Set.of("local", "dev", "operations", "default").contains(profile)) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
        Path path = Path.of(artifact.getPath());
        if (!path.isAbsolute() || !path.equals(path.normalize()) || !path.startsWith(repository.toAbsolutePath().normalize())
                || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        for (Path current = path; current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        }
        var before = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
                byte[] buffer = new byte[64 * 1024];
                for (int count; (count = input.read(buffer)) >= 0;) { if (count > 0) { digest.update(buffer, 0, count); } }
            }
            if (!artifact.getSha256().equals(HexFormat.of().formatHex(digest.digest()))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
        } catch (NoSuchAlgorithmException unavailable) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"); }
        Map<String, Object> values = new java.util.LinkedHashMap<>();
        try (var jar = new java.util.jar.JarFile(path.toFile(), false)) {
            Set<String> loaded = new HashSet<>();
            mergeArtifactConfiguration(jar, "application.yml", profile, values, new HashSet<>(), loaded, false);
            mergeArtifactConfiguration(jar, "application-" + profile + ".yml", profile, values, new HashSet<>(), loaded, true);
        }
        var after = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!Objects.equals(before.fileKey(), after.fileKey()) || before.size() != after.size()
                || !before.lastModifiedTime().equals(after.lastModifiedTime())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        if (values.size() > 10000) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
        return java.util.Collections.unmodifiableMap(values);
    }

    private static void mergeArtifactConfiguration(java.util.jar.JarFile jar, String name, String profile,
            Map<String, Object> output, Set<String> visiting, Set<String> loaded, boolean optional) throws IOException {
        if (!name.matches("[A-Za-z0-9_./-]+\\.(yml|yaml|properties)") || name.startsWith("/")
                || java.util.Arrays.asList(name.split("/")).contains("..") || visiting.size() > 8 || loaded.size() > 64) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
        if (loaded.contains(name)) { return; }
        if (!visiting.add(name)) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
        byte[] contents = artifactClasspathResource(jar, name);
        if (contents == null) {
            visiting.remove(name);
            if (optional) { return; }
            throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE");
        }
        var resource = new org.springframework.core.io.ByteArrayResource(contents, name);
        List<org.springframework.core.env.PropertySource<?>> documents = name.endsWith(".properties")
                ? new org.springframework.boot.env.PropertiesPropertySourceLoader().load(name, resource)
                : new org.springframework.boot.env.YamlPropertySourceLoader().load(name, resource);
        for (var document : documents) {
            Object active = document.getProperty("spring.config.activate.on-profile");
            if (active != null && !org.springframework.core.env.Profiles.of(active.toString()).matches(profile::equals)) { continue; }
            if (!(document instanceof org.springframework.core.env.EnumerablePropertySource<?> enumerable)) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
            }
            List<String> imports = new java.util.ArrayList<>();
            for (String key : enumerable.getPropertyNames()) {
                Object value = enumerable.getProperty(key);
                if (key.equals("spring.config.import") || key.startsWith("spring.config.import[")) {
                    if (value != null) { imports.addAll(java.util.Arrays.asList(value.toString().split(","))); }
                } else if (!key.equals("spring.config.activate.on-profile") && value != null) { output.put(key, value); }
            }
            for (String specification : imports) {
                String location = specification.trim();
                boolean optionalImport = location.startsWith("optional:");
                if (optionalImport) { location = location.substring("optional:".length()); }
                if (location.startsWith("classpath:")) {
                    mergeArtifactConfiguration(jar, location.substring("classpath:".length()), profile, output, visiting, loaded, optionalImport);
                } else if (!optionalImport || !(location.startsWith("file:") || location.startsWith("configtree:") || location.startsWith("tianshu:"))) {
                    throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                }
            }
        }
        visiting.remove(name);
        loaded.add(name);
    }

    private static byte[] artifactClasspathResource(java.util.jar.JarFile jar, String name) throws IOException {
        var entry = jar.getJarEntry("BOOT-INF/classes/" + name);
        if (entry != null) {
            try (var input = jar.getInputStream(entry)) { return boundedArtifactResource(input); }
        }
        byte[] selected = null;
        var entries = jar.entries();
        int libraries = 0;
        while (entries.hasMoreElements()) {
            var library = entries.nextElement();
            if (!library.getName().startsWith("BOOT-INF/lib/") || !library.getName().endsWith(".jar")) { continue; }
            if (++libraries > 1000) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
            try (var nested = new java.util.jar.JarInputStream(jar.getInputStream(library), false)) {
                for (java.util.jar.JarEntry nestedEntry; (nestedEntry = nested.getNextJarEntry()) != null;) {
                    if (nestedEntry.getName().equals(name)) {
                        if (selected != null) { throw new IllegalArgumentException("XINGYUAN_STATE_CONFLICT"); }
                        selected = boundedArtifactResource(nested);
                    }
                }
            }
        }
        return selected;
    }

    private static byte[] boundedArtifactResource(java.io.InputStream input) throws IOException {
        byte[] value = input.readNBytes(2 * 1024 * 1024 + 1);
        if (value.length > 2 * 1024 * 1024) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
        return value;
    }

    @Test
    void packagedRuntimeDefaultsApplyTheirProfileAndOnlyClasspathImports() throws Exception {
        Path artifact = temporaryRepository.resolve("controlled-runtime.jar");
        Map<String, String> resources = Map.of(
                "BOOT-INF/classes/application.yml", "spring:\n  config:\n    import:\n      - classpath:reviewed.yml\n      - optional:file:/must-not-read/application.yml\n      - optional:configtree:/must-not-read/secrets/\n      - optional:tianshu:application.yml\ncontrolled:\n  value: base\n",
                "BOOT-INF/classes/reviewed.yml", "controlled:\n  imported: true\n",
                "BOOT-INF/classes/application-local.yml", "controlled:\n  value: local\n");
        try (var zip = new java.util.zip.ZipOutputStream(Files.newOutputStream(artifact))) {
            for (var entry : resources.entrySet()) {
                zip.putNextEntry(new java.util.zip.ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        var reference = ArtifactReferenceDTO.builder().path(artifact.toString()).buildId("controlled-runtime")
                .sha256(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact)))).build();
        var defaults = artifactRuntimeDefaults(reference, temporaryRepository, "local");
        assertThat(defaults.get("controlled.value")).isEqualTo("local");
        assertThat(defaults.get("controlled.imported")).isEqualTo(true);
        assertThat(defaults.keySet()).noneMatch(key -> key.startsWith("spring.config.import"));
    }

    @Test
    void packagedDefaultsVerifyTheArtifactAndRejectCyclicOrAmbiguousImports() throws Exception {
        var nested = new java.io.ByteArrayOutputStream();
        try (var jar = new java.util.jar.JarOutputStream(nested)) {
            jar.putNextEntry(new java.util.jar.JarEntry("nested-defaults.yml"));
            jar.write("controlled:\n  nested: exact\n".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        var valid = controlledRuntimeArtifact(Map.of("BOOT-INF/classes/application.yml",
                "spring:\n  config:\n    import: classpath:nested-defaults.yml\n".getBytes(StandardCharsets.UTF_8),
                "BOOT-INF/lib/defaults.jar", nested.toByteArray()));
        assertThat(artifactRuntimeDefaults(valid, temporaryRepository, "local")).containsEntry("controlled.nested", "exact");
        valid.setSha256("0".repeat(64));
        assertThatThrownBy(() -> artifactRuntimeDefaults(valid, temporaryRepository, "local")).hasMessage("XINGYUAN_TARGET_UNSAFE");
        var ambiguous = controlledRuntimeArtifact(Map.of("BOOT-INF/classes/application.yml",
                "spring:\n  config:\n    import: classpath:nested-defaults.yml\n".getBytes(StandardCharsets.UTF_8),
                "BOOT-INF/lib/defaults-a.jar", nested.toByteArray(), "BOOT-INF/lib/defaults-b.jar", nested.toByteArray()));
        assertThatThrownBy(() -> artifactRuntimeDefaults(ambiguous, temporaryRepository, "local")).hasMessage("XINGYUAN_STATE_CONFLICT");
        var cyclic = controlledRuntimeArtifact(Map.of("BOOT-INF/classes/application.yml",
                "spring:\n  config:\n    import: classpath:application.yml\n".getBytes(StandardCharsets.UTF_8)));
        assertThatThrownBy(() -> artifactRuntimeDefaults(cyclic, temporaryRepository, "local")).hasMessage("XINGYUAN_INPUT_INVALID");
        var requiredTree = controlledRuntimeArtifact(Map.of("BOOT-INF/classes/application.yml",
                "spring:\n  config:\n    import: configtree:/must-not-read/secrets/\n".getBytes(StandardCharsets.UTF_8)));
        assertThatThrownBy(() -> artifactRuntimeDefaults(requiredTree, temporaryRepository, "local")).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    @Test
    void onlyThePrivateRegistryCanUseTheLargerPublicationBound() throws Exception {
        Path directory = temporaryRepository.resolve("bounded-publication");
        Files.createDirectory(directory, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        byte[] bytes = "x".repeat(129 * 1024).getBytes(StandardCharsets.US_ASCII);
        Path result = directory.resolve("public-result.json");
        assertThatThrownBy(() -> publishProtectedBytes(result, directory, bytes)).hasMessage("XINGYUAN_INPUT_INVALID");
        assertThat(Files.exists(result)).isFalse();
        Path registry = directory.resolve("private-registry.json");
        publishProtectedBytes(registry, directory, bytes, 2 * 1024 * 1024);
        assertThat(readProtectedBytes(registry, directory, 2 * 1024 * 1024)).isEqualTo(bytes);
    }

    private ArtifactReferenceDTO controlledRuntimeArtifact(Map<String, byte[]> resources) throws Exception {
        Path artifact = temporaryRepository.resolve("controlled-" + UUID.randomUUID() + ".jar");
        try (var zip = new java.util.zip.ZipOutputStream(Files.newOutputStream(artifact))) {
            for (var entry : resources.entrySet()) {
                zip.putNextEntry(new java.util.zip.ZipEntry(entry.getKey())); zip.write(entry.getValue()); zip.closeEntry();
            }
        }
        return ArtifactReferenceDTO.builder().path(artifact.toString()).buildId("controlled-runtime")
                .sha256(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact)))).build();
    }

    @Test
    void theExistingSpringResolverPreservesAnEscapedOpaqueCredential() {
        var sources = new org.springframework.core.env.MutablePropertySources();
        sources.addFirst(new org.springframework.core.env.MapPropertySource("controlled", Map.of(
                "credential.value", " leading \\${not-a-variable}")));
        var resolver = new org.springframework.core.env.PropertySourcesPropertyResolver(sources);
        String value = org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> resolver.getProperty("credential.value"));
        assertThat(value).isEqualTo(" leading ${not-a-variable}");
    }

    public record RuntimeProfileInputVO(@NotNull Set<String> referencedInputs, boolean unresolved) {
        public RuntimeProfileInputVO { referencedInputs = java.util.Collections.unmodifiableSet(new java.util.TreeSet<>(referencedInputs)); }
    }

    static Map<String, RuntimeProfileInputVO> runtimeProfileInputs(Map<String, Object> defaults) {
        if (defaults == null || defaults.size() > 10000 || defaults.values().stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
        Map<String, RuntimeProfileInputVO> requirements = new java.util.TreeMap<>();
        for (var entry : new java.util.TreeMap<>(defaults).entrySet()) {
            Set<String> referenced = new java.util.TreeSet<>();
            var sources = new org.springframework.core.env.MutablePropertySources();
            sources.addFirst(new org.springframework.core.env.MapPropertySource("artifactDefaults", defaults));
            sources.addLast(new org.springframework.core.env.PropertySource<Set<String>>("unprovidedInputs", referenced) {
                @Override public Object getProperty(String name) {
                    // Spring also probes name:default; only canonical names may enter the value-free report.
                    if (name.matches("[A-Za-z_][A-Za-z0-9_.-]{0,255}")) { getSource().add(name); }
                    return null;
                }
            });
            boolean unresolved = false;
            var resolver = new org.springframework.core.env.PropertySourcesPropertyResolver(sources);
            try { resolver.resolvePlaceholders(entry.getValue().toString()); }
            catch (IllegalArgumentException cyclic) { unresolved = true; }
            try { resolver.resolveRequiredPlaceholders(entry.getValue().toString()); }
            catch (IllegalArgumentException missing) { unresolved = true; }
            if (!referenced.isEmpty() || unresolved) { requirements.put(entry.getKey(), new RuntimeProfileInputVO(referenced, unresolved)); }
        }
        return java.util.Collections.unmodifiableMap(requirements);
    }

    @Test
    void profileInputInventoryUsesSpringDefaultsAndNeverInheritsOrPublishesValues() throws Exception {
        String name = "controlled.inventory.external";
        String previous = System.getProperty(name);
        System.setProperty(name, "private-controlled-value");
        try {
            var report = runtimeProfileInputs(Map.of("required", "${" + name + "}", "optional", "${OPTIONAL_INPUT:default-private-value}",
                    "internal", "${base}", "base", "artifact-private-value", "literal", "private-literal-value"));
            assertThat(report).containsKey("required");
            assertThat(report.get("required").unresolved()).isTrue();
            assertThat(report.get("required").referencedInputs()).containsExactly(name);
            assertThat(report.get("optional").unresolved()).isFalse();
            assertThat(report.get("optional").referencedInputs()).containsExactly("OPTIONAL_INPUT");
            assertThat(report).doesNotContainKeys("internal", "base", "literal");
            assertThat(strictMapper().writeValueAsString(report)).doesNotContain("private-controlled-value", "default-private-value", "artifact-private-value", "private-literal-value");
        } finally { if (previous == null) { System.clearProperty(name); } else { System.setProperty(name, previous); } }
        assertThat(runtimeProfileInputs(Map.of("cycle", "${other}", "other", "${cycle}")).get("cycle").unresolved()).isTrue();
        assertThat(runtimeProfileInputs(Map.of("pair", "${LEFT_INPUT}-${RIGHT_INPUT}")).get("pair").referencedInputs()).containsExactly("LEFT_INPUT", "RIGHT_INPUT");
    }

    static byte[] encodeRuntimeInventory(Map<String, ?> values) throws IOException {
        return strictMapper().enable(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).writeValueAsBytes(values);
    }

    @Test
    void runtimeInventoryEncodingIsStableAcrossMapInsertionOrders() throws Exception {
        Map<String, Object> first = new java.util.LinkedHashMap<>();
        first.put("z", Map.of("b", 2, "a", 1)); first.put("a", 1);
        Map<String, Object> nested = new java.util.LinkedHashMap<>(); nested.put("a", 1); nested.put("b", 2);
        Map<String, Object> second = new java.util.LinkedHashMap<>(); second.put("a", 1); second.put("z", nested);
        assertThat(encodeRuntimeInventory(first)).isEqualTo(encodeRuntimeInventory(second));
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "xingyuan.isolated.profile-inventory.enabled", matches = "true")
    void inventoryRuntimeProfileInputsOffline() throws IOException {
        Path repository = Path.of(requiredOperationProperty("xingyuan.isolated.repository-root")).toRealPath();
        Path input = Path.of(requiredOperationProperty("xingyuan.isolated.profile-inventory-file"));
        if (!input.isAbsolute() || !input.equals(input.normalize()) || input.getParent() == null || input.getParent().getParent() == null
                || !input.getParent().getFileName().toString().equals("policies") || !input.getFileName().toString().equals("runtime-profile-artifacts.json")) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        Path root = input.getParent().getParent();
        if (root.getParent() == null || !root.getParent().equals(repository.resolve("target")) || !root.getFileName().toString().matches("xingyuan-integration-[0-9a-f-]{36}")) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        String run = root.getFileName().toString().substring("xingyuan-integration-".length());
        if (!UUID.fromString(run).toString().equals(run)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        byte[] inputBytes = readProtectedBytes(input, root, 128 * 1024);
        var type = strictMapper().getTypeFactory().constructMapType(java.util.TreeMap.class, String.class, ArtifactReferenceDTO.class);
        Map<String, ArtifactReferenceDTO> artifacts = strictMapper().readValue(inputBytes, type);
        if (artifacts == null || !artifacts.keySet().equals(RUNTIME_PROFILES.keySet())) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
        Path evidence = root.resolve("evidence");
        validatePrivateSecretDestination(evidence.resolve(".profile-inputs-directory-check"), root);
        Path lockPath = evidence.resolve("profile-inputs.lock");
        try (var channel = FileChannel.open(lockPath, Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))) {
            readProtectedBytes(lockPath, root, 1024);
            try (var lock = channel.tryLock()) {
                if (lock == null) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
                Path output = evidence.resolve("profile-inputs");
                if (!Files.exists(output, LinkOption.NOFOLLOW_LINKS)) { Files.createDirectory(output, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))); }
                if (Files.exists(output.resolve("summary.json"), LinkOption.NOFOLLOW_LINKS)) { readProtectedBytes(output.resolve("summary.json"), root, 128 * 1024); }
                else { validatePrivateSecretDestination(output.resolve("summary.json"), root); }
                Map<String, byte[]> reports = new java.util.TreeMap<>();
                Map<String, Object> summary = new java.util.TreeMap<>();
                Map<String, String> edgeHostTemplate = null; Map<String, String> edgeRoutesTemplate = null;
                for (var entry : artifacts.entrySet()) {
                    String role = entry.getKey(); String profile = RUNTIME_PROFILES.get(role);
                    var defaults = artifactRuntimeDefaults(entry.getValue(), repository, profile);
                    try (var jar = new java.util.jar.JarFile(Path.of(entry.getValue().getPath()).toFile(), false)) {
                        if (jar.getJarEntry("BOOT-INF/classes/application.yml") == null) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                    }
                    if (role.equals("edge")) { edgeHostTemplate = edgeHostResourceTemplate(defaults); edgeRoutesTemplate = edgeRouteTemplate(defaults); }
                    var requirements = runtimeProfileInputs(defaults);
                    long unresolved = requirements.values().stream().filter(RuntimeProfileInputVO::unresolved).count();
                    var report = Map.of("schemaVersion", 1, "runId", run, "scope", "ARTIFACT_CONFIGURATION_INPUTS_ONLY", "role", role, "profile", profile,
                            "artifactSha256", entry.getValue().getSha256(), "propertyCount", defaults.size(), "requirements", requirements);
                    byte[] bytes = encodeRuntimeInventory(report);
                    if (bytes.length > 128 * 1024) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
                    reports.put(role, bytes);
                    summary.put(role, Map.of("artifactSha256", entry.getValue().getSha256(), "profile", profile, "propertyCount", defaults.size(),
                            "referencedPropertyCount", requirements.size(), "unresolvedPropertyCount", unresolved, "reportSha256", sha256Bytes(bytes)));
                }
                if (!MessageDigest.isEqual(inputBytes, readProtectedBytes(input, root, 128 * 1024))) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
                reports.put("summary", encodeRuntimeInventory(Map.of("schemaVersion", 1, "runId", run,
                        "scope", "ARTIFACT_CONFIGURATION_INPUTS_ONLY", "inputSha256", sha256Bytes(inputBytes), "roles", summary)));
                for (var entry : reports.entrySet()) {
                    Path destination = output.resolve(entry.getKey().equals("summary") ? "summary.json" : entry.getKey() + "-requirements.json");
                    if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)
                            && !MessageDigest.isEqual(entry.getValue(), readProtectedBytes(destination, root, 128 * 1024))) {
                        throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
                    }
                }
                if (edgeHostTemplate == null || edgeRoutesTemplate == null) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
                Map<String, byte[]> fragments = new java.util.TreeMap<>();
                fragments.put("edge-bff-user-fragment.properties", encodeRuntimeProperties(edgeBffUserClientTemplate()));
                fragments.put("edge-host-resource-fragment.properties", encodeRuntimeProperties(edgeHostTemplate));
                fragments.put("runtime-trust-fragment.properties", encodeRuntimeProperties(runtimeTrustTemplate()));
                fragments.put("edge-routes-fragment.properties", encodeRuntimeProperties(edgeRoutesTemplate));
                for (String role : RUNTIME_PROFILES.keySet()) { fragments.put(role + "-audit-fragment.properties", encodeRuntimeProperties(runtimeAuditTemplate(role))); }
                for (String role : List.of("biz", "mcp")) { fragments.put(role + "-listener-fragment.properties", encodeRuntimeProperties(gatewayListenerTemplate(role))); }
                for (var entry : fragments.entrySet()) {
                    Path destination = root.resolve("policies/" + entry.getKey());
                    if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)
                            && !MessageDigest.isEqual(entry.getValue(), readProtectedBytes(destination, root, 128 * 1024))) {
                        throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
                    }
                }
                for (var entry : reports.entrySet()) {
                    Path destination = output.resolve(entry.getKey().equals("summary") ? "summary.json" : entry.getKey() + "-requirements.json");
                    if (!Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) { publishProtectedBytes(destination, root, entry.getValue()); }
                }
                for (var entry : fragments.entrySet()) {
                    Path destination = root.resolve("policies/" + entry.getKey());
                    if (!Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) { publishProtectedBytes(destination, root, entry.getValue()); }
                }
            }
        }
    }

    static void validateProvisioningIdentity(Map<String, String> subjects, String provisioningSubject) {
        if (subjects == null || !subjects.keySet().equals(Set.of("admin", "viewer")) || provisioningSubject == null
                || !provisioningSubject.matches("[A-Za-z0-9._:-]{1,128}") || subjects.values().stream().anyMatch(value -> value == null || value.isBlank())
                || new HashSet<>(subjects.values()).size() != 2 || subjects.containsValue(provisioningSubject)) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
    }

    @Test
    void provisioningIdentityStaysDistinctFromInteractiveUsers() throws Exception {
        Map<String, String> users = Map.of("admin", "controlled-admin", "viewer", "controlled-viewer");
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> validateProvisioningIdentity(users, "controlled-provisioner"));
        for (String invalid : List.of("controlled-admin", "controlled-viewer", "", " bad ", "bad\nsubject")) {
            assertThatThrownBy(() -> validateProvisioningIdentity(users, invalid)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
        assertThatThrownBy(() -> validateProvisioningIdentity(users, null)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> validateProvisioningIdentity(Map.of("admin", "same", "viewer", "same"), "different")).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> validateProvisioningIdentity(Map.of("admin", "a", "viewer", "v", "provisioner", "p"), "p")).hasMessage("XINGYUAN_TARGET_UNSAFE");
        var facts = IsolatedOAuthProvisioningComponent.IdentityFactsBO.builder().subjects(users).provisioningSubject("controlled-provisioner").build();
        assertThat(facts.toString()).doesNotContain("controlled-provisioner");
        assertThat(strictMapper().writeValueAsString(facts)).doesNotContain("controlled-provisioner", "controlled-admin", "controlled-viewer");
        var registry = IsolatedRuntimeRegistryDTO.builder().subjects(users).provisioningSubject("controlled-provisioner").build();
        assertThat(registry.toString()).doesNotContain("controlled-provisioner");
        assertThat(strictMapper().writeValueAsString(registry)).contains("\"provisioningSubject\":\"controlled-provisioner\"");
    }

    static Map<String, String> runtimeRegistryFacts(VerifiedDatabaseInputsBO inputs, IsolatedRuntimeRegistryDTO registry) {
        var manifest = inputs.getManifest();
        if (registry == null || registry.getSchemaVersion() != 1 || !manifest.getRunId().equals(registry.getRunId())
                || !inputs.getManifestSha256().equals(registry.getManifestSha256())
                || !manifest.getPublicOrigin().equals(registry.getPublicOrigin())
                || !manifest.getPlatformStoragePartitionId().equals(registry.getPlatformStoragePartitionId())
                || registry.getRealms() == null || !registry.getRealms().keySet().equals(Set.of("platform", "default", "tenant-b"))
                || registry.getTenantIds() == null || !registry.getTenantIds().keySet().equals(Set.of("default", "tenant-b"))
                || registry.getSubjects() == null || registry.getClients() == null || registry.getResources() == null) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        validateProvisioningIdentity(registry.getSubjects(), registry.getProvisioningSubject());
        Map<String, String> facts = new java.util.LinkedHashMap<>();
        facts.put("xingyuan.it.run-id", manifest.getRunId());
        facts.put("xingyuan.it.namespace", "xingyuan-it-" + manifest.getRunId());
        facts.put("xingyuan.it.public-origin", manifest.getPublicOrigin());
        facts.put("xingyuan.it.runtime-root", manifest.getRuntimeRoot());
        for (var process : manifest.getProcesses()) {
            if (!"JAVA".equals(process.getKind()) || !RUNTIME_PROFILES.containsKey(process.getId())) { continue; }
            if (process.getPort() < 1 || process.getPort() > 65535
                    || facts.putIfAbsent("xingyuan.it.process." + process.getId() + ".https-origin", "https://127.0.0.1:" + process.getPort()) != null) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            facts.put("xingyuan.it.process." + process.getId() + ".port", Integer.toString(process.getPort()));
            if (RUNTIME_RPC_PORTS.containsKey(process.getId()) && process.getAdditionalPorts() != null && !process.getAdditionalPorts().isEmpty()) {
                int rpcPort = runtimeRpcPort(process);
                facts.put("xingyuan.it.process." + process.getId() + ".rpc-port", Integer.toString(rpcPort));
                facts.put("xingyuan.it.process." + process.getId() + ".rpc-target", "dns:///127.0.0.1:" + rpcPort);
            }
            if (Set.of("biz", "mcp").contains(process.getId()) && process.getAdditionalPorts() != null && !process.getAdditionalPorts().isEmpty()) {
                facts.put("xingyuan.it.process." + process.getId() + ".management-port", Integer.toString(runtimeManagementPort(process)));
            }
        }
        Set<String> tenantIds = new HashSet<>();
        for (String code : manifest.getTenantCodes()) {
            String tenant = registry.getTenantIds().get(code);
            try {
                if (tenant == null || !tenant.matches("[1-9][0-9]{0,18}") || Long.parseLong(tenant) <= 0L
                        || tenant.equals(manifest.getPlatformStoragePartitionId()) || !tenantIds.add(tenant)) {
                    throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                }
            } catch (NumberFormatException invalid) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            facts.put("xingyuan.it.tenant." + code + ".id", tenant);
        }
        for (String code : List.of("admin", "viewer")) {
            String subject = registry.getSubjects().get(code);
            if (subject == null || subject.isBlank() || subject.length() > 200 || subject.chars().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            facts.put("xingyuan.it.subject." + code, subject);
        }
        for (String code : List.of("platform", "default", "tenant-b")) {
            TrustedRealmBO realm = registry.getRealms().get(code);
            if (realm == null) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            realm.validateTrustedPartition();
            String issuer = manifest.getPublicOrigin() + (code.equals("platform") ? "/platform" : "/tenants/" + registry.getTenantIds().get(code));
            String partition = code.equals("platform") ? manifest.getPlatformStoragePartitionId() : registry.getTenantIds().get(code);
            if (!issuer.equals(realm.getIssuer().toASCIIString()) || !partition.equals(realm.getStoragePartitionId().toString())
                    || (code.equals("platform") ? realm.getRealmType() != RealmTypeEnum.PLATFORM : realm.getRealmType() != RealmTypeEnum.TENANT)) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            String prefix = "xingyuan.it.realm." + code + ".";
            facts.put(prefix + "issuer", issuer); facts.put(prefix + "realm-key", realm.getRealmKey());
            facts.put(prefix + "realm-type", realm.getRealmType().name()); facts.put(prefix + "storage-partition-id", partition);
            facts.put(prefix + "tenant-id", realm.getBusinessTenantId() == null ? "" : realm.getBusinessTenantId().toString());
            facts.put(prefix + "trust-version", realm.getTrustVersion().toString());
            facts.put(prefix + "token-uri", issuer + "/oauth2/token"); facts.put(prefix + "authorization-uri", issuer + "/oauth2/authorize");
            facts.put(prefix + "jwk-set-uri", issuer + "/.well-known/jwks.json"); facts.put(prefix + "user-info-uri", issuer + "/userinfo");
        }
        Set<String> resources = new HashSet<>();
        Set<String> uris = new HashSet<>();
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            for (var resource : registry.getResources()) {
                if (resource == null || !validators.getValidator().validate(resource).isEmpty() || !"ACTIVE".equals(resource.getStatus())
                        || !resources.add(resource.getResourceServerId()) || !uris.add(resource.getResourceUri())) {
                    throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                }
                String prefix = "xingyuan.it.resource." + resource.getResourceServerId() + ".";
                facts.put(prefix + "uri", resource.getResourceUri()); facts.put(prefix + "biz-code", resource.getBizCode());
                facts.put(prefix + "app-code", resource.getAppCode()); facts.put(prefix + "environment", resource.getEnvironment());
                facts.put(prefix + "rbac-application-code", resource.getRbacApplicationCode());
                facts.put(prefix + "entry-permission-code", resource.getEntryPermissionCode());
                facts.put(prefix + "management-client-id", resource.getManagementClientId());
            }
        }
        var environments = registry.getResources().stream().map(value -> value.getEnvironment()).collect(java.util.stream.Collectors.toSet());
        if (!"local-two-tenant-users-v1".equals(manifest.getUserRecipe()) || !environments.isEmpty() && !environments.equals(Set.of("local"))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        facts.put("xingyuan.it.environment", "local");
        Set<String> registrations = new HashSet<>();
        for (var client : registry.getClients()) {
            if (client == null || client.getStatus() != OAuthClient.Status.ACTIVE || client.getClientPk() == null || client.getClientPk() <= 0
                    || client.getClientId() == null || !client.getClientId().matches("[A-Za-z0-9._-]{1,128}")
                    || client.getRealmCode() == null || !registry.getRealms().containsKey(client.getRealmCode())
                    || !registry.getRealms().get(client.getRealmCode()).getIssuer().toASCIIString().equals(client.getIssuer())
                    || client.getPurpose() == null || client.getClientType() == null || client.getAllowedScopes() == null
                    || client.getAllowedScopes().isEmpty() || client.getAllowedScopes().size() > 64
                    || client.getAllowedScopes().stream().anyMatch(scope -> scope == null || !scope.matches("[A-Za-z0-9._:-]{1,128}"))
                    || client.getResourceUris() == null || client.getResourceUris().isEmpty() || !uris.containsAll(client.getResourceUris())) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            if (client.getRegistrationId() == null) { continue; }
            if (!client.getRegistrationId().matches("[A-Za-z0-9._-]{1,128}") || !registrations.add(client.getRegistrationId())) {
                throw new IllegalArgumentException("XINGYUAN_STATE_CONFLICT");
            }
            String prefix = "xingyuan.it.client." + client.getRegistrationId() + ".";
            facts.put(prefix + "client-id", client.getClientId()); facts.put(prefix + "client-pk", client.getClientPk().toString());
            facts.put(prefix + "issuer", client.getIssuer()); facts.put(prefix + "realm-code", client.getRealmCode());
            facts.put(prefix + "client-type", client.getClientType().name()); facts.put(prefix + "purpose", client.getPurpose().name());
            facts.put(prefix + "scopes", String.join(",", client.getAllowedScopes()));
            if (client.getAppId() != null) { facts.put(prefix + "app-id", client.getAppId()); }
            if (client.getCredentialVersion() != null) { facts.put(prefix + "credential-version", client.getCredentialVersion().toString()); }
            if (client.getCredentialFile() != null) { facts.put(prefix + "credential-file", client.getCredentialFile()); }
            if (client.getResourceUris().size() == 1) {
                String uri = client.getResourceUris().getFirst(); facts.put(prefix + "resource-uri", uri);
                var resource = registry.getResources().stream().filter(value -> uri.equals(value.getResourceUri())).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"));
                facts.put(prefix + "resource-server-id", resource.getResourceServerId());
            }
            if (client.getRedirectUris() != null && client.getRedirectUris().size() == 1) { facts.put(prefix + "redirect-uri", client.getRedirectUris().getFirst()); }
        }
        if (registry.getAliases() != null) {
            for (var alias : registry.getAliases().entrySet()) {
                String prefix = "xingyuan.it.alias." + alias.getKey() + ".";
                if (!registrations.contains(alias.getValue().getDefaultRegistrationId())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                facts.put(prefix + "default-registration-id", alias.getValue().getDefaultRegistrationId());
                for (String tenant : manifest.getTenantCodes()) {
                    String registration = alias.getValue().getIssuerRegistrationIds().get(registry.getRealms().get(tenant).getIssuer().toASCIIString());
                    if (!registrations.contains(registration)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                    facts.put(prefix + tenant + ".registration-id", registration);
                }
            }
        }
        return java.util.Collections.unmodifiableMap(facts);
    }

    @Test
    void runtimeCredentialFactsUseOnlyProtectedReferencesAndOwnedDatabaseReceipts() throws Exception {
        var manifest = intent();
        Path root = Path.of(manifest.getRuntimeRoot());
        for (String directory : List.of("", "evidence", "secrets")) {
            Files.createDirectories(root.resolve(directory));
            Files.setPosixFilePermissions(root.resolve(directory), PosixFilePermissions.fromString("rwx------"));
        }
        Map<String, String> secretFiles = new java.util.LinkedHashMap<>();
        for (String role : List.of("tls-keystore", "tls-keystore-password", "tls-ca-cert", "tls-server-cert", "tls-server-key")) {
            secretFiles.put(role, root.resolve("secrets/" + role).toString());
        }
        manifest.setSecretFiles(secretFiles);
        String hash = "a".repeat(64);
        for (var database : DATABASE_NAMES.entrySet()) {
            Path file = root.resolve("evidence/db-" + database.getKey() + ".json");
            Files.write(file, strictMapper().writeValueAsBytes(IsolatedDatabaseReceiptDTO.builder().runId(manifest.getRunId())
                    .manifestSha256(hash).role(database.getKey()).databaseName(database.getValue()).oid(42L)
                    .databaseOwner("controlled-owner").ownerComment("xingyuan-it:" + manifest.getRunId() + ":" + hash).build()));
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        }
        String credential = " leading ${opaque-placeholder}\nsecond line ";
        for (var file : List.of(Path.of(manifest.getPostgres().getCredentialFile()), Path.of(manifest.getRedis().getCredentialFile()),
                Path.of(secretFiles.get("tls-keystore-password")))) {
            Files.writeString(file, credential);
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        }
        var inputs = VerifiedDatabaseInputsBO.builder().manifest(manifest).manifestSha256(hash).build();
        var facts = runtimeCredentialFacts(inputs, runtimeRegistryFixture(manifest, hash));
        assertThat(facts.get("xingyuan.it.postgres.password")).isEqualTo(credential);
        assertThat(facts.get("xingyuan.it.redis.database")).isEqualTo("15");
        assertThat(facts.get("xingyuan.it.postgres.web.url")).isEqualTo("jdbc:postgresql://127.0.0.1:5432/" + DATABASE_NAMES.get("web"));
        Files.setPosixFilePermissions(Path.of(manifest.getPostgres().getCredentialFile()), PosixFilePermissions.fromString("rw-r--r--"));
        assertThatThrownBy(() -> runtimeCredentialFacts(inputs, runtimeRegistryFixture(manifest, hash))).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    private IsolatedRuntimeRegistryDTO runtimeRegistryFixture(IsolatedIntegrationManifestDTO manifest, String hash) {
        Map<String, TrustedRealmBO> realms = new java.util.LinkedHashMap<>();
        realms.put("platform", TrustedRealmBO.builder().issuer(URI.create(manifest.getPublicOrigin() + "/platform")).realmKey("platform")
                .realmType(RealmTypeEnum.PLATFORM).storagePartitionId(Long.valueOf(manifest.getPlatformStoragePartitionId())).trustVersion(2L).build());
        for (var entry : Map.of("default", 1001L, "tenant-b", 1002L).entrySet()) {
            realms.put(entry.getKey(), TrustedRealmBO.builder().issuer(URI.create(manifest.getPublicOrigin() + "/tenants/" + entry.getValue()))
                    .realmKey("tenant:" + entry.getValue()).realmType(RealmTypeEnum.TENANT).storagePartitionId(entry.getValue())
                    .businessTenantId(entry.getValue()).trustVersion(7L).build());
        }
        return IsolatedRuntimeRegistryDTO.builder().schemaVersion(1).runId(manifest.getRunId()).manifestSha256(hash)
                .publicOrigin(manifest.getPublicOrigin()).platformStoragePartitionId(manifest.getPlatformStoragePartitionId())
                .tenantIds(Map.of("default", "1001", "tenant-b", "1002")).subjects(Map.of("admin", "controlled-admin", "viewer", "controlled-viewer")).provisioningSubject("controlled-provisioner")
                .realms(realms).clients(List.of()).resources(List.of()).aliases(Map.of()).cliResources(List.of()).build();
    }

    @Test
    void runtimeFactsUseTheCommittedRealmVersionsAndActualTenantIds() {
        var manifest = intent();
        Map<String, TrustedRealmBO> realms = Map.of(
                "platform", TrustedRealmBO.builder().issuer(URI.create(manifest.getPublicOrigin() + "/platform")).realmKey("platform")
                        .realmType(RealmTypeEnum.PLATFORM).storagePartitionId(Long.valueOf(manifest.getPlatformStoragePartitionId())).trustVersion(2L).build(),
                "default", TrustedRealmBO.builder().issuer(URI.create(manifest.getPublicOrigin() + "/tenants/1001")).realmKey("tenant:1001")
                        .realmType(RealmTypeEnum.TENANT).storagePartitionId(1001L).businessTenantId(1001L).trustVersion(7L).build(),
                "tenant-b", TrustedRealmBO.builder().issuer(URI.create(manifest.getPublicOrigin() + "/tenants/1002")).realmKey("tenant:1002")
                        .realmType(RealmTypeEnum.TENANT).storagePartitionId(1002L).businessTenantId(1002L).trustVersion(9L).build());
        var registry = IsolatedRuntimeRegistryDTO.builder().schemaVersion(1).runId(manifest.getRunId()).manifestSha256("a".repeat(64))
                .publicOrigin(manifest.getPublicOrigin()).platformStoragePartitionId(manifest.getPlatformStoragePartitionId())
                .tenantIds(Map.of("default", "1001", "tenant-b", "1002")).subjects(Map.of("admin", "controlled-admin", "viewer", "controlled-viewer")).provisioningSubject("controlled-provisioner")
                .realms(realms).clients(List.of()).resources(List.of()).aliases(Map.of()).cliResources(List.of()).build();
        var inputs = VerifiedDatabaseInputsBO.builder().manifest(manifest).manifestSha256("a".repeat(64)).build();
        var facts = runtimeRegistryFacts(inputs, registry);
        assertThat(facts.get("xingyuan.it.realm.default.trust-version")).isEqualTo("7");
        assertThat(facts.get("xingyuan.it.realm.tenant-b.storage-partition-id")).isEqualTo("1002");
        assertThat(facts.get("xingyuan.it.namespace")).isEqualTo("xingyuan-it-" + manifest.getRunId());
        realms.get("default").setIssuer(URI.create(manifest.getPublicOrigin() + "/tenants/9999"));
        assertThatThrownBy(() -> runtimeRegistryFacts(inputs, registry)).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void generateRuntimeProfiles() throws IOException {
        Path repository = Path.of(requiredOperationProperty("xingyuan.isolated.repository-root"));
        var inputs = readVerifiedDatabaseInputs(Path.of(requiredOperationProperty("xingyuan.isolated.manifest-file")),
                Path.of(requiredOperationProperty("xingyuan.isolated.receipt-file")), repository, "idp");
        Path root = Path.of(inputs.getManifest().getRuntimeRoot());
        Path registryFile = root.resolve("secrets/runtime-registry.json");
        byte[] registryBytes = readProtectedBytes(registryFile, root.resolve("secrets"), 2 * 1024 * 1024);
        var registry = strictMapper().readValue(registryBytes, IsolatedRuntimeRegistryDTO.class);
        var identity = strictMapper().readValue(readProtectedBytes(root.resolve("evidence/identity.json"), root.resolve("evidence"), 128 * 1024), IsolatedIntegrationResult.class);
        if (!"CLI-002".equals(identity.getOperationId()) || !"COMMITTED".equals(identity.getStatus())
                || !inputs.getManifest().getRunId().equals(identity.getRunId()) || !inputs.getManifestSha256().equals(identity.getManifestSha256())
                || identity.getError() != null || registry.getClients() == null || registry.getClients().isEmpty()
                || registry.getResources() == null || registry.getResources().isEmpty()) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
        Map<String, String> facts = runtimeCredentialFacts(inputs, registry);
        Map<String, String> profiles = RUNTIME_PROFILES;
        Map<String, String> exclusions = runtimeProfileExclusions(inputs);
        Map<String, byte[]> encoded = new java.util.LinkedHashMap<>();
        Map<String, FileReferenceDTO> references = new java.util.LinkedHashMap<>();
        Set<String> visited = new HashSet<>();
        try {
            for (var process : inputs.getManifest().getProcesses()) {
                if (!"JAVA".equals(process.getKind())) { continue; }
                String role = process.getId();
                if (!profiles.containsKey(role) || !role.equals(process.getArtifactRole()) || !inputs.getManifest().getArtifacts().containsKey(role) || !visited.add(role)) {
                    throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                }
                var defaults = new java.util.LinkedHashMap<>(artifactRuntimeDefaults(inputs.getManifest().getArtifacts().get(role), repository, profiles.get(role)));
                requirePackagedRuntimeRpcModels(inputs.getManifest().getArtifacts().get(role));
                if (exclusions.containsKey(role)) { continue; }
                Path templatePath = root.resolve("policies/" + role + "-profile.properties");
                var templates = inputs.getManifest().getPolicyFiles().stream().filter(ref -> templatePath.toString().equals(ref.getPath())).toList();
                if (templates.size() != 1) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
                byte[] templateBytes = readProtectedBytes(templatePath, root, 2 * 1024 * 1024);
                if (!sha256Bytes(templateBytes).equals(templates.getFirst().getSha256())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                Map<String, String> reviewed = readRuntimeTemplate(templateBytes);
                if (role.equals("jianshen") || role.equals("tianshu")) {
                    Path routing = root.resolve("profiles/" + (role.equals("jianshen") ? "rbac" : "tianshu") + "-sharding.yml");
                    var policies = inputs.getManifest().getPolicyFiles().stream().filter(ref -> routing.toString().equals(ref.getPath())).toList();
                    if (policies.size() != 1 || !sha256Bytes(readProtectedBytes(routing, root, 2 * 1024 * 1024)).equals(policies.getFirst().getSha256())) {
                        throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                    }
                }
                applyRuntimeProfileFragments(role, defaults, reviewed);
                clearRuntimeIdentityDefaults(defaults);
                if (role.equals("edge")) { defaults.keySet().removeIf(key -> key.startsWith("spring.cloud.gateway.server.webflux.routes")); }
                applyRuntimeTransport(role, process, inputs, defaults, reviewed, profiles.get(role));
                Map<String, String> rendered = renderRuntimeProperties(defaults, reviewed, facts);
                validateRuntimeMachineId(rendered);
                validateRuntimeTrust(inputs, registry, rendered);
                validateRuntimeAudit(role, inputs, registry, rendered);
                validateRuntimeRpcTransport(role, process, inputs, rendered);
                if (role.equals("edge")) { validatePackagedEdgeProperties(inputs.getManifest().getArtifacts().get(role), repository, inputs, registry, rendered); }
                else {
                    validateRuntimeTransport(role, process, inputs, registry, rendered);
                    if (Set.of("biz", "mcp").contains(role)) { validatePackagedGatewayListeners(role, inputs.getManifest().getArtifacts().get(role), repository, inputs, rendered); }
                }
                byte[] bytes = encodeRuntimeProperties(rendered);
                encoded.put(role, bytes);
                Path destination = root.resolve("profiles/" + role + ".properties");
                references.put(role, FileReferenceDTO.builder().path(destination.toString()).sha256(sha256Bytes(bytes)).build());
            }
            if (!visited.equals(profiles.keySet())) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
            validateRuntimeProfileCoverage(references.keySet(), exclusions);
            publishGeneratedRuntimeProfiles(inputs, sha256Bytes(registryBytes), encoded, references, exclusions);
        } finally { encoded.values().forEach(bytes -> java.util.Arrays.fill(bytes, (byte) 0)); }
    }

    private static void publishGeneratedRuntimeProfiles(VerifiedDatabaseInputsBO inputs, String registryHash,
            Map<String, byte[]> encoded, Map<String, FileReferenceDTO> references, Map<String, String> exclusions) throws IOException {
        Path root = Path.of(inputs.getManifest().getRuntimeRoot());
        Path lockPath = root.resolve("evidence/profiles.lock");
        try (var channel = FileChannel.open(lockPath, Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))) {
            readProtectedBytes(lockPath, root.resolve("evidence"), 1024);
            try (var lock = channel.tryLock()) {
                if (lock == null) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
            validateRuntimeProfileCoverage(references.keySet(), exclusions);
            if (!encoded.keySet().equals(references.keySet()) || !exclusions.equals(runtimeProfileExclusions(inputs))) {
                throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
            }
            Path receipt = root.resolve("evidence/profiles-ready.json");
            var ready = RuntimeProfilesReceiptDTO.builder().schemaVersion(1).runId(inputs.getManifest().getRunId())
                    .manifestSha256(inputs.getManifestSha256()).registrySha256(registryHash).profiles(Map.copyOf(references)).runtimeExclusions(Map.copyOf(exclusions)).build();
            if (Files.exists(receipt, LinkOption.NOFOLLOW_LINKS)) {
                var old = strictMapper().readValue(readProtectedBytes(receipt, root.resolve("evidence"), 128 * 1024), RuntimeProfilesReceiptDTO.class);
                validateRuntimeProfileCoverage(old.getProfiles() == null ? Set.of() : old.getProfiles().keySet(), old.getRuntimeExclusions());
                if (!ready.equals(old)) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
            }
            for (var entry : encoded.entrySet()) {
                if (!exclusions.equals(runtimeProfileExclusions(inputs))) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
                Path file = Path.of(references.get(entry.getKey()).getPath());
                if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                    if (!MessageDigest.isEqual(entry.getValue(), readProtectedBytes(file, root.resolve("profiles"), 2 * 1024 * 1024))) {
                        throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
                    }
                } else { publishProtectedBytes(file, root.resolve("profiles"), entry.getValue(), 2 * 1024 * 1024); }
            }
            if (!exclusions.equals(runtimeProfileExclusions(inputs))) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
            if (!Files.exists(receipt, LinkOption.NOFOLLOW_LINKS)) { publishProtectedBytes(receipt, root.resolve("evidence"), strictMapper().writeValueAsBytes(ready)); }
            }
        }
    }

    static Map<String, String> runtimeProfileExclusions(VerifiedDatabaseInputsBO inputs) throws IOException {
        Path root = Path.of(inputs.getManifest().getRuntimeRoot());
        Path policy = root.resolve("policies/agent-runtime-deferred.json");
        var references = inputs.getManifest().getPolicyFiles().stream().filter(value -> policy.toString().equals(value.getPath())).toList();
        if (references.isEmpty() && !Files.exists(policy, LinkOption.NOFOLLOW_LINKS)) { return Map.of(); }
        if (references.size() != 1 || references.getFirst().getSha256() == null || !references.getFirst().getSha256().matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        byte[] bytes = readProtectedBytes(policy, root.resolve("policies"), 128 * 1024);
        String hash = sha256Bytes(bytes);
        if (!hash.equals(references.getFirst().getSha256())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var document = strictMapper().readTree(bytes); Set<String> fields = new HashSet<>();
        if (document == null || !document.isObject()) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        document.fieldNames().forEachRemaining(fields::add);
        if (!fields.equals(Set.of("schemaVersion", "runId", "role", "status", "reasonCode", "acceptedOn"))
                || !document.path("schemaVersion").isIntegralNumber() || !document.path("schemaVersion").canConvertToInt() || document.path("schemaVersion").intValue() != 1) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        for (var expected : Map.of("runId", inputs.getManifest().getRunId(), "role", "native-agent", "status", "UNVERIFIED",
                "reasonCode", "AGENT_RUNTIME_CONFIGURATION_NOT_PROVIDED", "acceptedOn", "2026-10-15").entrySet()) {
            var value = document.path(expected.getKey());
            if (!value.isTextual() || !expected.getValue().equals(value.textValue())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        }
        return Map.of("native-agent", hash);
    }

    static void validateRuntimeProfileCoverage(Set<String> profiles, Map<String, String> exclusions) {
        if (profiles == null || exclusions == null || exclusions.size() > 1
                || exclusions.entrySet().stream().anyMatch(value -> !"native-agent".equals(value.getKey())
                || value.getValue() == null || !value.getValue().matches("[0-9a-f]{64}"))) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
        Set<String> expected = new HashSet<>(RUNTIME_PROFILES.keySet()); expected.removeAll(exclusions.keySet());
        if (!expected.equals(profiles)) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
    }

    @Test
    void runtimeAgentExclusionRequiresTheExactAcceptedPolicyAndFrozenHash() throws Exception {
        var manifest = intent(); Path root = Path.of(manifest.getRuntimeRoot()); Path policies = root.resolve("policies");
        Files.createDirectories(policies); Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
        Files.setPosixFilePermissions(policies, PosixFilePermissions.fromString("rwx------"));
        manifest.setPolicyFiles(List.of());
        var inputs = VerifiedDatabaseInputsBO.builder().manifest(manifest).manifestSha256("a".repeat(64)).build();
        assertThat(runtimeProfileExclusions(inputs)).isEmpty();
        Path policy = policies.resolve("agent-runtime-deferred.json");
        var accepted = strictMapper().createObjectNode().put("schemaVersion", 1).put("runId", manifest.getRunId()).put("role", "native-agent")
                .put("status", "UNVERIFIED").put("reasonCode", "AGENT_RUNTIME_CONFIGURATION_NOT_PROVIDED").put("acceptedOn", "2026-10-15");
        byte[] bytes = strictMapper().writeValueAsBytes(accepted); Files.write(policy, bytes); Files.setPosixFilePermissions(policy, PosixFilePermissions.fromString("rw-------"));
        assertThatThrownBy(() -> runtimeProfileExclusions(inputs)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        var reference = FileReferenceDTO.builder().path(policy.toString()).sha256(sha256Bytes(bytes)).build(); manifest.setPolicyFiles(List.of(reference));
        assertThat(runtimeProfileExclusions(inputs)).containsExactlyEntriesOf(Map.of("native-agent", reference.getSha256()));
        reference.setSha256("b".repeat(64)); assertThatThrownBy(() -> runtimeProfileExclusions(inputs)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        reference.setSha256(sha256Bytes(bytes)); manifest.setPolicyFiles(List.of(reference, reference));
        assertThatThrownBy(() -> runtimeProfileExclusions(inputs)).hasMessage("XINGYUAN_TARGET_UNSAFE"); manifest.setPolicyFiles(List.of(reference));
        for (var change : Map.of("runId", UUID.randomUUID().toString(), "role", "native-web", "status", "PASSED", "reasonCode", "OTHER",
                "acceptedOn", "2026-10-16", "unknown", "unsupported").entrySet()) {
            var invalid = accepted.deepCopy().put(change.getKey(), change.getValue()); byte[] changed = strictMapper().writeValueAsBytes(invalid);
            Files.write(policy, changed); reference.setSha256(sha256Bytes(changed));
            assertThatThrownBy(() -> runtimeProfileExclusions(inputs)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
        var invalidVersion = accepted.deepCopy().put("schemaVersion", "1"); byte[] changed = strictMapper().writeValueAsBytes(invalidVersion);
        Files.write(policy, changed); reference.setSha256(sha256Bytes(changed));
        assertThatThrownBy(() -> runtimeProfileExclusions(inputs)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        Files.write(policy, bytes); reference.setSha256(sha256Bytes(bytes)); Files.setPosixFilePermissions(policy, PosixFilePermissions.fromString("rw-r--r--"));
        assertThatThrownBy(() -> runtimeProfileExclusions(inputs)).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    @Test
    void runtimeProfileCoverageAllowsOnlyTheExplicitAgentGapAndDefaultsToTwelve() throws Exception {
        var all = RUNTIME_PROFILES.keySet(); Set<String> eleven = new HashSet<>(all); eleven.remove("native-agent");
        Map<String, String> excluded = Map.of("native-agent", "a".repeat(64));
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> validateRuntimeProfileCoverage(all, Map.of()));
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> validateRuntimeProfileCoverage(eleven, excluded));
        assertThatThrownBy(() -> validateRuntimeProfileCoverage(eleven, Map.of())).hasMessage("XINGYUAN_STATE_CONFLICT");
        assertThatThrownBy(() -> validateRuntimeProfileCoverage(all, excluded)).hasMessage("XINGYUAN_STATE_CONFLICT");
        assertThatThrownBy(() -> validateRuntimeProfileCoverage(eleven, Map.of("native-web", "a".repeat(64)))).hasMessage("XINGYUAN_STATE_CONFLICT");
        assertThatThrownBy(() -> validateRuntimeProfileCoverage(eleven, Map.of("native-agent", "bad"))).hasMessage("XINGYUAN_STATE_CONFLICT");
        Set<String> missing = new HashSet<>(eleven); missing.remove("native-web");
        assertThatThrownBy(() -> validateRuntimeProfileCoverage(missing, excluded)).hasMessage("XINGYUAN_STATE_CONFLICT");
        var old = strictMapper().readValue("{\"schemaVersion\":1}", RuntimeProfilesReceiptDTO.class);
        assertThat(old.getRuntimeExclusions()).isEmpty();
        assertThat(RuntimeProfilesReceiptDTO.builder().build().getRuntimeExclusions()).isEmpty();
    }

    static void validateRuntimeMachineId(Map<String, String> values) {
        if ("false".equals(values.getOrDefault("egon.cola.component.id.enabled", "true"))) { return; }
        String value = values.get("egon.cola.component.id.machine-id");
        if (value == null || !value.matches("[0-9]{1,4}")) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
        int machineId = Integer.parseInt(value);
        if (machineId > 1023 || machineId == 711) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
    }

    @Test
    void runtimeProfilesKeepTheExistingOwnerFixtureMachineIdReservedBeforePublication() {
        assertThatCode(() -> validateRuntimeMachineId(Map.of("egon.cola.component.id.machine-id", "12"))).doesNotThrowAnyException();
        for (String value : List.of("711", "0711", "1024", "-1", "${EGON_ID_MACHINE_ID}")) {
            assertThatThrownBy(() -> validateRuntimeMachineId(Map.of("egon.cola.component.id.machine-id", value))).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> validateRuntimeMachineId(Map.of())).hasMessage("XINGYUAN_INPUT_UNAVAILABLE");
        assertThatCode(() -> validateRuntimeMachineId(Map.of("egon.cola.component.id.enabled", "false"))).doesNotThrowAnyException();
    }

    private static void applyRuntimeTransport(String role, ProcessTargetDTO process, VerifiedDatabaseInputsBO inputs,
            Map<String, Object> defaults, Map<String, String> values, String profile) {
        values.put("xingyuan.isolated.run-id", "${xingyuan.it.run-id}");
        values.put("spring.profiles.active", profile);
        values.put("spring.docker.compose.enabled", "false");
        values.put("management.endpoint.health.probes.enabled", "true");
        values.put("server.address", "127.0.0.1"); values.put("server.port", Integer.toString(runtimeManagementPort(process)));
        values.put("server.ssl.enabled", "true"); values.put("server.ssl.key-store-type", "PKCS12");
        values.put("server.ssl.key-store", "${xingyuan.it.tls.key-store-uri}");
        values.put("server.ssl.key-store-password", "${xingyuan.it.tls.key-store-password}");
        values.put("server.forward-headers-strategy", "framework");
        if (!role.equals("edge")) {
            values.put("egon.cola.platform.tianquan.shoubing.enabled", "true");
            values.put("egon.cola.platform.tianquan.jianshen.enabled", "true");
            values.put("egon.cola.platform.tianquan.jianshen.user-scope-policy.enabled", "true");
        }
        values.put("spring.data.redis.host", "${xingyuan.it.redis.host}"); values.put("spring.data.redis.port", "${xingyuan.it.redis.port}");
        values.put("spring.data.redis.url", "${xingyuan.it.redis.url}"); values.put("spring.data.redis.database", "${xingyuan.it.redis.database}");
        values.put("spring.data.redis.password", "${xingyuan.it.redis.password}"); values.put("spring.data.redis.timeout", "500ms");
        for (String prefix : List.of("egon.cola.platform.tianquan.jianshen.runtime", "egon.cola.platform.tianquan.jianshen.yuheng.runtime",
                "egon.cola.platform.tianquan.shoubing.yuheng.runtime")) {
            values.put(prefix + ".redis-address", "${xingyuan.it.redis.endpoint}");
            values.put(prefix + ".database", "${xingyuan.it.redis.database}");
            values.put(prefix + ".password-file", "${xingyuan.it.redis.credential-file}"); values.put(prefix + ".timeout", "500ms");
        }
        for (String key : List.of("egon.cola.component.tianshu.admin.redis.database", "egon.cola.component.tianshu.registry.redis.database",
                "egon.cola.component.yuheng.engine.traffic.redis.database", "egon.cola.component.yuheng.mcp.engine.traffic.redis.database")) {
            values.put(key, "${xingyuan.it.redis.database}");
        }
        if (role.equals("jianshen")) {
            values.put("egon.tianquan-jianshen.runtime.redis-address", "${xingyuan.it.redis.endpoint}");
            values.put("egon.tianquan-jianshen.runtime.redis-database", "${xingyuan.it.redis.database}");
            values.put("egon.tianquan-jianshen.runtime.redis-password-file", "${xingyuan.it.redis.credential-file}");
        }
        if (role.equals("edge")) { values.put("spring.session.redis.namespace", "xingyuan-it:${xingyuan.it.run-id}:bff:session:v3"); }
        values.put("egon.tianquan-shoubing.development-bootstrap.enabled", "false");
        values.put("egon.tianquan-jianshen.development-bootstrap.enabled", "false");
        values.put("egon.tianquan-jianshen.development-bootstrap.auto-activate-local-admin-roles", "false");
        if (role.equals("shoubing")) {
            values.put("egon.tianquan-shoubing.oauth.local-explicit-service-registrations", "true");
            values.put("egon.tianquan-shoubing.oauth.public-origin", "${xingyuan.it.public-origin}");
            values.put("egon.tianquan-shoubing.identity-directory.platform-storage-partition-id", inputs.getManifest().getPlatformStoragePartitionId());
            values.put("egon.tianquan-shoubing.oauth.platform-management-tenant-id", "${xingyuan.it.tenant.default.id}");
        }
        if (role.equals("jianshen") || role.equals("tianshu")) {
            String component = "egon.cola.component.mybatis-plus.sharding.";
            String physical = role.equals("jianshen") ? "rbac_primary" : "tianshu_0";
            String database = role.equals("jianshen") ? "rbac" : "tianshu";
            values.put(component + "enabled", "true"); values.put(component + "mode", "SHARDING");
            values.put(component + "config-style", "NATIVE");
            values.put(component + "native-rules-resource", "file:${xingyuan.it.runtime-root}/profiles/" + database + "-sharding.yml");
            values.put(component + "data-sources[0].name", physical); values.put(component + "data-sources[0].logical-name", physical);
            values.put(component + "data-sources[0].role", "PRIMARY"); values.put(component + "data-sources[0].driver-class-name", "org.postgresql.Driver");
            values.put(component + "data-sources[0].jdbc-url", "${xingyuan.it.postgres." + database + ".url}");
            values.put(component + "data-sources[0].username", "${xingyuan.it.postgres." + database + ".username}");
            values.put(component + "data-sources[0].password", "${xingyuan.it.postgres.password}");
        }
        if (role.equals("yuheng")) {
            values.put("yuheng.persistence.managed-ddl-enabled", "true");
            values.put("egon.cola.component.mybatis-plus.ddl.enabled", "true");
            values.put("egon.cola.component.transactional-outbox.storage.mp.auto-initialize-ddl", "true");
        }
        Set<String> keys = new HashSet<>(defaults.keySet()); keys.addAll(values.keySet());
        boolean jdbc = keys.stream().anyMatch(key -> key.equals("spring.datasource.url") || key.endsWith(".jdbc-url"));
        if (jdbc) {
            String databaseRole = Map.ofEntries(Map.entry("shoubing", "idp"), Map.entry("jianshen", "rbac"), Map.entry("tianshu", "tianshu"),
                    Map.entry("yuheng", "yuheng"), Map.entry("biz", "yuheng"), Map.entry("mcp", "yuheng"), Map.entry("llm", "llm"),
                    Map.entry("native-light", "light"), Map.entry("native-web", "web"), Map.entry("native-service", "service"), Map.entry("native-agent", "agent")).get(role);
            if (databaseRole == null) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            String prefix = "${xingyuan.it.postgres." + databaseRole;
            values.put("spring.datasource.url", prefix + ".url}"); values.put("spring.datasource.username", prefix + ".username}");
            values.put("spring.datasource.password", "${xingyuan.it.postgres.password}");
            values.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
            for (String key : keys) {
                if (key.startsWith("egon.cola.component.mybatis-plus.sharding.data-sources[") && key.endsWith(".jdbc-url")) {
                    values.put(key, prefix + ".url}");
                    values.put(key.substring(0, key.length() - "jdbc-url".length()) + "username", prefix + ".username}");
                    values.put(key.substring(0, key.length() - "jdbc-url".length()) + "password", "${xingyuan.it.postgres.password}");
                }
            }
        }
    }

    private static void validateRuntimeTransport(String role, ProcessTargetDTO process, VerifiedDatabaseInputsBO inputs,
            IsolatedRuntimeRegistryDTO registry, Map<String, String> values) throws IOException {
        var sources = new org.springframework.core.env.MutablePropertySources();
        sources.addFirst(new org.springframework.core.env.MapPropertySource("generatedRuntime", new java.util.LinkedHashMap<>(values)));
        var resolver = new org.springframework.core.env.PropertySourcesPropertyResolver(sources);
        if (!"127.0.0.1".equals(resolver.getProperty("server.address")) || !Integer.toString(runtimeManagementPort(process)).equals(resolver.getProperty("server.port"))
                || !"true".equals(resolver.getProperty("server.ssl.enabled")) || !"false".equals(resolver.getProperty("spring.docker.compose.enabled"))
                || !Integer.toString(inputs.getManifest().getRedis().getDatabase()).equals(resolver.getProperty("spring.data.redis.database"))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        String databaseRole = Map.ofEntries(Map.entry("shoubing", "idp"), Map.entry("jianshen", "rbac"), Map.entry("tianshu", "tianshu"),
                Map.entry("yuheng", "yuheng"), Map.entry("biz", "yuheng"), Map.entry("mcp", "yuheng"), Map.entry("llm", "llm"),
                Map.entry("native-light", "light"), Map.entry("native-web", "web"), Map.entry("native-service", "service"), Map.entry("native-agent", "agent")).get(role);
        String expectedJdbc = databaseRole == null ? null : "jdbc:postgresql://127.0.0.1:5432/" + DATABASE_NAMES.get(databaseRole);
        if (!role.equals("edge") && (!"true".equals(resolver.getProperty("egon.cola.platform.tianquan.shoubing.enabled"))
                || !"true".equals(resolver.getProperty("egon.cola.platform.tianquan.jianshen.enabled"))
                || !"true".equals(resolver.getProperty("egon.cola.platform.tianquan.jianshen.user-scope-policy.enabled")))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        Set<String> issuers = registry.getRealms().values().stream().map(realm -> realm.getIssuer().toASCIIString()).collect(java.util.stream.Collectors.toSet());
        for (String key : values.keySet()) {
            String suffix = key.endsWith(".token-uri") ? "/oauth2/token" : key.endsWith(".authorization-uri") ? "/oauth2/authorize"
                    : key.endsWith(".jwk-set-uri") || key.endsWith(".jwks-uri") ? "/.well-known/jwks.json"
                    : key.endsWith(".user-info-uri") ? "/userinfo" : key.endsWith(".issuer-uri") || key.endsWith(".issuer") || key.endsWith(".fallback-issuer") ? "" : null;
            String value = resolver.getProperty(key);
            if (suffix != null && issuers.stream().noneMatch(issuer -> (issuer + suffix).equals(value))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            if ((key.endsWith(".jdbc-url") || key.equals("spring.datasource.url")) && (expectedJdbc == null || !expectedJdbc.equals(value))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            if (key.contains("redis") && (key.endsWith(".database") || key.endsWith(".redis-database"))
                    && !Integer.toString(inputs.getManifest().getRedis().getDatabase()).equals(value)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        }
        var environment = new org.springframework.core.env.StandardEnvironment();
        environment.getPropertySources().remove(org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(org.springframework.core.env.StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("generatedRuntime", new java.util.LinkedHashMap<>(values)));
        var clients = org.springframework.boot.context.properties.bind.Binder.get(environment).bind("spring.security.oauth2.client",
                org.springframework.boot.context.properties.bind.Bindable.of(org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.class))
                .orElseGet(org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties::new);
        if (clients.getRegistration().isEmpty()) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
        for (var entry : clients.getRegistration().entrySet()) {
            var matches = registry.getClients().stream().filter(client -> entry.getKey().equals(client.getRegistrationId())).toList();
            if (matches.size() != 1) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            var fact = matches.getFirst(); var configured = entry.getValue();
            var provider = clients.getProvider().get(configured.getProvider() == null ? entry.getKey() : configured.getProvider());
            if (fact.getClientType() != OAuthClient.ClientType.CONFIDENTIAL || fact.getStatus() != OAuthClient.Status.ACTIVE
                    || !fact.getClientId().equals(configured.getClientId()) || !"client_secret_basic".equals(configured.getClientAuthenticationMethod())
                    || provider == null || !fact.getIssuer().equals(provider.getIssuerUri())
                    || !(fact.getIssuer() + "/oauth2/token").equals(provider.getTokenUri())
                    || !(fact.getIssuer() + "/.well-known/jwks.json").equals(provider.getJwkSetUri())
                    || configured.getScope() == null || configured.getScope().isEmpty() || !fact.getAllowedScopes().containsAll(configured.getScope())
                    || !((fact.getPurpose() == OAuthClientPurposeEnum.SERVICE ? "client_credentials" : "authorization_code").equals(configured.getAuthorizationGrantType()))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            char[] secret = readProtectedCharacters(Path.of(fact.getCredentialFile()), Path.of(inputs.getManifest().getRuntimeRoot()).resolve("secrets"), 4096);
            try {
                if (!new String(secret).equals(configured.getClientSecret())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            } finally { java.util.Arrays.fill(secret, '\0'); }
            if (fact.getPurpose() == OAuthClientPurposeEnum.USER) {
                String template = inputs.getManifest().getPublicOrigin() + "/login/oauth2/code/{registrationId}";
                String callback = template.replace("{registrationId}", entry.getKey());
                if (!role.equals("edge") || !configured.getScope().contains("openid") || !template.equals(configured.getRedirectUri())
                        || !java.net.URI.create(callback).normalize().toASCIIString().equals(callback)
                        || fact.getRedirectUris() == null || !fact.getRedirectUris().contains(callback)) {
                    throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                }
            }
        }
    }

    @Test
    void generatedRuntimeProfilesBindTheActualClientAndRejectUnsafeOverrides() throws Exception {
        var manifest = intent();
        Path root = Path.of(manifest.getRuntimeRoot());
        Files.createDirectories(root.resolve("secrets"));
        Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
        Files.setPosixFilePermissions(root.resolve("secrets"), PosixFilePermissions.fromString("rwx------"));
        Path secretFile = root.resolve("secrets/controlled-client.txt");
        String secret = " controlled ${opaque-client-secret}";
        Files.writeString(secretFile, secret);
        Files.setPosixFilePermissions(secretFile, PosixFilePermissions.fromString("rw-------"));
        var registry = runtimeRegistryFixture(manifest, "a".repeat(64));
        String issuer = registry.getRealms().get("default").getIssuer().toASCIIString();
        registry.setClients(List.of(IsolatedClientFactDTO.builder().clientPk(23L).clientId("controlled-client")
                .clientType(OAuthClient.ClientType.CONFIDENTIAL).purpose(OAuthClientPurposeEnum.SERVICE).status(OAuthClient.Status.ACTIVE)
                .realmCode("default").issuer(issuer).credentialFile(secretFile.toString()).registrationId("controlled")
                .allowedScopes(List.of("read")).build()));
        var inputs = VerifiedDatabaseInputsBO.builder().manifest(manifest).manifestSha256("a".repeat(64)).build();
        var process = ProcessTargetDTO.builder().id("shoubing").artifactRole("shoubing").kind("JAVA").port(28120).build();
        Map<String, String> values = new java.util.LinkedHashMap<>();
        values.put("server.address", "127.0.0.1"); values.put("server.port", "28120"); values.put("server.ssl.enabled", "true");
        values.put("spring.docker.compose.enabled", "false"); values.put("spring.data.redis.database", "15");
        values.put("egon.cola.platform.tianquan.shoubing.enabled", "true"); values.put("egon.cola.platform.tianquan.jianshen.enabled", "true");
        values.put("egon.cola.platform.tianquan.jianshen.user-scope-policy.enabled", "true");
        String registration = "spring.security.oauth2.client.registration.controlled.";
        values.put(registration + "client-id", "controlled-client"); values.put(registration + "client-secret", secret.replace("${", "\\${"));
        values.put(registration + "provider", "trusted"); values.put(registration + "authorization-grant-type", "client_credentials");
        values.put(registration + "client-authentication-method", "client_secret_basic"); values.put(registration + "scope", "read");
        values.put("spring.security.oauth2.client.provider.trusted.issuer-uri", issuer);
        values.put("spring.security.oauth2.client.provider.trusted.token-uri", issuer + "/oauth2/token");
        values.put("spring.security.oauth2.client.provider.trusted.jwk-set-uri", issuer + "/.well-known/jwks.json");
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> validateRuntimeTransport("shoubing", process, inputs, registry, values));
        values.put("egon.cola.platform.tianquan.shoubing.enabled", "false");
        assertThatThrownBy(() -> validateRuntimeTransport("shoubing", process, inputs, registry, values)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        values.put("egon.cola.platform.tianquan.shoubing.enabled", "true");
        values.put(registration + "client-id", "another-client");
        assertThatThrownBy(() -> validateRuntimeTransport("shoubing", process, inputs, registry, values)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        values.put(registration + "client-id", "controlled-client");
        values.put("spring.data.redis.database", "0");
        assertThatThrownBy(() -> validateRuntimeTransport("shoubing", process, inputs, registry, values)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        values.put("spring.data.redis.database", "15");
        values.put("spring.datasource.url", "jdbc:postgresql://127.0.0.1:5432/" + DATABASE_NAMES.get("rbac"));
        assertThatThrownBy(() -> validateRuntimeTransport("shoubing", process, inputs, registry, values)).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    @Test
    void bffRuntimeProfileUsesTheOriginalTemplateAndTheActualAsCallback() throws Exception {
        var manifest = intent(); Path root = Path.of(manifest.getRuntimeRoot());
        Files.createDirectories(root.resolve("secrets"));
        Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
        Files.setPosixFilePermissions(root.resolve("secrets"), PosixFilePermissions.fromString("rwx------"));
        Path secretFile = root.resolve("secrets/controlled-bff.txt");
        String secret = " controlled ${opaque-bff-secret}";
        Files.writeString(secretFile, secret); Files.setPosixFilePermissions(secretFile, PosixFilePermissions.fromString("rw-------"));
        var registry = runtimeRegistryFixture(manifest, "a".repeat(64));
        String issuer = registry.getRealms().get("default").getIssuer().toASCIIString();
        String callback = manifest.getPublicOrigin() + "/login/oauth2/code/controlled-bff";
        var client = IsolatedClientFactDTO.builder().clientPk(23L).clientId("actual-bff-client")
                .clientType(OAuthClient.ClientType.CONFIDENTIAL).purpose(OAuthClientPurposeEnum.USER).status(OAuthClient.Status.ACTIVE)
                .realmCode("default").issuer(issuer).credentialFile(secretFile.toString()).registrationId("controlled-bff")
                .allowedScopes(List.of("openid", "profile")).redirectUris(List.of(callback)).build();
        registry.setClients(List.of(client));
        var inputs = VerifiedDatabaseInputsBO.builder().manifest(manifest).manifestSha256("a".repeat(64)).build();
        var process = ProcessTargetDTO.builder().id("edge").artifactRole("edge").kind("JAVA").port(28200).build();
        Map<String, String> values = new java.util.LinkedHashMap<>();
        values.put("server.address", "127.0.0.1"); values.put("server.port", "28200"); values.put("server.ssl.enabled", "true");
        values.put("spring.docker.compose.enabled", "false"); values.put("spring.data.redis.database", "15");
        String registration = "spring.security.oauth2.client.registration.controlled-bff.";
        values.put(registration + "client-id", client.getClientId()); values.put(registration + "client-secret", secret.replace("${", "\\${"));
        values.put(registration + "provider", "trusted"); values.put(registration + "authorization-grant-type", "authorization_code");
        values.put(registration + "client-authentication-method", "client_secret_basic"); values.put(registration + "scope", "openid,profile");
        values.put(registration + "redirect-uri", manifest.getPublicOrigin() + "/login/oauth2/code/{registrationId}");
        values.put("spring.security.oauth2.client.provider.trusted.issuer-uri", issuer);
        values.put("spring.security.oauth2.client.provider.trusted.token-uri", issuer + "/oauth2/token");
        values.put("spring.security.oauth2.client.provider.trusted.jwk-set-uri", issuer + "/.well-known/jwks.json");
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> validateRuntimeTransport("edge", process, inputs, registry, values));
        for (String rejected : List.of(callback, manifest.getPublicOrigin() + "/login/oauth2/code/another-registration",
                "https://foreign.invalid/login/oauth2/code/{registrationId}", "{baseUrl}/login/oauth2/code/{registrationId}")) {
            values.put(registration + "redirect-uri", rejected);
            assertThatThrownBy(() -> validateRuntimeTransport("edge", process, inputs, registry, values)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
        values.put(registration + "redirect-uri", manifest.getPublicOrigin() + "/login/oauth2/code/{registrationId}");
        client.setRedirectUris(List.of(manifest.getPublicOrigin() + "/login/oauth2/code/another-registration"));
        assertThatThrownBy(() -> validateRuntimeTransport("edge", process, inputs, registry, values)).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    static void validatePackagedEdgeProperties(ArtifactReferenceDTO artifact, Path repository, VerifiedDatabaseInputsBO inputs,
            IsolatedRuntimeRegistryDTO registry, Map<String, String> values) throws IOException {
        var processes = inputs.getManifest().getProcesses().stream().filter(value -> "edge".equals(value.getId()) && "JAVA".equals(value.getKind())).toList();
        if (processes.size() != 1) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
        var facts = runtimeRegistryFacts(inputs, registry);
        validateRuntimeTransport("edge", processes.getFirst(), inputs, registry, values);
        var defaults = artifactRuntimeDefaults(artifact, repository, "local");
        validateRuntimeRoutes(renderRuntimeProperties(Map.of(), edgeRouteTemplate(defaults), facts), values);
        var classes = java.net.URI.create("jar:" + Path.of(artifact.getPath()).toUri().toASCIIString() + "!/BOOT-INF/classes/").toURL();
        try (var loader = new java.net.URLClassLoader(new java.net.URL[]{classes}, IsolatedOAuthProvisioningIT.class.getClassLoader())) {
            try {
                Class<?> type = Class.forName("top.egon.cola.component.yuheng.edge.config.EdgeOAuthProperties", true, loader);
                if (type.getClassLoader() != loader) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                var environment = new org.springframework.core.env.StandardEnvironment();
                environment.getPropertySources().remove(org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                environment.getPropertySources().remove(org.springframework.core.env.StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
                environment.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("edgeActualProfile", new java.util.LinkedHashMap<>(values)));
                Object properties = org.springframework.boot.context.properties.bind.Binder.get(environment).bind("egon.yuheng.edge",
                        org.springframework.boot.context.properties.bind.Bindable.of(type))
                        .orElseThrow(() -> new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"));
                try (var validators = Validation.buildDefaultValidatorFactory()) {
                    new top.egon.cola.component.common.core.validation.ValidationUtils(validators.getValidator()).validate(properties);
                }
                type.getMethod("validate").invoke(properties);
                requireEdgeRegistryBindings(new org.springframework.beans.BeanWrapperImpl(properties), inputs, registry, values, environment);
            } catch (ReflectiveOperationException invalid) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID", invalid); }
            finally { org.springframework.beans.CachedIntrospectionResults.clearClassLoader(loader); }
        }
    }

    private static void requireEdgeRegistryBindings(org.springframework.beans.BeanWrapper properties, VerifiedDatabaseInputsBO inputs,
            IsolatedRuntimeRegistryDTO registry, Map<String, String> values, org.springframework.core.env.ConfigurableEnvironment environment) throws IOException {
        var manifest = inputs.getManifest();
        Set<String> aliases = Set.of("edge-ui", "edge-shoubing", "edge-jianshen", "edge-yuheng", "edge-tianshu");
        if (!URI.create(manifest.getPublicOrigin()).equals(properties.getPropertyValue("publicOrigin"))
                || !"edge-ui".equals(properties.getPropertyValue("defaultRegistrationId"))
                || registry.getAliases() == null || !registry.getAliases().keySet().equals(aliases)
                || values.keySet().stream().anyMatch(key -> key.startsWith("egon.yuheng.edge.resource-bindings."))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        for (var endpoint : Map.of("authorizationServer", "shoubing", "bizGateway", "biz", "mcpGateway", "mcp").entrySet()) {
            var targets = manifest.getProcesses().stream().filter(value -> endpoint.getValue().equals(value.getId()) && "JAVA".equals(value.getKind())).toList();
            if (targets.size() != 1 || !URI.create("https://127.0.0.1:" + targets.getFirst().getPort()).equals(properties.getPropertyValue("upstreams." + endpoint.getKey()))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
        }
        String key = manifest.getSecretFiles().get("edge-session");
        if (key == null || !key.equals(properties.getPropertyValue("session.encryption.keyFile"))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        byte[] bytes = readProtectedBytes(Path.of(key), Path.of(manifest.getRuntimeRoot()).resolve("secrets"), 32);
        try { if (bytes.length != 32) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); } }
        finally { java.util.Arrays.fill(bytes, (byte) 0); }
        String namespace = "xingyuan-it:" + manifest.getRunId() + ":";
        String sessionNamespace = environment.getProperty("spring.session.redis.namespace");
        if (!((String) properties.getPropertyValue("logout.stream")).startsWith(namespace)
                || !((String) properties.getPropertyValue("logout.consumerGroup")).startsWith(namespace)
                || sessionNamespace == null || !sessionNamespace.startsWith(namespace)) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        Map<?, ?> resources = (Map<?, ?>) properties.getPropertyValue("resources");
        if (!resources.keySet().equals(aliases)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var configured = org.springframework.boot.context.properties.bind.Binder.get(environment).bind("spring.security.oauth2.client",
                org.springframework.boot.context.properties.bind.Bindable.of(org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.class))
                .orElseThrow(() -> new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"));
        Set<String> selected = new HashSet<>(); Set<String> clientIds = new HashSet<>();
        Set<Object> resourceIds = new HashSet<>(); Set<Object> resourceUris = new HashSet<>();
        List<?> returnPaths = (List<?>) properties.getPropertyValue("allowedReturnPaths");
        Set<String> expectedIssuers = manifest.getTenantCodes().stream().map(tenant -> registry.getRealms().get(tenant).getIssuer().toASCIIString()).collect(java.util.stream.Collectors.toSet());
        for (String alias : aliases) {
            var binding = new org.springframework.beans.BeanWrapperImpl(resources.get(alias));
            if (!resourceIds.add(binding.getPropertyValue("resourceServerId")) || !resourceUris.add(binding.getPropertyValue("resourceUri"))
                    || !returnPaths.contains(binding.getPropertyValue("returnPath"))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            var declared = registry.getAliases().get(alias);
            Map<?, ?> registrations = (Map<?, ?>) binding.getPropertyValue("issuerRegistrationIds");
            if (!registrations.equals(declared.getIssuerRegistrationIds()) || !registrations.keySet().equals(expectedIssuers)
                    || !Objects.equals(binding.getPropertyValue("defaultRegistrationId"), declared.getDefaultRegistrationId())
                    || !Objects.equals(declared.getDefaultRegistrationId(), declared.getIssuerRegistrationIds().get(registry.getRealms().get("default").getIssuer().toASCIIString()))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            for (String tenant : manifest.getTenantCodes()) {
                String issuer = registry.getRealms().get(tenant).getIssuer().toASCIIString();
                String registration = declared.getIssuerRegistrationIds().get(issuer);
                var matches = registry.getClients().stream().filter(value -> registration.equals(value.getRegistrationId())).toList();
                if (matches.size() != 1 || !selected.add(registration)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                var client = matches.getFirst(); var local = configured.getRegistration().get(registration);
                if (client.getClientType() != OAuthClient.ClientType.CONFIDENTIAL || client.getPurpose() != OAuthClientPurposeEnum.USER
                        || client.getStatus() != OAuthClient.Status.ACTIVE || !alias.equals(client.getAlias()) || !tenant.equals(client.getRealmCode())
                        || !issuer.equals(client.getIssuer()) || !clientIds.add(client.getClientId()) || client.getResourceUris().size() != 1
                        || local == null || !client.getClientId().equals(local.getClientId())
                        || !new HashSet<>(client.getAllowedScopes()).equals(binding.getPropertyValue("scopes"))
                        || !local.getScope().equals(binding.getPropertyValue("scopes"))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                var actual = registry.getResources().stream().filter(value -> client.getResourceUris().getFirst().equals(value.getResourceUri())).toList();
                if (actual.size() != 1 || !actual.getFirst().getResourceServerId().equals(binding.getPropertyValue("resourceServerId"))
                        || !URI.create(actual.getFirst().getResourceUri()).equals(binding.getPropertyValue("resourceUri"))) {
                    throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                }
            }
        }
        for (String registration : configured.getRegistration().keySet()) {
            if (!selected.contains(registration) && registry.getClients().stream().anyMatch(value -> registration.equals(value.getRegistrationId()) && value.getPurpose() == OAuthClientPurposeEnum.USER)) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
        }
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "xingyuan.isolated.edge-profile.enabled", matches = "true")
    void packagedEdgePropertiesBindOfflineWithoutStartingApplications() throws Exception {
        Path repository = Path.of(requiredOperationProperty("xingyuan.isolated.repository-root")).toRealPath();
        var artifact = ArtifactReferenceDTO.builder().path(requiredOperationProperty("xingyuan.isolated.edge-profile-artifact"))
                .sha256(requiredOperationProperty("xingyuan.isolated.edge-profile-artifact-sha256")).build();
        var manifest = intent(); Path root = Path.of(manifest.getRuntimeRoot());
        Files.createDirectories(root.resolve("secrets")); Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
        Files.setPosixFilePermissions(root.resolve("secrets"), PosixFilePermissions.fromString("rwx------"));
        Path key = root.resolve("secrets/edge-session.bin"); Files.write(key, new byte[32]);
        Files.setPosixFilePermissions(key, PosixFilePermissions.fromString("rw-------"));
        manifest.setSecretFiles(Map.of("edge-session", key.toString()));
        manifest.setProcesses(List.of(ProcessTargetDTO.builder().id("edge").kind("JAVA").port(28200).build(), ProcessTargetDTO.builder().id("shoubing").kind("JAVA").port(28120).build(),
                ProcessTargetDTO.builder().id("jianshen").kind("JAVA").port(28130).build(), ProcessTargetDTO.builder().id("tianshu").kind("JAVA").port(28150).build(),
                ProcessTargetDTO.builder().id("yuheng").kind("JAVA").port(28140).build(),
                ProcessTargetDTO.builder().id("biz").kind("JAVA").port(28180).build(), ProcessTargetDTO.builder().id("mcp").kind("JAVA").port(28185).build()));
        var inputs = VerifiedDatabaseInputsBO.builder().manifest(manifest).manifestSha256("a".repeat(64)).build();
        var registry = runtimeRegistryFixture(manifest, inputs.getManifestSha256());
        var defaults = artifactRuntimeDefaults(artifact, repository, "local");
        var values = packagedEdgeProfileFixture(inputs, registry, defaults);
        values.putAll(renderRuntimeProperties(Map.of(), edgeRouteTemplate(defaults), runtimeRegistryFacts(inputs, registry)));
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> validatePackagedEdgeProperties(artifact, repository, inputs, registry, values));
        for (var invalid : Map.of("egon.yuheng.edge.public-origin", "https://foreign.invalid",
                "egon.yuheng.edge.upstreams.biz-gateway", "https://127.0.0.1:28140",
                "egon.yuheng.edge.session.encryption.key-file", root.resolve("secrets/another-key.bin").toString(),
                "egon.yuheng.edge.logout.stream", "old-system-stream",
                "spring.session.redis.namespace", "old-system-session",
                "egon.yuheng.edge.default-registration-id", "edge-shoubing",
                "egon.yuheng.edge.resources.edge-ui.return-path", "/unlisted",
                "egon.yuheng.edge.resources.edge-ui.resource-uri", "https://resource.invalid/edge-shoubing",
                "spring.cloud.gateway.server.webflux.routes[1].uri", "https://127.0.0.1:18120",
                "spring.cloud.gateway.server.webflux.routes[1].filters[0]", "TokenRelay").entrySet()) {
            var changed = new java.util.TreeMap<>(values); changed.put(invalid.getKey(), invalid.getValue());
            assertThatThrownBy(() -> validatePackagedEdgeProperties(artifact, repository, inputs, registry, changed)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
        var excessiveTimeout = new java.util.TreeMap<>(values);
        excessiveTimeout.put("egon.yuheng.edge.session.absolute-timeout", "PT9H");
        assertThatThrownBy(() -> validatePackagedEdgeProperties(artifact, repository, inputs, registry, excessiveTimeout)).hasMessage("XINGYUAN_INPUT_INVALID");
        var missingNamespace = new java.util.TreeMap<>(values); missingNamespace.remove("spring.session.redis.namespace");
        assertThatThrownBy(() -> validatePackagedEdgeProperties(artifact, repository, inputs, registry, missingNamespace)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        var issuerMismatch = new java.util.TreeMap<>(values);
        issuerMismatch.put("egon.yuheng.edge.resources.edge-ui.issuer-registration-ids.[https://old.invalid/tenants/1001]", "actual-0");
        assertThatThrownBy(() -> validatePackagedEdgeProperties(artifact, repository, inputs, registry, issuerMismatch)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        var duplicateResource = new java.util.TreeMap<>(values);
        duplicateResource.put("egon.yuheng.edge.resources.edge-shoubing.resource-server-id", "controlled-edge-ui");
        duplicateResource.put("egon.yuheng.edge.resources.edge-shoubing.resource-uri", "https://resource.invalid/edge-ui");
        registry.getClients().stream().filter(client -> "edge-shoubing".equals(client.getAlias()))
                .forEach(client -> client.setResourceUris(List.of("https://resource.invalid/edge-ui")));
        assertThatThrownBy(() -> validatePackagedEdgeProperties(artifact, repository, inputs, registry, duplicateResource)).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    private Map<String, String> packagedEdgeProfileFixture(VerifiedDatabaseInputsBO inputs, IsolatedRuntimeRegistryDTO registry,
            Map<String, Object> businessDefaults) throws IOException {
        var manifest = inputs.getManifest(); Path root = Path.of(manifest.getRuntimeRoot());
        Map<String, String> values = new java.util.TreeMap<>(); Map<String, String> privateClientFacts = new java.util.TreeMap<>();
        var clients = new java.util.ArrayList<IsolatedClientFactDTO>();
        var resources = new java.util.ArrayList<top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ResourceServerVO>();
        Map<String, IsolatedResourceAliasDTO> aliases = new java.util.TreeMap<>();
        int index = 0;
        for (String alias : List.of("edge-ui", "edge-shoubing", "edge-jianshen", "edge-yuheng", "edge-tianshu")) {
            String uri = "https://resource.invalid/" + alias; String resourceId = "controlled-" + alias;
            resources.add(top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ResourceServerVO.builder()
                    .resourceServerId(resourceId).resourceUri(uri).bizCode("controlled").appCode(alias).environment("local").displayName(alias)
                    .managementClientId("controlled-manager").rbacApplicationCode(alias).entryPermissionCode(alias + ":entry").status("ACTIVE").build());
            Map<String, String> registrations = new java.util.TreeMap<>(); String first = null;
            for (String tenant : manifest.getTenantCodes()) {
                String issuer = registry.getRealms().get(tenant).getIssuer().toASCIIString(); String registration = "actual-" + index;
                Path credential = root.resolve("secrets/client-" + index + ".txt");
                String secret = "controlled-client-secret-" + index; Files.writeString(credential, secret);
                Files.setPosixFilePermissions(credential, PosixFilePermissions.fromString("rw-------"));
                clients.add(IsolatedClientFactDTO.builder().clientPk((long) ++index).clientId("client-" + index)
                        .clientType(OAuthClient.ClientType.CONFIDENTIAL).purpose(OAuthClientPurposeEnum.USER).status(OAuthClient.Status.ACTIVE)
                        .realmCode(tenant).issuer(issuer).credentialFile(credential.toString()).registrationId(registration).alias(alias)
                        .allowedScopes(List.of("openid", "profile", "offline_access")).resourceUris(List.of(uri))
                        .redirectUris(List.of(manifest.getPublicOrigin() + "/login/oauth2/code/" + registration)).build());
                privateClientFacts.put("xingyuan.it.client." + registration + ".client-secret", secret); registrations.put(issuer, registration);
                if (first == null) { first = registration; }
            }
            aliases.put(alias, IsolatedResourceAliasDTO.builder().defaultRegistrationId(first).issuerRegistrationIds(registrations).build());
        }
        registry.setClients(clients).setResources(resources).setAliases(aliases);
        Map<String, String> facts = new java.util.TreeMap<>(runtimeRegistryFacts(inputs, registry)); facts.putAll(privateClientFacts);
        values.putAll(edgeBffUserClientTemplate());
        facts.put("xingyuan.it.secret.edge-session.file", manifest.getSecretFiles().get("edge-session"));
        values.putAll(edgeHostResourceTemplate(businessDefaults));
        values.put("server.address", "127.0.0.1"); values.put("server.port", "28200"); values.put("server.ssl.enabled", "true");
        values.put("spring.docker.compose.enabled", "false"); values.put("spring.data.redis.database", Integer.toString(manifest.getRedis().getDatabase()));
        return new java.util.TreeMap<>(renderRuntimeProperties(Map.of(), values, facts));
    }

    static void clearRuntimeIdentityDefaults(Map<String, Object> defaults) {
        defaults.keySet().removeIf(key -> key.startsWith("spring.security.oauth2.client.registration.") || key.startsWith("spring.security.oauth2.client.provider.")
                || key.equals("egon.cola.platform.tianquan.shoubing.audit.stream-key")
                || key.equals("egon.cola.platform.tianquan.shoubing.issuer") || key.equals("egon.cola.platform.tianquan.shoubing.jwk-set-uri")
                || key.startsWith("egon.cola.platform.tianquan.shoubing.trusted-issuers.") || key.startsWith("egon.yuheng.edge.resource-bindings.") || key.startsWith("egon.yuheng.edge.resources."));
    }

    private static int runtimeManagementPort(ProcessTargetDTO process) {
        if (!Set.of("biz", "mcp").contains(process.getId())) { return process.getPort(); }
        var additional = process.getAdditionalPorts();
        if (additional == null || additional.size() != 1 || additional.getFirst() == null || additional.getFirst() < 1
                || additional.getFirst() > 65535 || additional.getFirst() == process.getPort()) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        return additional.getFirst();
    }

    static final Map<String, Integer> RUNTIME_RPC_PORTS = Map.of("shoubing", 28122, "tianshu", 29080,
            "native-light", 29310, "native-web", 29311, "native-service", 29312);

    private static int runtimeRpcPort(ProcessTargetDTO process) {
        Integer expected = RUNTIME_RPC_PORTS.get(process.getId());
        if (expected == null || process.getAdditionalPorts() == null || !process.getAdditionalPorts().equals(List.of(expected))
                || process.getPort() == expected) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        return expected;
    }

    private static void applyRuntimeProfileFragments(String role, Map<String, Object> defaults, Map<String, String> reviewed) {
        Map<String, String> required = new java.util.TreeMap<>(runtimeTrustTemplate());
        required.putAll(runtimeAuditTemplate(role)); required.putAll(runtimeRpcTemplate(role));
        if (Set.of("biz", "mcp").contains(role)) { required.putAll(gatewayListenerTemplate(role)); }
        if (role.equals("edge")) {
            required.putAll(edgeBffUserClientTemplate()); required.putAll(edgeHostResourceTemplate(defaults)); required.putAll(edgeRouteTemplate(defaults));
        }
        required.forEach(reviewed::putIfAbsent);
    }

    static Map<String, String> runtimeRpcTemplate(String role) {
        if (!RUNTIME_PROFILES.containsKey(role)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        Map<String, String> values = new java.util.TreeMap<>();
        String component = "egon.cola.component.rpc.";
        boolean provider = RUNTIME_RPC_PORTS.containsKey(role);
        values.put(component + "provider.enabled", Boolean.toString(provider));
        if (provider) {
            values.put(component + "enabled", "true");
            values.put(component + "provider.bind-address", "127.0.0.1"); values.put(component + "provider.advertised-host", "127.0.0.1");
            values.put(component + "provider.port", "${xingyuan.it.process." + role + ".rpc-port}");
            values.put(component + "provider.advertised-port", "${xingyuan.it.process." + role + ".rpc-port}");
        }
        values.put("egon.cola.component.tianshu.rpc.target", "${xingyuan.it.process.tianshu.rpc-target}");
        for (String tls : List.of(component + "tls.", "egon.cola.component.tianshu.rpc.tls.")) {
            values.put(tls + "enabled", "true"); values.put(tls + "development-plaintext", "false");
            values.put(tls + "certificate-chain-path", "${xingyuan.it.secret.tls-server-cert.file}");
            values.put(tls + "private-key-path", "${xingyuan.it.secret.tls-server-key.file}");
            values.put(tls + "trust-certificate-collection-path", "${xingyuan.it.secret.tls-ca-cert.file}");
        }
        return java.util.Collections.unmodifiableMap(values);
    }

    private static void validateRuntimeRpcTransport(String role, ProcessTargetDTO process, VerifiedDatabaseInputsBO inputs,
            Map<String, String> values) throws IOException {
        if (!role.equals(process.getId()) || !"JAVA".equals(process.getKind()) || !RUNTIME_PROFILES.containsKey(role)
                || !RUNTIME_RPC_PORTS.containsKey(role) && !Set.of("biz", "mcp").contains(role)
                && process.getAdditionalPorts() != null && !process.getAdditionalPorts().isEmpty()) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var binder = new org.springframework.boot.context.properties.bind.Binder(new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(values));
        var rpc = binder.bind("egon.cola.component.rpc", top.egon.cola.component.rpc.config.EgonRpcProperties.class)
                .orElseThrow(() -> new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"));
        var tianshu = binder.bind("egon.cola.component.tianshu.rpc", top.egon.cola.component.rpc.tianshu.autoconfigure.DdcRpcProperties.class)
                .orElseThrow(() -> new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"));
        boolean provider = RUNTIME_RPC_PORTS.containsKey(role);
        var tianshuProcesses = inputs.getManifest().getProcesses().stream().filter(value -> "tianshu".equals(value.getId()) && "JAVA".equals(value.getKind())).toList();
        if (tianshuProcesses.size() != 1 || !tianshu.requireTarget().equals("dns:///127.0.0.1:" + runtimeRpcPort(tianshuProcesses.getFirst()))
                || rpc.getProvider().isEnabled() != provider || provider && (!rpc.isEnabled()
                || !"127.0.0.1".equals(rpc.getProvider().getBindAddress()) || !"127.0.0.1".equals(rpc.getProvider().getAdvertisedHost())
                || rpc.getProvider().getPort() != runtimeRpcPort(process) || !Integer.valueOf(runtimeRpcPort(process)).equals(rpc.getProvider().getAdvertisedPort()))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        if (rpc.getConsumer().isEnabled()) { rpc.getConsumer().validateSharedSettings(); }
        for (Object tls : List.of(rpc.getTls(), tianshu.getTls())) {
            var properties = new org.springframework.beans.BeanWrapperImpl(tls);
            if (!Boolean.TRUE.equals(properties.getPropertyValue("enabled")) || !Boolean.FALSE.equals(properties.getPropertyValue("developmentPlaintext"))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            for (var file : Map.of("certificateChainPath", "tls-server-cert", "privateKeyPath", "tls-server-key", "trustCertificateCollectionPath", "tls-ca-cert").entrySet()) {
                String expected = inputs.getManifest().getSecretFiles().get(file.getValue());
                if (expected == null || !expected.equals(properties.getPropertyValue(file.getKey()))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                byte[] bytes = readProtectedBytes(Path.of(expected), Path.of(inputs.getManifest().getRuntimeRoot()).resolve("secrets"), 64 * 1024);
                try { if (bytes.length == 0) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); } }
                finally { java.util.Arrays.fill(bytes, (byte) 0); }
            }
        }
        new top.egon.cola.component.rpc.config.RpcTransportSecurity(rpc.getTls().isEnabled(), rpc.getTls().isDevelopmentPlaintext(),
                rpc.getTls().getCertificateChainPath(), rpc.getTls().getPrivateKeyPath(), rpc.getTls().getTrustCertificateCollectionPath());
        tianshu.transportSecurity();
    }

    @Test
    void runtimeArtifactsRejectMissingOrStaleStrictAuditClasses(@org.junit.jupiter.api.io.TempDir Path temporary) throws Exception {
        var rpcRoots = List.<Class<?>>of(top.egon.cola.component.rpc.config.EgonRpcProperties.class,
                top.egon.cola.component.rpc.tianshu.autoconfigure.DdcRpcProperties.class, top.egon.cola.component.rpc.config.RpcTransportSecurity.class,
                top.egon.cola.component.rpc.provider.registration.RpcProviderRegistrationMode.class, top.egon.cola.component.rpc.annotation.LoadBalance.class);
        var sdkRoots = List.<Class<?>>of(top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent.class,
                top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterAutoConfiguration.class);
        var rpc = new java.io.ByteArrayOutputStream(); var sdk = new java.util.LinkedHashMap<String, byte[]>();
        try (var jar = new java.util.jar.JarOutputStream(rpc)) {
            var pending = new java.util.ArrayList<>(rpcRoots);
            for (int index = 0; index < pending.size(); index++) {
                Class<?> type = pending.get(index); String name = type.getName().replace('.', '/') + ".class";
                try (var bytes = type.getResourceAsStream('/' + name)) { jar.putNextEntry(new java.util.jar.JarEntry(name)); jar.write(bytes.readAllBytes()); jar.closeEntry(); }
                pending.addAll(List.of(type.getDeclaredClasses()));
            }
        }
        var pending = new java.util.ArrayList<>(sdkRoots);
        for (int index = 0; index < pending.size(); index++) {
            Class<?> type = pending.get(index); String name = type.getName().replace('.', '/') + ".class";
            try (var bytes = type.getResourceAsStream('/' + name)) { sdk.put(name, bytes.readAllBytes()); }
            pending.addAll(List.of(type.getDeclaredClasses()));
        }
        String observedClass = sdkRoots.getFirst().getName().replace('.', '/') + ".class";
        for (String scenario : List.of("current", "missing", "stale", "duplicate", "shadow")) {
            var nested = new java.io.ByteArrayOutputStream();
            try (var jar = new java.util.jar.JarOutputStream(nested)) {
                for (var entry : sdk.entrySet()) {
                    if (scenario.equals("missing") && entry.getKey().equals(observedClass)) { continue; }
                    byte[] bytes = entry.getValue().clone();
                    if (scenario.equals("stale") && entry.getKey().equals(observedClass)) { bytes[bytes.length - 1] ^= 1; }
                    jar.putNextEntry(new java.util.jar.JarEntry(entry.getKey())); jar.write(bytes); jar.closeEntry();
                }
            }
            Path file = temporary.resolve("controlled-" + scenario + ".jar");
            try (var jar = new java.util.jar.JarOutputStream(Files.newOutputStream(file))) {
                jar.putNextEntry(new java.util.jar.JarEntry("BOOT-INF/lib/egon-cola-component-rpc-controlled.jar")); jar.write(rpc.toByteArray()); jar.closeEntry();
                jar.putNextEntry(new java.util.jar.JarEntry("BOOT-INF/lib/egon-cola-tianquan-shoubing-starter-controlled.jar")); jar.write(nested.toByteArray()); jar.closeEntry();
                if (scenario.equals("duplicate")) {
                    jar.putNextEntry(new java.util.jar.JarEntry("BOOT-INF/lib/egon-cola-tianquan-shoubing-starter-duplicate.jar")); jar.write(nested.toByteArray()); jar.closeEntry();
                }
                if (scenario.equals("shadow")) { jar.putNextEntry(new java.util.jar.JarEntry("BOOT-INF/classes/" + observedClass)); jar.write(sdk.get(observedClass)); jar.closeEntry(); }
            }
            var artifact = ArtifactReferenceDTO.builder().path(file.toString()).sha256(sha256Bytes(Files.readAllBytes(file))).buildId("controlled").build();
            if (scenario.equals("current")) { assertThatCode(() -> requirePackagedRuntimeRpcModels(artifact)).doesNotThrowAnyException(); }
            else { assertThatThrownBy(() -> requirePackagedRuntimeRpcModels(artifact)).hasMessage(scenario.equals("missing") ? "XINGYUAN_INPUT_UNAVAILABLE" : "XINGYUAN_TARGET_UNSAFE"); }
        }
    }

    private static void requirePackagedRuntimeRpcModels(ArtifactReferenceDTO artifact) throws IOException {
        requirePackagedRuntimeClassBytes(artifact, List.of(top.egon.cola.component.rpc.config.EgonRpcProperties.class,
                top.egon.cola.component.rpc.tianshu.autoconfigure.DdcRpcProperties.class,
                top.egon.cola.component.rpc.config.RpcTransportSecurity.class,
                top.egon.cola.component.rpc.provider.registration.RpcProviderRegistrationMode.class,
                top.egon.cola.component.rpc.annotation.LoadBalance.class), "BOOT-INF/lib/egon-cola-component-rpc-");
        requirePackagedRuntimeClassBytes(artifact, List.of(top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent.class,
                top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterAutoConfiguration.class), "BOOT-INF/lib/egon-cola-tianquan-shoubing-starter-");
    }

    private static void requirePackagedRuntimeClassBytes(ArtifactReferenceDTO artifact, List<Class<?>> roots, String libraryPrefix) throws IOException {
        var pending = new java.util.ArrayList<>(roots);
        Map<String, byte[]> expected = new java.util.HashMap<>();
        for (int index = 0; index < pending.size(); index++) {
            Class<?> type = pending.get(index); String name = type.getName().replace('.', '/') + ".class";
            try (var source = type.getResourceAsStream('/' + name)) {
                if (source == null) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
                expected.put(name, boundedArtifactResource(source));
            }
            pending.addAll(List.of(type.getDeclaredClasses()));
        }
        Set<String> found = new HashSet<>();
        try (var jar = new java.util.jar.JarFile(artifact.getPath(), false)) {
            for (String name : expected.keySet()) {
                if (jar.getJarEntry("BOOT-INF/classes/" + name) != null) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            }
            var libraries = jar.entries();
            while (libraries.hasMoreElements()) {
                var library = libraries.nextElement();
                if (!library.getName().startsWith(libraryPrefix) || !library.getName().endsWith(".jar")) { continue; }
                try (var nested = new java.util.jar.JarInputStream(jar.getInputStream(library), false)) {
                    for (java.util.jar.JarEntry entry; (entry = nested.getNextJarEntry()) != null;) {
                        byte[] model = expected.get(entry.getName());
                        if (model != null && (!found.add(entry.getName()) || !MessageDigest.isEqual(model, boundedArtifactResource(nested)))) {
                            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                        }
                    }
                }
            }
        }
        if (!found.equals(expected.keySet())) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
    }

    @Test
    void runtimeRpcProfilesBindOriginalModelsAndRejectUnownedTransports() throws Exception {
        var manifest = intent(); Path root = Path.of(manifest.getRuntimeRoot());
        Files.createDirectories(root.resolve("secrets")); Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
        Files.setPosixFilePermissions(root.resolve("secrets"), PosixFilePermissions.fromString("rwx------"));
        Map<String, String> secrets = new java.util.TreeMap<>();
        for (String key : List.of("tls-server-cert", "tls-server-key", "tls-ca-cert")) {
            Path file = root.resolve("secrets/" + key + ".model-only"); Files.writeString(file, "controlled-path-binding-only");
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------")); secrets.put(key, file.toString());
        }
        var ports = Map.ofEntries(Map.entry("shoubing", 28120), Map.entry("jianshen", 28130), Map.entry("tianshu", 28150),
                Map.entry("yuheng", 28140), Map.entry("biz", 28180), Map.entry("mcp", 28185), Map.entry("llm", 28190), Map.entry("edge", 28200),
                Map.entry("native-light", 28310), Map.entry("native-web", 28311), Map.entry("native-service", 28312), Map.entry("native-agent", 28313));
        var processes = ports.entrySet().stream().map(entry -> ProcessTargetDTO.builder().id(entry.getKey()).kind("JAVA").port(entry.getValue())
                .additionalPorts(RUNTIME_RPC_PORTS.containsKey(entry.getKey()) ? List.of(RUNTIME_RPC_PORTS.get(entry.getKey()))
                        : entry.getKey().equals("biz") ? List.of(28182) : entry.getKey().equals("mcp") ? List.of(28186) : List.<Integer>of()).build()).toList();
        manifest.setSecretFiles(secrets).setProcesses(processes);
        var inputs = VerifiedDatabaseInputsBO.builder().manifest(manifest).manifestSha256("a".repeat(64)).build();
        Map<String, String> facts = new java.util.TreeMap<>(runtimeRegistryFacts(inputs, runtimeRegistryFixture(manifest, inputs.getManifestSha256())));
        secrets.forEach((key, path) -> facts.put("xingyuan.it.secret." + key + ".file", path));
        for (var process : processes) {
            String role = process.getId(); var values = renderRuntimeProperties(Map.of(), runtimeRpcTemplate(role), facts);
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> validateRuntimeRpcTransport(role, process, inputs, values));
            for (var change : Map.of("egon.cola.component.rpc.tls.enabled", "false", "egon.cola.component.rpc.tls.development-plaintext", "true",
                    "egon.cola.component.tianshu.rpc.target", "dns:///foreign:29080", "egon.cola.component.tianshu.rpc.tls.private-key-path", root.resolve("secrets/foreign-key").toString()).entrySet()) {
                var wrong = new java.util.TreeMap<>(values); wrong.put(change.getKey(), change.getValue());
                assertThatThrownBy(() -> validateRuntimeRpcTransport(role, process, inputs, wrong)).hasMessage("XINGYUAN_TARGET_UNSAFE");
            }
            var wrong = new java.util.TreeMap<>(values);
            if (RUNTIME_RPC_PORTS.containsKey(role)) { wrong.put("egon.cola.component.rpc.provider.bind-address", "0.0.0.0"); }
            else { wrong.put("egon.cola.component.rpc.provider.enabled", "true"); }
            assertThatThrownBy(() -> validateRuntimeRpcTransport(role, process, inputs, wrong)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
        var provider = processes.stream().filter(value -> "native-light".equals(value.getId())).findFirst().orElseThrow();
        provider.setAdditionalPorts(List.of(28182));
        assertThatThrownBy(() -> runtimeRpcPort(provider)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        provider.setAdditionalPorts(List.of(29310, 29311));
        assertThatThrownBy(() -> runtimeRpcPort(provider)).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "xingyuan.isolated.rpc-profile.enabled", matches = "true")
    void packagedRpcRuntimeModelsMatchTheActualTwelveArtifacts() throws Exception {
        Path repository = Path.of(requiredOperationProperty("xingyuan.isolated.repository-root")).toRealPath();
        Path input = Path.of(requiredOperationProperty("xingyuan.isolated.profile-inventory-file"));
        Path root = input.getParent().getParent();
        if (!root.getParent().equals(repository.resolve("target")) || !root.getFileName().toString().matches("xingyuan-integration-[0-9a-f-]{36}")) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var artifacts = strictMapper().readTree(readProtectedBytes(input, root, 128 * 1024));
        Set<String> roles = new HashSet<>(); artifacts.fieldNames().forEachRemaining(roles::add);
        if (!roles.equals(RUNTIME_PROFILES.keySet())) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
        for (String role : roles) {
            var artifact = strictMapper().treeToValue(artifacts.get(role), ArtifactReferenceDTO.class);
            artifactRuntimeDefaults(artifact, repository, RUNTIME_PROFILES.get(role));
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> requirePackagedRuntimeRpcModels(artifact));
        }
    }

    @Test
    void gatewayListenerEnvironmentMatchesTheOriginalDdcOwnerAndItsBoundedQueryContract() {
        var manifest = intent(); var inputs = VerifiedDatabaseInputsBO.builder().manifest(manifest).manifestSha256("a".repeat(64)).build();
        var registry = runtimeRegistryFixture(manifest, inputs.getManifestSha256());
        registry.setResources(List.of(top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ResourceServerVO.builder()
                .resourceServerId("controlled-gateway").resourceUri("https://127.0.0.1:28180").bizCode("controlled").appCode("controlled-gateway").environment("local")
                .displayName("controlled-gateway").managementClientId("controlled-manager").rbacApplicationCode("controlled-gateway").entryPermissionCode("controlled:entry").status("ACTIVE").build()));
        var facts = runtimeRegistryFacts(inputs, registry);
        for (String role : List.of("biz", "mcp")) {
            var template = gatewayListenerTemplate(role); String engine = role.equals("biz") ? "egon.cola.component.yuheng.engine.env" : "egon.cola.component.yuheng.mcp-engine.env";
            assertThat(template).containsEntry("egon.cola.component.tianshu.env", template.get(engine));
            var actual = renderRuntimeProperties(Map.of(), Map.of(engine, template.get(engine), "egon.cola.component.tianshu.env", template.get("egon.cola.component.tianshu.env")), facts);
            assertThat(actual.get(engine)).isEqualTo(registry.getResources().getFirst().getEnvironment()).isEqualTo("local");
            assertThat(actual.get(engine)).hasSizeLessThanOrEqualTo(32);
        }
        registry.getResources().getFirst().setEnvironment("foreign");
        assertThatThrownBy(() -> runtimeRegistryFacts(inputs, registry)).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    static Map<String, String> gatewayListenerTemplate(String role) {
        if (!Set.of("biz", "mcp").contains(role)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        Map<String, String> values = new java.util.TreeMap<>();
        String process = "${xingyuan.it.process." + role + ".";
        values.put("server.port", process + "management-port}");
        values.put("egon.cola.component.tianshu.env", "${xingyuan.it.environment}");
        String tls;
        if (role.equals("biz")) {
            String engine = "egon.cola.component.yuheng.engine.";
            values.put(engine + "gateway-group-code", "xingyuan-it-${xingyuan.it.run-id}");
            values.put(engine + "env", "${xingyuan.it.environment}"); values.put(engine + "namespace", "xingyuan-it-${xingyuan.it.run-id}");
            values.put(engine + "http.public-enabled", "true"); values.put(engine + "http.public-host", "127.0.0.1");
            values.put(engine + "http.public-port", process + "port}"); values.put(engine + "http.internal-enabled", "false");
            values.put(engine + "rpc.enabled", "false"); tls = engine + "http.public-tls.";
        } else {
            String engine = "egon.cola.component.yuheng.mcp-engine.";
            values.put(engine + "yuheng-group-code", "xingyuan-it-${xingyuan.it.run-id}");
            values.put(engine + "env", "${xingyuan.it.environment}"); values.put(engine + "namespace", "xingyuan-it-${xingyuan.it.run-id}");
            values.put(engine + "management-port", process + "management-port}");
            values.put(engine + "listener.enabled", "true"); values.put(engine + "listener.host", "127.0.0.1");
            values.put(engine + "listener.port", process + "port}"); tls = engine + "listener.tls.";
        }
        values.put(tls + "enabled", "true"); values.put(tls + "development-plaintext", "false");
        values.put(tls + "certificate-chain-path", "${xingyuan.it.secret.tls-server-cert.file}");
        values.put(tls + "private-key-path", "${xingyuan.it.secret.tls-server-key.file}");
        values.put(tls + "trust-certificate-collection-path", "${xingyuan.it.secret.tls-ca-cert.file}");
        values.put(tls + "client-certificate-required", "false");
        return java.util.Collections.unmodifiableMap(values);
    }

    private static void validatePackagedGatewayListeners(String role, ArtifactReferenceDTO artifact, Path repository,
            VerifiedDatabaseInputsBO inputs, Map<String, String> values) throws IOException {
        if (!Set.of("biz", "mcp").contains(role)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var processes = inputs.getManifest().getProcesses().stream().filter(process -> role.equals(process.getId()) && "JAVA".equals(process.getKind())).toList();
        if (processes.size() != 1) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
        var process = processes.getFirst(); int management = runtimeManagementPort(process);
        if (!Integer.toString(management).equals(values.get("server.port")) || !"127.0.0.1".equals(values.get("server.address"))
                || !"true".equals(values.get("server.ssl.enabled"))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        artifactRuntimeDefaults(artifact, repository, "operations");
        boolean biz = role.equals("biz");
        String prefix = biz ? "egon.cola.component.yuheng.engine.http" : "egon.cola.component.yuheng.mcp-engine.listener";
        String className = biz ? "top.egon.cola.component.yuheng.engine.common.config.GatewayEngineRuntimeProperties$Http"
                : "top.egon.cola.component.yuheng.mcp.engine.config.McpGatewayEngineProperties$ListenerProperties";
        var classes = URI.create("jar:" + Path.of(artifact.getPath()).toUri().toASCIIString() + "!/BOOT-INF/classes/").toURL();
        try (var loader = new java.net.URLClassLoader(new java.net.URL[]{classes}, IsolatedOAuthProvisioningIT.class.getClassLoader())) {
            try {
                Class<?> type = Class.forName(className, true, loader);
                if (type.getClassLoader() != loader) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                Object bound = new org.springframework.boot.context.properties.bind.Binder(new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(values))
                        .bind(prefix, org.springframework.boot.context.properties.bind.Bindable.of(type))
                        .orElseThrow(() -> new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"));
                try (var validators = Validation.buildDefaultValidatorFactory()) { new top.egon.cola.component.common.core.validation.ValidationUtils(validators.getValidator()).validate(bound); }
                var properties = new org.springframework.beans.BeanWrapperImpl(bound); String tls = biz ? "publicTls." : "tls.";
                if (!Boolean.TRUE.equals(properties.getPropertyValue(biz ? "publicEnabled" : "enabled"))
                        || !"127.0.0.1".equals(properties.getPropertyValue(biz ? "publicHost" : "host"))
                        || !Integer.valueOf(process.getPort()).equals(properties.getPropertyValue(biz ? "publicPort" : "port"))
                        || biz && (!Boolean.FALSE.equals(properties.getPropertyValue("internalEnabled")) || !"false".equals(values.get("egon.cola.component.yuheng.engine.rpc.enabled")))
                        || !biz && !Integer.toString(management).equals(values.get("egon.cola.component.yuheng.mcp-engine.management-port"))
                        || !Boolean.TRUE.equals(properties.getPropertyValue(tls + "enabled")) || !Boolean.FALSE.equals(properties.getPropertyValue(tls + "developmentPlaintext"))
                        || !Boolean.FALSE.equals(properties.getPropertyValue(tls + "clientCertificateRequired"))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                for (var file : Map.of("certificateChainPath", "tls-server-cert", "privateKeyPath", "tls-server-key", "trustCertificateCollectionPath", "tls-ca-cert").entrySet()) {
                    String expected = inputs.getManifest().getSecretFiles().get(file.getValue());
                    if (expected == null || !expected.equals(properties.getPropertyValue(tls + file.getKey()))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                    byte[] bytes = readProtectedBytes(Path.of(expected), Path.of(inputs.getManifest().getRuntimeRoot()).resolve("secrets"), 64 * 1024);
                    try { if (bytes.length == 0) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); } }
                    finally { java.util.Arrays.fill(bytes, (byte) 0); }
                }
            } catch (ClassNotFoundException invalid) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID", invalid); }
            finally { org.springframework.beans.CachedIntrospectionResults.clearClassLoader(loader); }
        }
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "xingyuan.isolated.gateway-profile.enabled", matches = "true")
    void packagedGatewayListenerModelsBindWithoutStartingApplications() throws Exception {
        Path repository = Path.of(requiredOperationProperty("xingyuan.isolated.repository-root")).toRealPath();
        Path input = Path.of(requiredOperationProperty("xingyuan.isolated.profile-inventory-file"));
        Path inputRoot = input.getParent().getParent();
        if (!inputRoot.getParent().equals(repository.resolve("target")) || !inputRoot.getFileName().toString().matches("xingyuan-integration-[0-9a-f-]{36}")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var artifacts = strictMapper().readTree(readProtectedBytes(input, inputRoot, 128 * 1024));
        var manifest = intent(); Path root = Path.of(manifest.getRuntimeRoot());
        Files.createDirectories(root.resolve("secrets")); Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
        Files.setPosixFilePermissions(root.resolve("secrets"), PosixFilePermissions.fromString("rwx------"));
        Map<String, String> secrets = new java.util.TreeMap<>(); Map<String, String> privateReferences = new java.util.TreeMap<>();
        for (String key : List.of("tls-server-cert", "tls-server-key", "tls-ca-cert")) {
            Path file = root.resolve("secrets/" + key + ".model-only"); Files.writeString(file, "controlled-path-binding-only");
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------")); secrets.put(key, file.toString());
            privateReferences.put("xingyuan.it.secret." + key + ".file", file.toString());
        }
        manifest.setSecretFiles(secrets).setProcesses(List.of(
                ProcessTargetDTO.builder().id("biz").kind("JAVA").port(28180).additionalPorts(List.of(28182)).build(),
                ProcessTargetDTO.builder().id("mcp").kind("JAVA").port(28185).additionalPorts(List.of(28186)).build()));
        var inputs = VerifiedDatabaseInputsBO.builder().manifest(manifest).manifestSha256("a".repeat(64)).build();
        var registry = runtimeRegistryFixture(manifest, inputs.getManifestSha256());
        Map<String, String> facts = new java.util.TreeMap<>(runtimeRegistryFacts(inputs, registry)); facts.putAll(privateReferences);
        for (String role : List.of("biz", "mcp")) {
            var artifact = strictMapper().treeToValue(artifacts.get(role), ArtifactReferenceDTO.class);
            var values = new java.util.TreeMap<>(renderRuntimeProperties(Map.of(), gatewayListenerTemplate(role), facts));
            values.put("server.address", "127.0.0.1"); values.put("server.ssl.enabled", "true");
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> validatePackagedGatewayListeners(role, artifact, repository, inputs, values));
            String prefix = role.equals("biz") ? "egon.cola.component.yuheng.engine.http.public-" : "egon.cola.component.yuheng.mcp-engine.listener.";
            for (var change : Map.of("server.port", role.equals("biz") ? "28180" : "28185", prefix + "port", "18081", prefix + "host", "0.0.0.0",
                    prefix + "tls.development-plaintext", "true", prefix + "tls.private-key-path", root.resolve("secrets/foreign-key").toString()).entrySet()) {
                var wrong = new java.util.TreeMap<>(values); wrong.put(change.getKey(), change.getValue());
                assertThatThrownBy(() -> validatePackagedGatewayListeners(role, artifact, repository, inputs, wrong)).hasMessage("XINGYUAN_TARGET_UNSAFE");
            }
        }
    }

    @Test
    void gatewayManagementTransportUsesTheDeclaredSecondaryPort() {
        var manifest = intent(); var inputs = VerifiedDatabaseInputsBO.builder().manifest(manifest).manifestSha256("a".repeat(64)).build();
        for (var entry : Map.of("biz", List.of(28180, 28182), "mcp", List.of(28185, 28186)).entrySet()) {
            var process = ProcessTargetDTO.builder().id(entry.getKey()).artifactRole(entry.getKey()).kind("JAVA")
                    .port(entry.getValue().getFirst()).additionalPorts(List.of(entry.getValue().getLast())).build();
            Map<String, String> values = new java.util.TreeMap<>();
            applyRuntimeTransport(entry.getKey(), process, inputs, Map.of(), values, "operations");
            assertThat(values).containsEntry("server.port", Integer.toString(entry.getValue().getLast()));
            manifest.setProcesses(List.of(process));
            var facts = runtimeRegistryFacts(inputs, runtimeRegistryFixture(manifest, inputs.getManifestSha256()));
            assertThat(facts).containsEntry("xingyuan.it.process." + entry.getKey() + ".management-port", Integer.toString(entry.getValue().getLast()));
            process.setAdditionalPorts(List.of());
            assertThatThrownBy(() -> runtimeManagementPort(process)).hasMessage("XINGYUAN_TARGET_UNSAFE");
            process.setAdditionalPorts(List.of(process.getPort()));
            assertThatThrownBy(() -> runtimeManagementPort(process)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
        var rpcProvider = ProcessTargetDTO.builder().id("native-web").port(28311).additionalPorts(List.of(29311)).build();
        assertThat(runtimeManagementPort(rpcProvider)).isEqualTo(28311);
    }

    static Map<String, String> runtimeAuditTemplate(String role) {
        if (!RUNTIME_PROFILES.containsKey(role)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        String receiver = switch (role) {
            case "shoubing" -> "SHOUBING"; case "jianshen" -> "JIANSHEN"; case "tianshu" -> "TIANSHU";
            case "yuheng" -> "YUHENG_ADMIN"; case "biz" -> "BIZ"; case "mcp" -> "MCP"; case "llm" -> "LLM"; case "edge" -> "EDGE";
            default -> "APPLICATION";
        };
        String prefix = "egon.cola.platform.tianquan.shoubing.";
        Map<String, String> values = new java.util.TreeMap<>();
        values.put(prefix + "audit.stream", "xingyuan-it:${xingyuan.it.run-id}:oauth:audit:v1");
        values.put(prefix + "audit.spool-directory", "${xingyuan.it.runtime-root}/evidence/oauth-spool-" + role);
        values.put(prefix + "audit.spool-max-bytes", "104857600"); values.put(prefix + "audit.hmac-key-file", "${xingyuan.it.secret.audit-hmac.file}");
        values.put(prefix + "audit.delivery.receipt-ttl", "1h"); values.put(prefix + "audit.delivery.confirmation-timeout", "30s");
        values.put(prefix + "audit.delivery.batch-size", "100");
        values.put(prefix + "usage-observation.enabled", "true"); values.put(prefix + "usage-observation.receiver", receiver);
        values.put(prefix + "usage-observation.fallback-issuer", "${xingyuan.it.realm.default.issuer}"); values.put(prefix + "usage-observation.allow-local-plaintext", "true");
        values.put(prefix + "revocation.mode", "STRICT_SHARED"); values.put(prefix + "revocation.timeout", "500ms");
        values.put(prefix + "revocation.primary-endpoint", "${xingyuan.it.redis.endpoint}"); values.put(prefix + "revocation.epoch", "${xingyuan.it.run-id}");
        return java.util.Collections.unmodifiableMap(values);
    }

    private static void validateRuntimeAudit(String role, VerifiedDatabaseInputsBO inputs, IsolatedRuntimeRegistryDTO registry,
            Map<String, String> values) throws IOException {
        var binder = new org.springframework.boot.context.properties.bind.Binder(
                new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(values));
        String prefix = "egon.cola.platform.tianquan.shoubing.";
        var audit = binder.bind(prefix + "audit", top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties.class)
                .orElseThrow(() -> new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"));
        var usage = binder.bind(prefix + "usage-observation", top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties.class)
                .orElseThrow(() -> new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"));
        var delivery = binder.bind(prefix + "audit.delivery", top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties.class)
                .orElseThrow(() -> new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"));
        var revocation = binder.bind(prefix + "revocation", top.egon.cola.platform.tianquan.shoubing.admin.token.config.OAuthRevocationProperties.class)
                .orElseThrow(() -> new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"));
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var validation = new top.egon.cola.component.common.core.validation.ValidationUtils(validators.getValidator());
            validation.validate(audit); validation.validate(usage); validation.validate(delivery); validation.validate(revocation);
        }
        revocation.validateValues();
        var manifest = inputs.getManifest(); var facts = runtimeRegistryFacts(inputs, registry);
        Path root = Path.of(manifest.getRuntimeRoot()); Path spool = root.resolve("evidence/oauth-spool-" + role);
        String keyReference = manifest.getSecretFiles().get("audit-hmac");
        if (keyReference == null) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
        if (!audit.stream().equals("xingyuan-it:" + manifest.getRunId() + ":oauth:audit:v1") || !spool.equals(audit.spoolDirectory())
                || audit.spoolMaxBytes() > 104857600L || !Path.of(keyReference).equals(audit.hmacKeyFile())
                || !usage.isEnabled() || !usage.isAllowLocalPlaintext() || usage.getReceiver() == null
                || !usage.getReceiver().name().equals(runtimeAuditTemplate(role).get(prefix + "usage-observation.receiver"))
                || !facts.get("xingyuan.it.realm.default.issuer").equals(usage.getFallbackIssuer().toASCIIString())
                || !manifest.getRedis().getEndpoint().equals(revocation.getPrimaryEndpoint().toASCIIString())
                || !manifest.getRunId().equals(revocation.getEpoch()) || values.containsKey(prefix + "audit.stream-key")) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        if (Files.exists(spool, LinkOption.NOFOLLOW_LINKS)) { validatePrivateSecretDestination(spool.resolve(".profile-spool-check"), root); }
        else { validatePrivateSecretDestination(spool, root); }
        byte[] key = readProtectedBytes(audit.hmacKeyFile(), root.resolve("secrets"), 32);
        try { if (key.length != 32) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); } }
        finally { java.util.Arrays.fill(key, (byte) 0); }
    }

    @Test
    void runtimeAuditFragmentsRejectCrossTaskAndCrossRoleDestinations() throws Exception {
        var manifest = intent(); Path root = Path.of(manifest.getRuntimeRoot());
        for (String directory : List.of("", "secrets", "evidence")) {
            Files.createDirectories(root.resolve(directory)); Files.setPosixFilePermissions(root.resolve(directory), PosixFilePermissions.fromString("rwx------"));
        }
        Path key = root.resolve("secrets/audit-hmac.bin"); Files.write(key, new byte[32]); Files.setPosixFilePermissions(key, PosixFilePermissions.fromString("rw-------"));
        manifest.setSecretFiles(Map.of("audit-hmac", key.toString()));
        var inputs = VerifiedDatabaseInputsBO.builder().manifest(manifest).manifestSha256("a".repeat(64)).build();
        var registry = runtimeRegistryFixture(manifest, inputs.getManifestSha256());
        Map<String, String> facts = new java.util.TreeMap<>(runtimeRegistryFacts(inputs, registry));
        facts.put("xingyuan.it.secret.audit-hmac.file", key.toString()); facts.put("xingyuan.it.redis.endpoint", manifest.getRedis().getEndpoint());
        for (String role : RUNTIME_PROFILES.keySet()) {
            var rendered = renderRuntimeProperties(Map.of(), runtimeAuditTemplate(role), facts);
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> validateRuntimeAudit(role, inputs, registry, rendered));
            assertThat(Files.exists(root.resolve("evidence/oauth-spool-" + role))).isFalse();
        }
        var values = renderRuntimeProperties(Map.of(), runtimeAuditTemplate("edge"), facts);
        String prefix = "egon.cola.platform.tianquan.shoubing.";
        for (var change : Map.ofEntries(
                Map.entry(prefix + "audit.spool-directory", root.resolve("evidence/oauth-spool-jianshen").toString()),
                Map.entry(prefix + "audit.stream", "egon:oauth:audit:v1"), Map.entry(prefix + "audit.stream-key", "egon:oauth:audit:v1"),
                Map.entry(prefix + "audit.hmac-key-file", root.resolve("secrets/foreign-key.bin").toString()),
                Map.entry(prefix + "audit.spool-max-bytes", "1073741824"), Map.entry(prefix + "usage-observation.enabled", "false"),
                Map.entry(prefix + "usage-observation.receiver", "JIANSHEN"), Map.entry(prefix + "usage-observation.allow-local-plaintext", "false"),
                Map.entry(prefix + "usage-observation.fallback-issuer", facts.get("xingyuan.it.realm.tenant-b.issuer")),
                Map.entry(prefix + "revocation.epoch", "old-epoch"), Map.entry(prefix + "revocation.primary-endpoint", "redis://127.0.0.1:6380")).entrySet()) {
            var foreign = new java.util.TreeMap<>(values); foreign.put(change.getKey(), change.getValue());
            assertThatThrownBy(() -> validateRuntimeAudit("edge", inputs, registry, foreign)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
        var invalidDelivery = new java.util.TreeMap<>(values); invalidDelivery.put(prefix + "audit.delivery.confirmation-timeout", "1s");
        assertThatThrownBy(() -> validateRuntimeAudit("edge", inputs, registry, invalidDelivery)).isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        assertThatThrownBy(() -> runtimeAuditTemplate("foreign")).hasMessage("XINGYUAN_TARGET_UNSAFE");
        Path spool = root.resolve("evidence/oauth-spool-edge"); Files.createDirectory(spool, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Path retained = spool.resolve("retained.evt"); Files.writeString(retained, "original-audit-payload");
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> validateRuntimeAudit("edge", inputs, registry, values));
        assertThat(Files.readString(retained)).isEqualTo("original-audit-payload");
        Files.setPosixFilePermissions(key, PosixFilePermissions.fromString("rw-r-----"));
        try { assertThatThrownBy(() -> validateRuntimeAudit("edge", inputs, registry, values)).hasMessage("XINGYUAN_TARGET_UNSAFE"); }
        finally { Files.setPosixFilePermissions(key, PosixFilePermissions.fromString("rw-------")); }
        Files.write(key, new byte[31]);
        try { assertThatThrownBy(() -> validateRuntimeAudit("edge", inputs, registry, values)).hasMessage("XINGYUAN_TARGET_UNSAFE"); }
        finally { Files.write(key, new byte[32]); }
        Path moved = root.resolve("evidence/preserved-edge-spool"); Files.move(spool, moved); Files.createSymbolicLink(spool, moved);
        try { assertThatThrownBy(() -> validateRuntimeAudit("edge", inputs, registry, values)).hasMessage("XINGYUAN_TARGET_UNSAFE"); }
        finally { Files.delete(spool); Files.move(moved, spool); }
        var missing = new java.util.TreeMap<>(facts); missing.remove("xingyuan.it.secret.audit-hmac.file");
        assertThatThrownBy(() -> renderRuntimeProperties(Map.of(), runtimeAuditTemplate("edge"), missing)).hasMessage("XINGYUAN_INPUT_UNAVAILABLE");
    }

    static Map<String, String> runtimeTrustTemplate() {
        Map<String, String> values = new java.util.TreeMap<>();
        for (String code : List.of("platform", "default", "tenant-b")) {
            String output = "egon.cola.platform.tianquan.shoubing.trusted-issuers." + code + ".";
            String fact = "${xingyuan.it.realm." + code + ".";
            for (String field : List.of("issuer", "realm-key", "realm-type", "trust-version")) {
                values.put(output + field, fact + field + "}");
            }
            values.put(output + "jwks-uri", fact + "jwk-set-uri}"); values.put(output + "status", "ACTIVE");
            if (!code.equals("platform")) { values.put(output + "tenant-id", fact + "tenant-id}"); }
        }
        return java.util.Collections.unmodifiableMap(values);
    }

    private static void validateRuntimeTrust(VerifiedDatabaseInputsBO inputs, IsolatedRuntimeRegistryDTO registry,
            Map<String, String> values) {
        var properties = new org.springframework.boot.context.properties.bind.Binder(
                new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(values))
                .bind("egon.cola.platform.tianquan.shoubing",
                        top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.class)
                .orElseThrow(() -> new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"));
        Map<String, String> facts = runtimeRegistryFacts(inputs, registry);
        if (!properties.getTrustedIssuers().keySet().equals(Set.of("platform", "default", "tenant-b"))
                || properties.getIssuer() != null || properties.getJwkSetUri() != null) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        properties.getTrustedIssuers().forEach((code, entry) -> {
            entry.validate(code);
            String fact = "xingyuan.it.realm." + code + ".";
            String tenant = code.equals("platform") ? null : facts.get(fact + "tenant-id");
            if (!facts.get(fact + "issuer").equals(entry.getIssuer()) || !facts.get(fact + "jwk-set-uri").equals(entry.getJwksUri())
                    || !facts.get(fact + "realm-key").equals(entry.getRealmKey()) || !facts.get(fact + "realm-type").equals(entry.getRealmType())
                    || !Objects.equals(tenant, entry.getTenantId()) || !"ACTIVE".equals(entry.getStatus())
                    || Long.parseLong(facts.get(fact + "trust-version")) != entry.getTrustVersion()) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
        });
    }

    @Test
    void runtimeTrustFragmentBindsOriginalSdkAndRejectsForeignRealmMetadata() {
        var manifest = intent(); var registry = runtimeRegistryFixture(manifest, "a".repeat(64));
        registry.getRealms().get("default").setTrustVersion(7L);
        registry.getRealms().get("tenant-b").setTrustVersion(11L);
        var inputs = VerifiedDatabaseInputsBO.builder().manifest(manifest).manifestSha256("a".repeat(64)).build();
        var facts = runtimeRegistryFacts(inputs, registry);
        var rendered = renderRuntimeProperties(Map.of(), runtimeTrustTemplate(), facts);
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> validateRuntimeTrust(inputs, registry, rendered));
        String prefix = "egon.cola.platform.tianquan.shoubing.trusted-issuers.";
        assertThat(rendered).containsEntry(prefix + "default.trust-version", "7")
                .containsEntry(prefix + "tenant-b.trust-version", "11")
                .containsEntry(prefix + "default.tenant-id", registry.getTenantIds().get("default"))
                .doesNotContainKey(prefix + "platform.tenant-id");
        for (var change : Map.ofEntries(
                Map.entry(prefix + "default.tenant-id", registry.getTenantIds().get("tenant-b")),
                Map.entry(prefix + "default.issuer", registry.getRealms().get("tenant-b").getIssuer().toASCIIString()),
                Map.entry(prefix + "default.jwks-uri", facts.get("xingyuan.it.realm.tenant-b.jwk-set-uri")),
                Map.entry(prefix + "default.realm-key", "tenant:" + registry.getTenantIds().get("tenant-b")),
                Map.entry(prefix + "default.trust-version", "8"), Map.entry(prefix + "default.status", "INACTIVE"),
                Map.entry("egon.cola.platform.tianquan.shoubing.issuer", facts.get("xingyuan.it.realm.default.issuer")),
                Map.entry("egon.cola.platform.tianquan.shoubing.jwk-set-uri", facts.get("xingyuan.it.realm.default.jwk-set-uri"))).entrySet()) {
            var foreign = new java.util.TreeMap<>(rendered); foreign.put(change.getKey(), change.getValue());
            assertThatThrownBy(() -> validateRuntimeTrust(inputs, registry, foreign)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
        var extra = new java.util.TreeMap<>(rendered); extra.put(prefix + "old.issuer", facts.get("xingyuan.it.realm.default.issuer"));
        assertThatThrownBy(() -> validateRuntimeTrust(inputs, registry, extra)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        var missing = new java.util.TreeMap<>(rendered); missing.keySet().removeIf(key -> key.startsWith(prefix + "tenant-b."));
        assertThatThrownBy(() -> validateRuntimeTrust(inputs, registry, missing)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        var platformTenant = new java.util.TreeMap<>(rendered); platformTenant.put(prefix + "platform.tenant-id", registry.getTenantIds().get("default"));
        assertThatThrownBy(() -> validateRuntimeTrust(inputs, registry, platformTenant)).isInstanceOf(IllegalStateException.class);
        var incompleteFacts = new java.util.TreeMap<>(facts); incompleteFacts.remove("xingyuan.it.realm.default.trust-version");
        assertThatThrownBy(() -> renderRuntimeProperties(Map.of(), runtimeTrustTemplate(), incompleteFacts)).hasMessage("XINGYUAN_INPUT_UNAVAILABLE");
        Map<String, Object> defaults = new java.util.TreeMap<>(rendered);
        defaults.put("egon.cola.platform.tianquan.shoubing.issuer", "https://old.invalid");
        defaults.put("egon.cola.platform.tianquan.shoubing.jwk-set-uri", "https://old.invalid/jwks");
        defaults.put("business.route", "retained"); clearRuntimeIdentityDefaults(defaults);
        assertThat(defaults).containsOnlyKeys("business.route");
    }

    static Map<String, String> edgeRouteTemplate(Map<String, Object> defaults) {
        String routes = "spring.cloud.gateway.server.webflux.routes";
        Map<Integer, Map<String, String>> original = new java.util.TreeMap<>();
        var pattern = java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(routes) + "\\[([0-9]{1,2})\\]\\.(.+)");
        for (var entry : defaults.entrySet()) {
            if (!entry.getKey().startsWith(routes)) { continue; }
            var match = pattern.matcher(entry.getKey());
            if (!match.matches() || entry.getValue() == null) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
            original.computeIfAbsent(Integer.parseInt(match.group(1)), ignored -> new java.util.TreeMap<>()).put(match.group(2), entry.getValue().toString());
        }
        Map<String, String> roles = Map.of("edge-shoubing", "shoubing", "edge-jianshen", "jianshen", "edge-yuheng", "yuheng", "edge-tianshu", "tianshu");
        Set<String> ids = new HashSet<>(); Set<String> aliases = new HashSet<>(); Set<String> selectedIds = new HashSet<>();
        Set<String> declaredIds = new HashSet<>();
        for (String alias : roles.keySet()) {
            String resource = "egon.yuheng.edge.resources." + alias + ".route-ids";
            defaults.forEach((key, value) -> {
                if (key.equals(resource) || key.startsWith(resource + "[")) {
                    if (value == null || !declaredIds.add(value.toString())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                }
            });
        }
        Map<String, String> result = new java.util.TreeMap<>(); int inputIndex = 0; int outputIndex = 0; boolean authorizationServer = false;
        for (var entry : original.entrySet()) {
            var route = entry.getValue(); String id = route.get("id"); String alias = route.get("metadata.edge-registration-id");
            if (entry.getKey() != inputIndex++ || id == null || !id.matches("[A-Za-z0-9._-]{1,128}") || !ids.add(id)) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
            }
            if (alias == null) {
                if (!id.equals("authorization-server") || !"${egon.yuheng.edge.upstreams.authorization-server}".equals(route.get("uri"))
                        || !"Path=/tenants/**,/platform/**".equals(route.get("predicates[0]")) || !route.keySet().equals(Set.of("id", "uri", "predicates[0]"))) {
                    throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                }
                authorizationServer = true;
            } else {
                if (!roles.containsKey(alias)) { continue; }
                String resource = "egon.yuheng.edge.resources." + alias + ".route-ids";
                if (defaults.entrySet().stream().noneMatch(value -> (value.getKey().equals(resource) || value.getKey().startsWith(resource + "[")) && id.equals(value.getValue()))
                        || !route.keySet().equals(Set.of("id", "uri", "metadata.edge-registration-id", "predicates[0]", "filters[0]"))
                        || !Objects.equals(route.get("filters[0]"), "EdgeResourceAuthorization=" + alias)
                        || !route.get("predicates[0]").startsWith("Path=")) {
                    throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                }
                aliases.add(alias); selectedIds.add(id);
            }
            String output = routes + "[" + outputIndex++ + "].";
            route.forEach((key, value) -> result.put(output + key, value));
            result.put(output + "uri", "${xingyuan.it.process." + (alias == null ? "shoubing" : roles.get(alias)) + ".https-origin}");
        }
        if (!authorizationServer || !aliases.equals(roles.keySet()) || !selectedIds.equals(declaredIds)) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
        return java.util.Collections.unmodifiableMap(result);
    }

    private static void validateRuntimeRoutes(Map<String, String> expected, Map<String, String> values) {
        Map<String, String> actual = new java.util.TreeMap<>();
        values.forEach((key, value) -> { if (key.startsWith("spring.cloud.gateway.server.webflux.routes")) { actual.put(key, value); } });
        if (!expected.equals(actual)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
    }

    @Test
    void edgeRouteFragmentKeepsDeclaredAuthorizationAndUsesManifestTransport() {
        Map<String, Object> defaults = new java.util.TreeMap<>();
        String routes = "spring.cloud.gateway.server.webflux.routes";
        defaults.put(routes + "[0].id", "authorization-server"); defaults.put(routes + "[0].uri", "${egon.yuheng.edge.upstreams.authorization-server}");
        defaults.put(routes + "[0].predicates[0]", "Path=/tenants/**,/platform/**");
        int index = 1;
        for (String alias : List.of("edge-shoubing", "edge-jianshen", "edge-yuheng", "edge-tianshu", "edge-biz")) {
            String route = routes + "[" + index++ + "].";
            defaults.put(route + "id", "route-" + alias); defaults.put(route + "metadata.edge-registration-id", alias);
            defaults.put(route + "uri", "${egon.yuheng.edge.resources." + alias + ".resource-uri}");
            defaults.put(route + "predicates[0]", "Path=/controlled/" + alias + "/**");
            defaults.put(route + "filters[0]", "EdgeResourceAuthorization=" + alias);
            defaults.put("egon.yuheng.edge.resources." + alias + ".route-ids[0]", "route-" + alias);
        }
        var template = edgeRouteTemplate(defaults);
        assertThat(template).containsEntry(routes + "[1].uri", "${xingyuan.it.process.shoubing.https-origin}");
        assertThat(template.values()).doesNotContain("route-edge-biz");
        assertThat(template).containsEntry(routes + "[0].predicates[0]", "Path=/tenants/**,/platform/**")
                .containsEntry(routes + "[1].filters[0]", "EdgeResourceAuthorization=edge-shoubing");
        var facts = Map.of("xingyuan.it.process.shoubing.https-origin", "https://127.0.0.1:28120",
                "xingyuan.it.process.jianshen.https-origin", "https://127.0.0.1:28130", "xingyuan.it.process.yuheng.https-origin", "https://127.0.0.1:28140",
                "xingyuan.it.process.tianshu.https-origin", "https://127.0.0.1:28150");
        var rendered = renderRuntimeProperties(Map.of(), template, facts);
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> validateRuntimeRoutes(rendered, rendered));
        for (var change : Map.of(routes + "[1].uri", "https://127.0.0.1:18120", routes + "[1].filters[0]", "TokenRelay",
                routes + "[1].predicates[0]", "Path=/**", routes + "[5].id", "unexpected").entrySet()) {
            var wrong = new java.util.TreeMap<>(rendered); wrong.put(change.getKey(), change.getValue());
            assertThatThrownBy(() -> validateRuntimeRoutes(rendered, wrong)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
        var missing = new java.util.TreeMap<>(rendered); missing.remove(routes + "[1].filters[0]");
        assertThatThrownBy(() -> validateRuntimeRoutes(rendered, missing)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        var noAlias = new java.util.TreeMap<>(defaults); noAlias.remove(routes + "[1].metadata.edge-registration-id");
        assertThatThrownBy(() -> edgeRouteTemplate(noAlias)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        var wrongFilter = new java.util.TreeMap<>(defaults); wrongFilter.put(routes + "[1].filters[0]", "TokenRelay");
        assertThatThrownBy(() -> edgeRouteTemplate(wrongFilter)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        var omittedRoute = new java.util.TreeMap<>(defaults); omittedRoute.put("egon.yuheng.edge.resources.edge-jianshen.route-ids[1]", "missing-about");
        assertThatThrownBy(() -> edgeRouteTemplate(omittedRoute)).hasMessage("XINGYUAN_INPUT_UNAVAILABLE");
    }

    static Map<String, String> edgeHostResourceTemplate(Map<String, Object> defaults) {
        Map<String, String> values = new java.util.TreeMap<>();
        Set<String> returnPaths = new java.util.TreeSet<>();
        String edge = "egon.yuheng.edge.";
        for (String alias : List.of("edge-ui", "edge-shoubing", "edge-jianshen", "edge-yuheng", "edge-tianshu")) {
            String original = edge + "resources." + alias + ".";
            String output = edge + "resources." + alias + ".";
            Object returnPath = defaults.get(original + "return-path");
            if (returnPath == null) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
            values.put(output + "return-path", returnPath.toString()); returnPaths.add(returnPath.toString());
            for (var entry : defaults.entrySet()) {
                if (entry.getKey().startsWith(original + "request-paths[") || entry.getKey().equals(original + "request-paths")
                        || entry.getKey().startsWith(original + "route-ids[") || entry.getKey().equals(original + "route-ids")) {
                    if (entry.getValue() == null) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
                    values.put(output + entry.getKey().substring(original.length()), entry.getValue().toString());
                }
            }
            String client = "${xingyuan.it.client.${xingyuan.it.alias." + alias + ".default.registration-id}.";
            values.put(output + "resource-server-id", client + "resource-server-id}"); values.put(output + "resource-uri", client + "resource-uri}");
            values.put(output + "scopes", client + "scopes}");
            values.put(output + "default-registration-id", "${xingyuan.it.alias." + alias + ".default-registration-id}");
            for (String tenant : List.of("default", "tenant-b")) {
                values.put(output + "issuer-registration-ids.[${xingyuan.it.realm." + tenant + ".issuer}]",
                        "${xingyuan.it.alias." + alias + "." + tenant + ".registration-id}");
            }
        }
        for (var entry : defaults.entrySet()) {
            if (entry.getKey().equals(edge + "allowed-return-paths") || entry.getKey().startsWith(edge + "allowed-return-paths[")) {
                if (entry.getValue() == null) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
                values.put(entry.getKey(), entry.getValue().toString());
            }
        }
        if (values.keySet().stream().noneMatch(key -> key.startsWith(edge + "allowed-return-paths"))) {
            int index = 0; for (String path : returnPaths) { values.put(edge + "allowed-return-paths[" + index++ + "]", path); }
        }
        values.put(edge + "public-origin", "${xingyuan.it.public-origin}"); values.put(edge + "default-registration-id", "edge-ui");
        values.put(edge + "upstreams.authorization-server", "${xingyuan.it.process.shoubing.https-origin}");
        values.put(edge + "upstreams.biz-gateway", "${xingyuan.it.process.biz.https-origin}"); values.put(edge + "upstreams.mcp-gateway", "${xingyuan.it.process.mcp.https-origin}");
        values.put(edge + "session.encryption.key-id", "task-${xingyuan.it.run-id}");
        values.put(edge + "session.encryption.key-file", "${xingyuan.it.secret.edge-session.file}");
        values.put(edge + "logout.stream", "xingyuan-it:${xingyuan.it.run-id}:bff:logout:v1");
        values.put(edge + "logout.consumer-group", "xingyuan-it:${xingyuan.it.run-id}:bff:revocation:v1");
        values.put("spring.session.redis.namespace", "xingyuan-it:${xingyuan.it.run-id}:bff:session:v3");
        return java.util.Collections.unmodifiableMap(values);
    }

    @Test
    void edgeHostFragmentRebuildsIdentitiesAndKeepsOnlyDeclaredBusinessMetadata() throws Exception {
        var manifest = intent(); Path root = Path.of(manifest.getRuntimeRoot());
        Files.createDirectories(root.resolve("secrets")); Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
        Files.setPosixFilePermissions(root.resolve("secrets"), PosixFilePermissions.fromString("rwx------"));
        Path key = root.resolve("secrets/edge-session.bin"); Files.write(key, new byte[32]); Files.setPosixFilePermissions(key, PosixFilePermissions.fromString("rw-------"));
        manifest.setSecretFiles(Map.of("edge-session", key.toString())).setProcesses(List.of(
                ProcessTargetDTO.builder().id("edge").kind("JAVA").port(28200).build(), ProcessTargetDTO.builder().id("shoubing").kind("JAVA").port(28120).build(),
                ProcessTargetDTO.builder().id("biz").kind("JAVA").port(28180).build(), ProcessTargetDTO.builder().id("mcp").kind("JAVA").port(28185).build()));
        var inputs = VerifiedDatabaseInputsBO.builder().manifest(manifest).manifestSha256("a".repeat(64)).build();
        var registry = runtimeRegistryFixture(manifest, inputs.getManifestSha256());
        Map<String, Object> defaults = new java.util.TreeMap<>();
        for (String alias : List.of("edge-ui", "edge-shoubing", "edge-jianshen", "edge-yuheng", "edge-tianshu")) {
            String prefix = "egon.yuheng.edge.resources." + alias + ".";
            defaults.put(prefix + "return-path", "/controlled/" + alias); defaults.put(prefix + "request-paths[0]", "/controlled/" + alias + "/**");
            defaults.put(prefix + "route-ids[0]", "route-" + alias); defaults.put(prefix + "resource-uri", "https://old.invalid");
            defaults.put(prefix + "issuer-registration-ids.[https://old.invalid]", "old-client");
        }
        packagedEdgeProfileFixture(inputs, registry, defaults);
        defaults.put("spring.cloud.gateway.server.webflux.routes[0].uri", "${egon.yuheng.edge.resources.edge-ui.resource-uri}");
        var template = edgeHostResourceTemplate(defaults);
        Map<String, Object> inherited = new java.util.TreeMap<>(defaults); clearRuntimeIdentityDefaults(inherited);
        Map<String, String> facts = new java.util.TreeMap<>(runtimeRegistryFacts(inputs, registry));
        facts.put("xingyuan.it.secret.edge-session.file", key.toString());
        var rendered = org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> renderRuntimeProperties(inherited, template, facts));
        assertThat(rendered.get("egon.yuheng.edge.resources.edge-ui.resource-server-id")).isEqualTo("controlled-edge-ui");
        assertThat(rendered.get("egon.yuheng.edge.resources.edge-ui.resource-uri")).isEqualTo("https://resource.invalid/edge-ui");
        assertThat(rendered.get("egon.yuheng.edge.resources.edge-ui.request-paths[0]")).isEqualTo("/controlled/edge-ui/**");
        assertThat(rendered.get("egon.yuheng.edge.upstreams.biz-gateway")).isEqualTo("https://127.0.0.1:28180");
        assertThat(rendered.get("spring.cloud.gateway.server.webflux.routes[0].uri")).isEqualTo("https://resource.invalid/edge-ui");
        assertThat(rendered.toString()).doesNotContain("old.invalid", "old-client");
        facts.remove("xingyuan.it.client.actual-0.resource-server-id");
        assertThatThrownBy(() -> renderRuntimeProperties(Map.of(), edgeHostResourceTemplate(defaults), facts)).hasMessage("XINGYUAN_INPUT_UNAVAILABLE");
    }

    static Map<String, String> edgeBffUserClientTemplate() {
        Map<String, String> values = new java.util.TreeMap<>();
        for (String tenant : List.of("default", "tenant-b")) {
            String provider = "task-" + tenant;
            String source = "${xingyuan.it.realm." + tenant + ".";
            String target = "spring.security.oauth2.client.provider." + provider + ".";
            for (String field : List.of("token-uri", "authorization-uri", "jwk-set-uri", "user-info-uri")) { values.put(target + field, source + field + "}"); }
            values.put(target + "issuer-uri", source + "issuer}"); values.put(target + "user-name-attribute", "sub");
            for (String alias : List.of("edge-ui", "edge-shoubing", "edge-jianshen", "edge-yuheng", "edge-tianshu")) {
                String registration = "${xingyuan.it.alias." + alias + "." + tenant + ".registration-id}";
                String prefix = "spring.security.oauth2.client.registration.[" + registration + "].";
                String client = "${xingyuan.it.client." + registration + ".";
                values.put(prefix + "provider", provider); values.put(prefix + "client-authentication-method", "client_secret_basic");
                values.put(prefix + "authorization-grant-type", "authorization_code");
                values.put(prefix + "client-id", client + "client-id}"); values.put(prefix + "client-secret", client + "client-secret}");
                values.put(prefix + "scope", client + "scopes}");
                values.put(prefix + "redirect-uri", "${xingyuan.it.public-origin}/login/oauth2/code/{registrationId}");
            }
        }
        return java.util.Collections.unmodifiableMap(values);
    }

    @Test
    void bffTemplateSelectorsUseActualMetadataAndKeepSecretExpressionsOpaque() throws Exception {
        String secret = " private ${must-stay-literal}";
        var rendered = org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> renderRuntimeProperties(Map.of(), Map.of(
                "selected-registration", "${xingyuan.it.alias.edge-ui.default.registration-id}",
                "selected-client", "${xingyuan.it.client.${selected-registration}.client-id}",
                "selected-secret", "${xingyuan.it.client.${selected-registration}.client-secret}"), Map.of(
                "xingyuan.it.alias.edge-ui.default.registration-id", "actual-registration-a",
                "xingyuan.it.client.actual-registration-a.client-id", "actual-client-a",
                "xingyuan.it.client.actual-registration-a.client-secret", secret)));
        assertThat(rendered.get("selected-client")).isEqualTo("actual-client-a");
        var properties = new java.util.Properties(); properties.load(new java.io.ByteArrayInputStream(encodeRuntimeProperties(rendered)));
        var sources = new org.springframework.core.env.MutablePropertySources();
        sources.addFirst(new org.springframework.core.env.PropertiesPropertySource("actual-selected-facts", properties));
        assertThat(new org.springframework.core.env.PropertySourcesPropertyResolver(sources).getProperty("selected-secret")).isEqualTo(secret);
        assertThatThrownBy(() -> renderRuntimeProperties(Map.of(), Map.of("unsafe", "${xingyuan.it.client.${xingyuan.it.client.actual.client-secret}.client-id}"),
                Map.of("xingyuan.it.client.actual.client-secret", "safe-looking-secret", "xingyuan.it.client.safe-looking-secret.client-id", "must-not-select")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void edgeUserClientFragmentBindsTenDistinctActualRegistrations() throws Exception {
        Map<String, String> facts = new java.util.TreeMap<>();
        facts.put("xingyuan.it.public-origin", "https://127.0.0.1:28443");
        Set<String> clients = new HashSet<>(); Map<String, String> secrets = new java.util.TreeMap<>();
        Map<String, String> actualClients = new java.util.TreeMap<>(); Map<String, String> providers = new java.util.TreeMap<>();
        int index = 0;
        for (String tenant : List.of("default", "tenant-b")) {
            String issuer = "https://127.0.0.1:28443/tenants/" + (tenant.equals("default") ? "1001" : "1002");
            String realm = "xingyuan.it.realm." + tenant + ".";
            facts.put(realm + "issuer", issuer); facts.put(realm + "token-uri", issuer + "/oauth2/token");
            facts.put(realm + "authorization-uri", issuer + "/oauth2/authorize"); facts.put(realm + "jwk-set-uri", issuer + "/.well-known/jwks.json");
            facts.put(realm + "user-info-uri", issuer + "/userinfo");
            for (String alias : List.of("edge-ui", "edge-shoubing", "edge-jianshen", "edge-yuheng", "edge-tianshu")) {
                String registration = "owned-registration-" + index; String client = "actual-client-" + index;
                String secret = " private-" + index + " ${literal-" + index + "}";
                facts.put("xingyuan.it.alias." + alias + "." + tenant + ".registration-id", registration);
                facts.put("xingyuan.it.client." + registration + ".client-id", client);
                facts.put("xingyuan.it.client." + registration + ".client-secret", secret);
                facts.put("xingyuan.it.client." + registration + ".scopes", "openid,profile");
                clients.add(client); secrets.put(registration, secret); actualClients.put(registration, client);
                providers.put(registration, "task-" + tenant); index++;
            }
        }
        var template = readRuntimeTemplate(encodeRuntimeProperties(edgeBffUserClientTemplate()));
        var rendered = renderRuntimeProperties(Map.of(), template, facts);
        var properties = new java.util.Properties(); properties.load(new java.io.ByteArrayInputStream(encodeRuntimeProperties(rendered)));
        var environment = new org.springframework.core.env.StandardEnvironment();
        environment.getPropertySources().remove(org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(org.springframework.core.env.StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addFirst(new org.springframework.core.env.PropertiesPropertySource("rendered-bff-fragment", properties));
        var bound = org.springframework.boot.context.properties.bind.Binder.get(environment).bind("spring.security.oauth2.client",
                org.springframework.boot.context.properties.bind.Bindable.of(org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.class))
                .orElseThrow(() -> new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"));
        assertThat(bound.getRegistration()).hasSize(10); assertThat(bound.getProvider()).hasSize(2);
        assertThat(bound.getRegistration().values().stream().map(value -> value.getClientId()).collect(java.util.stream.Collectors.toSet())).isEqualTo(clients);
        for (var registration : bound.getRegistration().entrySet()) {
            assertThat(registration.getValue().getClientId()).isEqualTo(actualClients.get(registration.getKey()));
            assertThat(registration.getValue().getProvider()).isEqualTo(providers.get(registration.getKey()));
            assertThat(registration.getValue().getClientSecret()).isEqualTo(secrets.get(registration.getKey()));
            assertThat(registration.getValue().getScope()).containsExactlyInAnyOrder("openid", "profile");
            assertThat(registration.getValue().getAuthorizationGrantType()).isEqualTo("authorization_code");
            assertThat(registration.getValue().getClientAuthenticationMethod()).isEqualTo("client_secret_basic");
            assertThat(registration.getValue().getRedirectUri()).isEqualTo("https://127.0.0.1:28443/login/oauth2/code/{registrationId}");
        }
        facts.remove("xingyuan.it.client.owned-registration-0.client-id");
        assertThatThrownBy(() -> renderRuntimeProperties(Map.of(), template, facts)).hasMessage("XINGYUAN_INPUT_UNAVAILABLE");
    }

    @Test
    void rebuildingRuntimeIdentityMapsDropsBuiltinAliasesAndKeepsBusinessRoutes() {
        Map<String, Object> defaults = new java.util.LinkedHashMap<>(Map.of(
                "egon.yuheng.edge.resources.edge-ui.issuer-registration-ids.[https://old.invalid/tenants/1]", "old-client",
                "egon.yuheng.edge.resource-bindings.edge-ui.default-registration-id", "old-binding",
                "spring.security.oauth2.client.registration.old.client-id", "old-client",
                "spring.security.oauth2.client.provider.old.issuer-uri", "https://old.invalid/tenants/1",
                "egon.cola.platform.tianquan.shoubing.trusted-issuers.old.issuer", "https://old.invalid/tenants/1",
                "spring.cloud.gateway.server.webflux.routes[0].id", "original-business-route"));
        clearRuntimeIdentityDefaults(defaults);
        var rendered = renderRuntimeProperties(defaults, Map.of("egon.yuheng.edge.resources.edge-ui.default-registration-id", "${xingyuan.it.actual-registration}"),
                Map.of("xingyuan.it.actual-registration", "new-actual-client"));
        assertThat(rendered).containsExactlyInAnyOrderEntriesOf(Map.of("egon.yuheng.edge.resources.edge-ui.default-registration-id", "new-actual-client",
                "spring.cloud.gateway.server.webflux.routes[0].id", "original-business-route"));
    }

    private static Map<String, String> readRuntimeTemplate(byte[] bytes) throws IOException {
        var properties = new java.util.Properties() {
            @Override public synchronized Object put(Object key, Object value) {
                if (containsKey(key)) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
                return super.put(key, value);
            }
        };
        try (var reader = new java.io.InputStreamReader(new java.io.ByteArrayInputStream(bytes), StandardCharsets.UTF_8.newDecoder())) { properties.load(reader); }
        Map<String, String> values = new java.util.LinkedHashMap<>();
        properties.forEach((key, value) -> values.put(key.toString(), value.toString()));
        return values;
    }

    private static String sha256Bytes(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException unavailable) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"); }
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class RuntimeProfilesReceiptDTO {
        @Min(1) @Max(1) private int schemaVersion;
        @NotBlank private String runId;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String manifestSha256;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String registrySha256;
        @NotNull @Size(min = 1, max = 12) @Valid private Map<@NotBlank String, FileReferenceDTO> profiles;
        @Builder.Default @NotNull @Size(max = 1) private Map<@Pattern(regexp = "native-agent") String, @Pattern(regexp = "[0-9a-f]{64}") String> runtimeExclusions = Map.of();
    }

    static Map<String, String> runtimeCredentialFacts(VerifiedDatabaseInputsBO inputs, IsolatedRuntimeRegistryDTO registry) throws IOException {
        Map<String, String> facts = new java.util.LinkedHashMap<>(runtimeRegistryFacts(inputs, registry));
        var manifest = inputs.getManifest();
        Path root = Path.of(manifest.getRuntimeRoot());
        for (String role : DATABASE_NAMES.keySet()) {
            var receipt = strictMapper().readValue(readProtectedBytes(root.resolve("evidence/db-" + role + ".json"),
                    root.resolve("evidence"), 128 * 1024), IsolatedDatabaseReceiptDTO.class);
            validateDatabaseReceipt(manifest, inputs.getManifestSha256(), role, receipt);
            String prefix = "xingyuan.it.postgres." + role + ".";
            facts.put(prefix + "url", "jdbc:postgresql://127.0.0.1:5432/" + receipt.getDatabaseName());
            facts.put(prefix + "username", receipt.getDatabaseOwner());
        }
        char[] postgres = readProtectedCharacters(Path.of(manifest.getPostgres().getCredentialFile()), null, 4096);
        try {
            char[] redis = readProtectedCharacters(Path.of(manifest.getRedis().getCredentialFile()), null, 4096);
            try {
                if (postgres.length == 0 || redis.length == 0) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
                facts.put("xingyuan.it.postgres.password", new String(postgres));
                facts.put("xingyuan.it.redis.password", new String(redis));
            } finally { java.util.Arrays.fill(redis, '\0'); }
        } finally { java.util.Arrays.fill(postgres, '\0'); }
        facts.put("xingyuan.it.redis.endpoint", manifest.getRedis().getEndpoint());
        facts.put("xingyuan.it.redis.host", "127.0.0.1"); facts.put("xingyuan.it.redis.port", "6379");
        facts.put("xingyuan.it.redis.database", Integer.toString(manifest.getRedis().getDatabase()));
        facts.put("xingyuan.it.redis.url", manifest.getRedis().getEndpoint() + "/" + manifest.getRedis().getDatabase());
        facts.put("xingyuan.it.redis.username", "default");
        facts.put("xingyuan.it.redis.credential-file", manifest.getRedis().getCredentialFile());
        for (var reference : manifest.getSecretFiles().entrySet()) {
            Path file = Path.of(reference.getValue());
            if (!file.isAbsolute() || !file.equals(file.normalize()) || !file.startsWith(root.resolve("secrets"))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            for (Path current = file; current != null && current.startsWith(root); current = current.getParent()) {
                if (Files.isSymbolicLink(current)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            }
            facts.put("xingyuan.it.secret." + reference.getKey() + ".file", file.toString());
        }
        for (String required : List.of("tls-keystore", "tls-keystore-password", "tls-ca-cert", "tls-server-cert", "tls-server-key")) {
            if (!manifest.getSecretFiles().containsKey(required)) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
        }
        facts.put("xingyuan.it.tls.key-store-uri", Path.of(manifest.getSecretFiles().get("tls-keystore")).toUri().toASCIIString());
        char[] password = readProtectedCharacters(Path.of(manifest.getSecretFiles().get("tls-keystore-password")), root.resolve("secrets"), 4096);
        try {
            if (password.length == 0) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
            facts.put("xingyuan.it.tls.key-store-password", new String(password));
        } finally { java.util.Arrays.fill(password, '\0'); }
        for (var client : registry.getClients()) {
            if (client.getRegistrationId() == null || client.getClientType() != OAuthClient.ClientType.CONFIDENTIAL) { continue; }
            if (client.getCredentialFile() == null || client.getCredentialVersion() == null || client.getCredentialVersion() < 1L) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE");
            }
            char[] secret = readProtectedCharacters(Path.of(client.getCredentialFile()), root.resolve("secrets"), 4096);
            try {
                if (secret.length == 0) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
                facts.put("xingyuan.it.client." + client.getRegistrationId() + ".client-secret", new String(secret));
            } finally { java.util.Arrays.fill(secret, '\0'); }
        }
        return java.util.Collections.unmodifiableMap(facts);
    }

    static Map<String, String> renderRuntimeProperties(Map<String, Object> defaults, Map<String, String> template,
                                                       Map<String, String> facts) {
        if (defaults == null || template == null || facts == null || defaults.size() + template.size() > 10000 || facts.size() > 10000) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
        Map<String, Object> inputs = new java.util.LinkedHashMap<>();
        for (var entry : defaults.entrySet()) {
            if (entry.getValue() == null) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
            inputs.put(entry.getKey(), entry.getValue().toString());
        }
        inputs.putAll(template);
        Map<String, Object> protectedFacts = new java.util.LinkedHashMap<>();
        Map<String, String> literals = new java.util.LinkedHashMap<>();
        Set<String> privateMarkers = new HashSet<>();
        for (var entry : new java.util.TreeMap<>(facts).entrySet()) {
            if (entry.getKey() == null || !entry.getKey().startsWith("xingyuan.it.") || entry.getValue() == null
                    || entry.getKey().indexOf('\0') >= 0 || entry.getValue().indexOf('\0') >= 0) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
            }
            // Derived values are data, including credentials that themselves contain ${...}.
            String marker = "\0fact-" + protectedFacts.size() + "\0";
            protectedFacts.put(entry.getKey(), marker);
            literals.put(marker, entry.getValue());
            if (entry.getKey().endsWith("password") || entry.getKey().endsWith("client-secret") || entry.getKey().contains(".subject.")) {
                privateMarkers.add(marker);
            }
        }
        var sources = new org.springframework.core.env.MutablePropertySources();
        sources.addFirst(new org.springframework.core.env.MapPropertySource("committedTaskFacts", protectedFacts) {
            @Override public Object getProperty(String name) {
                String selected = name;
                for (var literal : literals.entrySet()) {
                    if (!privateMarkers.contains(literal.getKey()) && literal.getValue().matches("[A-Za-z0-9._-]{1,128}")) {
                        selected = selected.replace(literal.getKey(), literal.getValue());
                    }
                }
                return super.getProperty(selected);
            }
        });
        sources.addLast(new org.springframework.core.env.MapPropertySource("artifactAndReviewedTemplate", inputs));
        var resolver = new org.springframework.core.env.PropertySourcesPropertyResolver(sources);
        Map<String, String> output = new java.util.TreeMap<>();
        Set<String> reviewedKeys = new HashSet<>();
        List<String> ordered = new java.util.ArrayList<>();
        defaults.keySet().stream().filter(key -> !template.containsKey(key)).sorted().forEach(ordered::add);
        template.keySet().stream().sorted().forEach(ordered::add);
        for (String inputKey : ordered) {
            String key = inputKey;
            Object raw = inputs.get(inputKey);
            if (key == null || key.isBlank() || key.startsWith("xingyuan.it.") || key.indexOf('\0') >= 0
                    || raw == null || raw.toString().indexOf('\0') >= 0) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
            }
            String value;
            try { key = resolver.resolveRequiredPlaceholders(key); value = resolver.resolveRequiredPlaceholders(raw.toString()); }
            catch (IllegalArgumentException missing) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
            for (String marker : privateMarkers) {
                if (key.contains(marker)) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
            }
            for (var literal : literals.entrySet()) {
                key = key.replace(literal.getKey(), literal.getValue());
                value = value.replace(literal.getKey(), literal.getValue());
            }
            if (key.indexOf('\0') >= 0 || value.indexOf('\0') >= 0 || key.contains("${")) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
            }
            if (output.containsKey(key) && !template.containsKey(inputKey)) {
                throw new IllegalArgumentException("XINGYUAN_STATE_CONFLICT");
            }
            if (template.containsKey(inputKey) && !reviewedKeys.add(key)) {
                throw new IllegalArgumentException("XINGYUAN_STATE_CONFLICT");
            }
            // The next Spring environment must also treat restored fact values as opaque literals.
            output.put(key, value.replace("${", "\\${"));
        }
        return java.util.Collections.unmodifiableMap(output);
    }

    @Test
    void reviewedRuntimeTemplatesUseActualFactsAndKeepCredentialTextOpaque() throws Exception {
        String issuer = "https://127.0.0.1:28443/tenants/1001";
        String credential = " leading ${do-not-expand}\n目录";
        String key = "egon.yuheng.edge.resource-bindings.[edge-ui].issuer-registration-ids.[" + issuer + "]";
        Map<String, String> rendered = renderRuntimeProperties(Map.of("controlled.base", "base", "controlled.inherited", "${controlled.alias}"),
                Map.of("controlled.alias", "${xingyuan.it.realm.default.issuer}", "controlled.credential", "${xingyuan.it.postgres.password}",
                        "controlled.echo", "${controlled.credential}",
                        "egon.yuheng.edge.resource-bindings.[edge-ui].issuer-registration-ids.[${xingyuan.it.realm.default.issuer}]", "edge-ui-a"),
                Map.of("xingyuan.it.realm.default.issuer", issuer, "xingyuan.it.postgres.password", credential));
        assertThat(rendered.get(key)).isEqualTo("edge-ui-a");
        var properties = new java.util.Properties();
        properties.load(new java.io.ByteArrayInputStream(encodeRuntimeProperties(rendered)));
        var sources = new org.springframework.core.env.MutablePropertySources();
        sources.addFirst(new org.springframework.core.env.PropertiesPropertySource("controlled-runtime", properties));
        var resolver = new org.springframework.core.env.PropertySourcesPropertyResolver(sources);
        assertThat(resolver.getProperty("controlled.inherited")).isEqualTo(issuer);
        assertThat(resolver.getProperty("controlled.credential")).isEqualTo(credential);
        assertThat(resolver.getProperty("controlled.echo")).isEqualTo(credential);
    }

    static byte[] encodeRuntimeProperties(Map<String, String> values) throws IOException {
        if (values == null || values.size() > 10000 || values.entrySet().stream().anyMatch(entry ->
                entry.getKey() == null || entry.getKey().isBlank() || entry.getKey().length() > 4096
                        || entry.getValue() == null || entry.getValue().length() > 32768
                        || entry.getKey().indexOf('\0') >= 0 || entry.getValue().indexOf('\0') >= 0)) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
        var properties = new java.util.Properties();
        new java.util.TreeMap<>(values).forEach(properties::setProperty);
        var output = new java.io.ByteArrayOutputStream();
        properties.store(output, null);
        // JDK escaping preserves bracketed URI keys and Unicode; omit its timestamp comment for stable bytes.
        byte[] encoded = output.toString(StandardCharsets.ISO_8859_1).lines().filter(line -> !line.startsWith("#"))
                .collect(java.util.stream.Collectors.joining("\n", "", "\n")).getBytes(StandardCharsets.US_ASCII);
        if (encoded.length > 2 * 1024 * 1024) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
        return encoded;
    }

    @Test
    void runtimePropertiesPreserveTheExactIssuerMapKeyAndUtf8Credential() throws Exception {
        String key = "egon.yuheng.edge.resource-bindings.[edge-ui].issuer-registration-ids.[https://127.0.0.1:28443/tenants/1001]";
        Map<String, String> expected = Map.of(key, "edge-ui-a", "controlled.value", " leading \n目录\\second\r\t=:#");
        byte[] encoded = encodeRuntimeProperties(expected);
        var decoded = new java.util.Properties();
        decoded.load(new java.io.ByteArrayInputStream(encoded));
        assertThat(decoded.getProperty(key)).isEqualTo("edge-ui-a");
        assertThat(decoded.getProperty("controlled.value")).isEqualTo(expected.get("controlled.value"));
        assertThat(encodeRuntimeProperties(expected)).isEqualTo(encoded);
    }

    static IsolatedRuntimeRegistryDTO buildRuntimeRegistry(IsolatedPhaseCommandDTO command, String manifestSha256) {
        if (command.getIdentityFacts() == null) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
        validateProvisioningIdentity(command.getIdentityFacts().getSubjects(), command.getIdentityFacts().getProvisioningSubject());
        List<OAuthClientPreparationDTO> recipeClients = new java.util.ArrayList<>(command.getRecipe().getManagementClients());
        recipeClients.addAll(command.getRecipe().getClients());
        if (command.getCommittedClients().size() != recipeClients.size()
                || command.getCommittedResources().size() != command.getRecipe().getResources().size()) {
            throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
        }
        var converter = new IsolatedClientFactConverterImpl();
        List<IsolatedClientFactDTO> clients = new java.util.ArrayList<>();
        var ids = new HashSet<String>();
        var registrations = new HashSet<String>();
        var credentials = new HashSet<String>();
        for (var input : recipeClients) {
            String key = input.getRealmCode() + ":" + input.getInput().getClientId();
            OAuthClientBO committed = command.getCommittedClients().get(key);
            if (!ids.add(key) || committed == null || committed.getId() == null || committed.getId() <= 0L
                    || committed.getStatus() != OAuthClient.Status.ACTIVE || !command.getRealms().containsKey(input.getRealmCode())
                    || input.getRegistrationId() != null && !registrations.add(input.getRegistrationId())
                    || input.getSecretFile() != null && !credentials.add(input.getSecretFile())) {
                throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
            }
            clients.add(converter.toTarget(IsolatedClientFactBO.builder().client(committed).realmCode(input.getRealmCode())
                    .issuer(command.getRealms().get(input.getRealmCode()).getIssuer().toASCIIString())
                    .credentialFile(input.getSecretFile()).registrationId(input.getRegistrationId()).alias(input.getAlias()).build()));
        }
        return IsolatedRuntimeRegistryDTO.builder().schemaVersion(1).runId(command.getManifest().getRunId())
                .manifestSha256(manifestSha256).publicOrigin(command.getManifest().getPublicOrigin())
                .platformStoragePartitionId(command.getManifest().getPlatformStoragePartitionId())
                .tenantIds(Map.copyOf(command.getIdentityFacts().getTenantIds())).realms(Map.copyOf(command.getRealms()))
                .subjects(Map.copyOf(command.getIdentityFacts().getSubjects())).provisioningSubject(command.getIdentityFacts().getProvisioningSubject())
                .clients(List.copyOf(clients))
                .resources(List.copyOf(command.getCommittedResources().values()))
                .cliResources(List.copyOf(command.getRecipe().getCliResources())).aliases(validateRegistrationAliases(clients)).build();
    }

    @Test
    void privateRealmMetadataRetainsItsRealPartitionAndTrustVersion() throws Exception {
        var realm = TrustedRealmBO.builder().issuer(URI.create("https://127.0.0.1:28443/tenants/1001"))
                .realmKey("tenant:1001").realmType(RealmTypeEnum.TENANT).businessTenantId(1001L)
                .storagePartitionId(1001L).trustVersion(7L).build();
        realm.validateTrustedPartition();
        var registry = IsolatedRuntimeRegistryDTO.builder().schemaVersion(1).realms(Map.of("default", realm)).build();
        var loaded = strictMapper().readValue(strictMapper().writeValueAsBytes(registry), IsolatedRuntimeRegistryDTO.class);
        assertThat(loaded.getRealms().get("default").getIssuer()).isEqualTo(realm.getIssuer());
        assertThat(loaded.getRealms().get("default").getStoragePartitionId()).isEqualTo(1001L);
        assertThat(loaded.getRealms().get("default").getTrustVersion()).isEqualTo(7L);
    }

    @Test
    void committedIdentitySubjectsStayInThePrivateRegistryAndOutOfDiagnosticText() throws Exception {
        var registry = IsolatedRuntimeRegistryDTO.builder().schemaVersion(1).runId(UUID.randomUUID().toString())
                .subjects(Map.of("admin", "controlled-subject-admin", "viewer", "controlled-subject-viewer")).build();
        String json = strictMapper().writeValueAsString(registry);
        assertThat(strictMapper().readValue(json, IsolatedRuntimeRegistryDTO.class).getSubjects())
                .containsExactlyInAnyOrderEntriesOf(registry.getSubjects());
        assertThat(registry.toString()).doesNotContain("controlled-subject-admin", "controlled-subject-viewer");
    }

    static Map<String, IsolatedResourceAliasDTO> validateRegistrationAliases(List<IsolatedClientFactDTO> clients) {
        var aliases = new java.util.LinkedHashMap<String, IsolatedResourceAliasDTO>();
        var registrations = new HashSet<String>();
        for (String alias : List.of("edge-ui", "edge-shoubing", "edge-jianshen", "edge-yuheng", "edge-tianshu")) {
            var candidates = clients.stream().filter(client -> alias.equals(client.getAlias())).toList();
            if (candidates.size() != 2) {
                throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
            }
            var byRealm = new java.util.LinkedHashMap<String, IsolatedClientFactDTO>();
            for (var client : candidates) {
                if (client.getRealmCode() == null || client.getIssuer() == null || client.getClientId() == null
                        || !Set.of("default", "tenant-b").contains(client.getRealmCode())
                        || byRealm.putIfAbsent(client.getRealmCode(), client) != null
                        || client.getClientType() != OAuthClient.ClientType.CONFIDENTIAL || client.getPurpose() != OAuthClientPurposeEnum.USER
                        || client.getClientPk() == null || client.getClientPk() <= 0L || client.getStatus() != OAuthClient.Status.ACTIVE
                        || client.getResourceUris() == null || client.getResourceUris().size() != 1
                        || client.getAllowedScopes() == null || !client.getAllowedScopes().containsAll(List.of("openid", "profile", "offline_access"))
                        || client.getRefreshTokenTtlSeconds() == null || client.getRefreshTokenTtlSeconds() <= 0
                        || client.getCredentialFile() == null || client.getRegistrationId() == null || !registrations.add(client.getRegistrationId())) {
                    throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
                }
            }
            var first = byRealm.get("default");
            var second = byRealm.get("tenant-b");
            if (first == null || second == null || first.getIssuer().equals(second.getIssuer()) || first.getClientPk().equals(second.getClientPk())
                    || first.getClientId().equals(second.getClientId()) || first.getCredentialFile().equals(second.getCredentialFile())
                    || !first.getResourceUris().equals(second.getResourceUris())) {
                throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
            }
            aliases.put(alias, IsolatedResourceAliasDTO.builder().defaultRegistrationId(first.getRegistrationId())
                    .issuerRegistrationIds(Map.of(first.getIssuer(), first.getRegistrationId(), second.getIssuer(), second.getRegistrationId())).build());
        }
        if (clients.stream().anyMatch(client -> client.getAlias() != null && !aliases.containsKey(client.getAlias()))) {
            throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
        }
        return Map.copyOf(aliases);
    }

    @Test
    void issuerAliasesRequireIndependentCommittedClientsForBothTenants() {
        List<IsolatedClientFactDTO> clients = new java.util.ArrayList<>();
        long key = 1L;
        for (String alias : List.of("edge-ui", "edge-shoubing", "edge-jianshen", "edge-yuheng", "edge-tianshu")) {
            for (String realm : List.of("default", "tenant-b")) {
                String suffix = realm.equals("default") ? "a" : "b";
                clients.add(IsolatedClientFactDTO.builder().alias(alias).realmCode(realm).clientPk(key++)
                        .clientId("client-" + alias + "-" + suffix).registrationId(alias + "-" + suffix)
                        .issuer("https://127.0.0.1:28443/tenants/" + (realm.equals("default") ? "1001" : "1002"))
                        .clientType(OAuthClient.ClientType.CONFIDENTIAL).purpose(OAuthClientPurposeEnum.USER).status(OAuthClient.Status.ACTIVE)
                        .credentialFile("/task/secrets/" + alias + "-" + suffix + ".secret")
                        .resourceUris(List.of("https://resources.example/" + alias))
                        .allowedScopes(List.of("openid", "profile", "offline_access", "resource:read"))
                        .refreshTokenTtlSeconds(3600).build());
            }
        }
        var aliases = validateRegistrationAliases(clients);
        assertThat(aliases).hasSize(5);
        assertThat(aliases.get("edge-ui").getDefaultRegistrationId()).isEqualTo("edge-ui-a");
        assertThat(aliases.get("edge-ui").getIssuerRegistrationIds())
                .containsEntry("https://127.0.0.1:28443/tenants/1001", "edge-ui-a")
                .containsEntry("https://127.0.0.1:28443/tenants/1002", "edge-ui-b");
        var missing = clients.stream().filter(client -> !("edge-ui".equals(client.getAlias()) && "tenant-b".equals(client.getRealmCode()))).toList();
        assertThatThrownBy(() -> validateRegistrationAliases(missing)).hasMessage("XINGYUAN_STATE_CONFLICT");
        clients.get(1).setCredentialFile(clients.getFirst().getCredentialFile());
        assertThatThrownBy(() -> validateRegistrationAliases(clients)).hasMessage("XINGYUAN_STATE_CONFLICT");
        clients.get(1).setCredentialFile("/task/secrets/edge-ui-b.secret").setIssuer(clients.getFirst().getIssuer());
        assertThatThrownBy(() -> validateRegistrationAliases(clients)).hasMessage("XINGYUAN_STATE_CONFLICT");
    }

    @Test
    void clientRegistryProjectionCarriesOnlyCredentialReferences() throws Exception {
        var client = OAuthClientBO.builder().id(11L).clientId("client-a").clientType(OAuthClient.ClientType.CONFIDENTIAL)
                .purpose(OAuthClientPurposeEnum.USER).status(OAuthClient.Status.ACTIVE).credentialVersion(1L)
                .secretHash("must-not-enter-the-registry").redirectUris(List.of("https://127.0.0.1:28443/callback"))
                .resourceUris(List.of("https://resources.example/a")).allowedScopes(List.of("openid"))
                .accessTtlSeconds(300).refreshTtlSeconds(3600).build();
        var source = IsolatedClientFactBO.builder().client(client).realmCode("default")
                .issuer("https://127.0.0.1:28443/tenants/1001").credentialFile("/task/secrets/client-a.secret")
                .registrationId("edge-ui-a").alias("edge-ui").build();
        var result = new IsolatedClientFactConverterImpl().toTarget(source);
        assertThat(result.getClientPk()).isEqualTo(11L);
        assertThat(result.getCredentialFile()).isEqualTo("/task/secrets/client-a.secret");
        String json = strictMapper().writeValueAsString(result);
        assertThat(json).doesNotContain("must-not-enter-the-registry", "secretHash", "clientSecret", "subject");
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class IsolatedPhaseCommandDTO {
        @NotNull private ProvisionStageEnum stage;
        private boolean resume;
        @NotNull @Valid private IsolatedIntegrationManifestDTO manifest;
        private Map<String, Path> passwordFiles;
        @com.fasterxml.jackson.annotation.JsonIgnore
        @lombok.ToString.Exclude
        private IsolatedOAuthProvisioningComponent.IdentityFactsBO identityFacts;
        private Map<String, TrustedRealmBO> realms;
        @Valid private IdentityProvisioningRecipeDTO recipe;
        @com.fasterxml.jackson.annotation.JsonIgnore
        @lombok.ToString.Exclude
        @Builder.Default private Map<String, OAuthClientBO> committedClients = new java.util.LinkedHashMap<>();
        @Builder.Default private Map<String, top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ResourceServerVO> committedResources = new java.util.LinkedHashMap<>();
        @Builder.Default private List<top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ClientResourceGrantVO> committedGrants = new java.util.ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class IdentityProvisioningRecipeDTO {
        @Min(1) @Max(1) private int schemaVersion;
        @NotBlank @Pattern(regexp = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}") private String runId;
        @NotNull @Size(min = 1, max = 32) @Valid private List<OAuthClientPreparationDTO> managementClients;
        @NotNull @Size(min = 1, max = 32) @Valid private List<top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.CreateResourceServerDTO> resources;
        @NotNull @Size(min = 1, max = 160) @Valid private List<OAuthClientPreparationDTO> clients;
        @NotNull @Size(max = 192) @Valid private List<OAuthGrantPreparationDTO> grants;
        @Builder.Default @NotNull @Size(max = 32) @Valid private List<CliResourcePreparationDTO> cliResources = List.of();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class CliResourcePreparationDTO {
        @NotBlank @Pattern(regexp = "[a-z][a-z0-9-]{2,63}") private String resourceKey;
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,128}") private String resourceServerId;
        @NotNull @Size(min = 2, max = 2)
        private Map<@Pattern(regexp = "default|tenant-b") String, @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,128}") String> clientIds;
        @NotBlank @Size(max = 2048) @Pattern(regexp = "/[^\\s]*") private String readPath;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class OAuthClientPreparationDTO {
        @NotBlank @Pattern(regexp = "platform|default|tenant-b") private String realmCode;
        @NotNull @Valid private CreateOAuthClientDTO input;
        @Size(max = 4096) private String secretFile;
        @Pattern(regexp = "[A-Za-z0-9._-]{1,128}") private String registrationId;
        @Pattern(regexp = "edge-ui|edge-shoubing|edge-jianshen|edge-yuheng|edge-tianshu") private String alias;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class OAuthGrantPreparationDTO {
        @NotBlank @Pattern(regexp = "platform|default|tenant-b") private String realmCode;
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,128}") private String clientId;
        @NotBlank @Size(max = 128) private String resourceServerId;
        @NotNull @Valid private UpsertClientResourceGrantDTO input;
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
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class IsolatedIntegrationResult {
        private int schemaVersion;
        private String operationId;
        private String runId;
        private String manifestSha256;
        private String status;
        @JsonFormat(shape = JsonFormat.Shape.STRING) private Instant completedAt;
        @Builder.Default private List<IsolatedTargetResultDTO> targets = List.of();
        @NotNull private IsolatedCountsDTO counts;
        @Builder.Default private List<IsolatedStepResultDTO> steps = List.of();
        private IsolatedErrorDTO error;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class IsolatedCountsDTO {
        @Min(0) @Max(12) private int databaseCount;
        @Min(0) @Max(2) private int tenantCount;
        @Min(0) @Max(192) private int clientCount;
        @Min(0) @Max(32) private int catalogCount;
        @Min(0) @Max(128) private int liveCaseCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class IsolatedTargetResultDTO {
        private String targetKey;
        private String identity;
        private String version;
        private String state;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class IsolatedStepResultDTO {
        private String stepId;
        private String state;
        private String evidence;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class IsolatedErrorDTO {
        private String code;
        private String message;
        private boolean retryable;
        private String stage;
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
