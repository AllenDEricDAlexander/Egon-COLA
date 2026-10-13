package top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.controller.cli;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.command.DeploymentCatalogRegistrationCommand;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.service.GlobalResourceCatalogService;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.repository.IdentityProfileDirectory;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.repository.IdentityTenantMembershipDirectory;
import top.egon.cola.platform.tianquan.jianshen.core.rule.Rbac3RuleViolation;

/** Bounded local operator entry; the manifest selects facts, never grants resource access. */
@Slf4j
@RequiredArgsConstructor
public class Rbac3ApplicationCatalogRegistrationCli {
    public static final String COMMAND = "register-application-catalog";
    private static final int MAX_FILE_BYTES = 1_048_576;
    private static final Set<String> OPTIONS = Set.of("--manifest", "--catalog-file", "--business-code", "--application-code", "--expected-version", "--actor-id");
    @Qualifier("globalResourceCatalogService") private final GlobalResourceCatalogService catalog;
    @Qualifier("identityTenantMembershipDirectory") private final IdentityTenantMembershipDirectory memberships;
    @Qualifier("identityProfileDirectory") private final IdentityProfileDirectory profiles;
    @Qualifier("jacksonObjectMapper") private final ObjectMapper mapper;
    @Qualifier("defaultValidator") private final Validator validator;

    public int run(String[] arguments) {
        try {
            PreparedCatalogInvocationBO prepared = prepare(arguments, mapper, validator);
            var manifest = prepared.getManifest(); var command = prepared.getCommand(); String actor = prepared.getActor();
            var batch = profiles.batchGet(List.of(actor));
            if (!batch.available()) { throw new Rbac3RuleViolation("IDENTITY_DIRECTORY_UNAVAILABLE"); }
            var profile = batch.profiles().get(actor);
            if (profile == null || !actor.equals(profile.subject()) || !"ACTIVE".equals(profile.status())) { throw new Rbac3RuleViolation("IDENTITY_NOT_ACTIVE"); }
            memberships.requireActive(manifest.getTenantId(), actor);
            var result = catalog.registerDeploymentCatalog(command, actor);
            try { System.out.println(mapper.writeValueAsString(result)); }
            catch (com.fasterxml.jackson.core.JsonProcessingException outputFailure) {
                throw new IllegalStateException("Committed catalog receipt could not be emitted", outputFailure);
            }
            return 0;
        } catch (IllegalArgumentException | jakarta.validation.ValidationException | IOException invalid) {
            log.warn("Deployment catalog command rejected; outcome=INVALID_INPUT, failureType={}", invalid.getClass().getSimpleName()); return 2;
        } catch (Rbac3RuleViolation conflict) {
            boolean unavailable = conflict.reasonCode().contains("UNAVAILABLE");
            log.warn("Deployment catalog command did not complete; outcome={}", unavailable ? "DEPENDENCY_UNAVAILABLE" : "CONFLICT"); return unavailable ? 4 : 3;
        } catch (RuntimeException unknown) {
            // Operator boundary cannot claim a failed/unknown transaction committed; no payload/cause is emitted.
            log.error("Deployment catalog command outcome is unknown; failureType={}", unknown.getClass().getSimpleName()); return 4;
        }
    }

    /** Executes all local input/owner/artifact checks in a Jackson/Validation-only context before any DB/RPC context. */
    public static int validateLocalInvocation(String[] arguments, ObjectMapper mapper, Validator validator) {
        try { prepare(arguments, mapper, validator); return 0; }
        catch (IllegalArgumentException | jakarta.validation.ValidationException | IOException invalid) {
            log.warn("Deployment catalog preflight rejected; failureType={}", invalid.getClass().getSimpleName()); return 2;
        }
    }

    private static PreparedCatalogInvocationBO prepare(String[] arguments, ObjectMapper mapper, Validator validator) throws IOException {
        Map<String, String> options = parse(arguments);
        String explicitManifest = options.get("--manifest"); String testAlias = System.getenv("XINGYUAN_INTEGRATION_MANIFEST");
        if (explicitManifest == null && testAlias == null) { throw new IllegalArgumentException("Explicit deployment manifest is required"); }
        Path manifestFile = protectedFile(explicitManifest == null ? testAlias : explicitManifest);
        if (testAlias != null && !manifestFile.equals(protectedFile(testAlias))) { throw new IllegalArgumentException("Manifest sources disagree"); }
        var reader = mapper.reader().with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        DeploymentManifestBO manifest = reader.forType(DeploymentManifestBO.class).readValue(readBounded(manifestFile));
        new ValidationUtils(validator).validate(manifest);
        Path root = protectedDirectory(manifest.getRootPath());
        if (!manifestFile.startsWith(root)) { throw new IllegalArgumentException("Manifest is outside its deployment root"); }
        if (testAlias != null && !root.getFileName().toString().equals("xingyuan-integration-" + manifest.getRunId())) {
            throw new IllegalArgumentException("Integration manifest has a foreign run root");
        }
        String actor = required(options, "--actor-id");
        if (!actor.equals(manifest.getIdentitySub())) { throw new IllegalArgumentException("Deployment actor is not manifest-bound"); }
        String business = required(options, "--business-code"), application = required(options, "--application-code");
        var owner = manifest.getApplications().stream().filter(value -> business.equals(value.getBusinessCode()) && application.equals(value.getApplicationCode()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Application is outside the deployment manifest"));
        Path artifact = protectedFile(owner.getArtifactFile());
        if (!artifact.startsWith(root) || !owner.getArtifactSha256().equals(fileDigest(artifact))) { throw new IllegalArgumentException("Deployment artifact binding is invalid"); }
        Path catalogFile = protectedFile(required(options, "--catalog-file"));
        if (!catalogFile.startsWith(root)) { throw new IllegalArgumentException("Catalog file is outside the deployment root"); }
        byte[] bytes = readBounded(catalogFile);
        if (!owner.getCatalogSha256().equals(digest(bytes))) { throw new IllegalArgumentException("Deployment catalog file binding is invalid"); }
        DeploymentCatalogRegistrationCommand command = reader.forType(DeploymentCatalogRegistrationCommand.class).readValue(bytes);
        new ValidationUtils(validator).validate(command, DeploymentCatalogRegistrationCommand.Registration.class);
        long expected = Long.parseLong(required(options, "--expected-version"));
        if (expected < 0 || command.getExpectedApplicationVersion() != expected || !business.equals(command.getBusinessCode())
                || !application.equals(command.getApplicationCode()) || !owner.getBuildId().equals(command.getBuildId())) {
            throw new IllegalArgumentException("Catalog command is not deployment-bound");
        }
        return new PreparedCatalogInvocationBO(manifest, command, actor);
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    private static class PreparedCatalogInvocationBO {
        @NotNull @Valid private DeploymentManifestBO manifest;
        @NotNull @Valid private DeploymentCatalogRegistrationCommand command;
        @NotBlank private String actor;
    }

    private static Map<String, String> parse(String[] arguments) {
        if (arguments == null || arguments.length < 1 || !COMMAND.equals(arguments[0]) || (arguments.length - 1) % 2 != 0) {
            throw new IllegalArgumentException("Invalid deployment catalog command");
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (int index = 1; index < arguments.length; index += 2) {
            String key = arguments[index], value = arguments[index + 1];
            if (!OPTIONS.contains(key) || value == null || value.isBlank() || !value.equals(value.strip()) || result.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("Invalid or duplicate deployment catalog option");
            }
        }
        for (String key : OPTIONS) { if (!key.equals("--manifest")) { required(result, key); } }
        return result;
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) { throw new IllegalArgumentException("Required deployment option is absent"); }
        return value;
    }

    private static Path absolutePath(String value) {
        if (value == null || value.isBlank() || value.length() > 4096 || value.indexOf('\0') >= 0) { throw new IllegalArgumentException("Deployment path is invalid"); }
        Path path = Path.of(value);
        if (!path.isAbsolute() || !path.normalize().equals(path)) { throw new IllegalArgumentException("Deployment path must be absolute and normalized"); }
        for (Path segment : path) { if (segment.toString().equals("..")) { throw new IllegalArgumentException("Deployment path escapes its root"); } }
        return path;
    }

    private static Path protectedFile(String value) throws IOException {
        Path path = absolutePath(value);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path) || !path.equals(path.toRealPath())) {
            throw new IllegalArgumentException("Deployment file must be a canonical regular file");
        }
        requireOwnerPermissions(path); return path;
    }

    private static Path protectedDirectory(String value) throws IOException {
        Path path = absolutePath(value);
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path) || !path.equals(path.toRealPath())) {
            throw new IllegalArgumentException("Deployment root must be a canonical directory");
        }
        requireOwnerPermissions(path); return path;
    }

    private static void requireOwnerPermissions(Path path) throws IOException {
        if (!Files.getOwner(path, LinkOption.NOFOLLOW_LINKS).getName().equals(System.getProperty("user.name"))) { throw new IllegalArgumentException("Deployment file has a foreign owner"); }
        var permissions = Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS);
        if (permissions.contains(PosixFilePermission.GROUP_WRITE) || permissions.contains(PosixFilePermission.OTHERS_WRITE)) {
            throw new IllegalArgumentException("Deployment file is writable by another principal");
        }
    }

    private static byte[] readBounded(Path path) throws IOException {
        if (Files.size(path) > MAX_FILE_BYTES) { throw new IllegalArgumentException("Deployment input exceeds its bound"); }
        ByteBuffer buffer = ByteBuffer.allocate(MAX_FILE_BYTES + 1);
        try (SeekableByteChannel channel = Files.newByteChannel(path, Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
            while (buffer.hasRemaining() && channel.read(buffer) >= 0) { }
        }
        if (buffer.position() > MAX_FILE_BYTES) { throw new IllegalArgumentException("Deployment input exceeds its bound"); }
        return java.util.Arrays.copyOf(buffer.array(), buffer.position());
    }

    private static String fileDigest(Path path) throws IOException {
        if (Files.size(path) > 536_870_912L) { throw new IllegalArgumentException("Deployment artifact exceeds its bound"); }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); ByteBuffer buffer = ByteBuffer.allocate(65_536);
            try (SeekableByteChannel channel = Files.newByteChannel(path, Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
                long count = 0; int read;
                while ((read = channel.read(buffer)) >= 0) { count += read; if (count > 536_870_912L) { throw new IllegalArgumentException("Deployment artifact exceeds its bound"); }
                    buffer.flip(); digest.update(buffer); buffer.clear(); }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException failure) { throw new IllegalStateException("Deployment digest is unavailable", failure); }
    }

    private static String digest(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException failure) { throw new IllegalStateException("Deployment digest is unavailable", failure); }
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class DeploymentManifestBO {
        @Min(1) @jakarta.validation.constraints.Max(1) private int schemaVersion;
        @NotBlank @Pattern(regexp = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}") private String runId;
        @NotBlank @Size(max = 4096) private String rootPath;
        @NotBlank @Size(max = 64) private String identitySub;
        @NotBlank @Pattern(regexp = "[1-9][0-9]{0,18}") @DecimalMax("9223372036854775807") private String tenantId;
        @NotNull @Size(min = 1, max = 64) @Valid private List<@NotNull ApplicationDeploymentBO> applications;
        @JsonIgnore @AssertTrue public boolean isApplicationSetValid() {
            return applications != null && applications.stream().noneMatch(java.util.Objects::isNull)
                    && applications.stream().map(ApplicationDeploymentBO::getApplicationCode).distinct().count() == applications.size();
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class ApplicationDeploymentBO {
        @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,127}") private String businessCode;
        @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,127}") private String applicationCode;
        @NotBlank @Size(max = 256) private String buildId;
        @NotBlank @Size(max = 4096) private String artifactFile;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String artifactSha256;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String catalogSha256;
    }
}
