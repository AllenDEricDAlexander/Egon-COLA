package top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.controller.cli;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;
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
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.dto.ReplaceRoleFieldRulesCommandDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.dto.ReplaceRoleFieldRulesCommandDTO.RoleFieldRuleDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.service.RoleResourceGrantService;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.command.DeploymentCatalogRegistrationCommand;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.service.GlobalResourceCatalogService;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.repository.IdentityProfileDirectory;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.repository.IdentityTenantMembershipDirectory;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.DdcCatalogGateway;
import top.egon.cola.platform.tianquan.jianshen.core.rule.Rbac3RuleViolation;

/** Bounded local operator entry; the manifest selects facts, never grants resource access. */
@Slf4j
@RequiredArgsConstructor
public class Rbac3ApplicationCatalogRegistrationCli {
    public static final String COMMAND = "register-application-catalog";
    public static final String FIELD_COMMAND = "replace-role-field-rules";
    private static final int MAX_FILE_BYTES = 1_048_576;
    private static final Set<String> OPTIONS = Set.of("--manifest", "--catalog-file", "--business-code", "--application-code", "--expected-version", "--actor-id");
    private static final Set<String> FIELD_OPTIONS = Set.of("--manifest", "--rules-file", "--tenant-id", "--application-code", "--role-id", "--expected-role-version", "--actor-id");
    @Qualifier("globalResourceCatalogService") private final GlobalResourceCatalogService catalog;
    @Qualifier("identityTenantMembershipDirectory") private final IdentityTenantMembershipDirectory memberships;
    @Qualifier("identityProfileDirectory") private final IdentityProfileDirectory profiles;
    @Qualifier("jacksonObjectMapper") private final ObjectMapper mapper;
    @Qualifier("defaultValidator") private final Validator validator;
    @Qualifier("roleResourceGrantService") private final RoleResourceGrantService roleGrants;
    @Qualifier("ddcCatalogGateway") private final DdcCatalogGateway deploymentCatalog;

    /** Existing catalog-only callers retain their construction contract. */
    public Rbac3ApplicationCatalogRegistrationCli(GlobalResourceCatalogService catalog, IdentityTenantMembershipDirectory memberships,
            IdentityProfileDirectory profiles, ObjectMapper mapper, Validator validator) {
        this(catalog, memberships, profiles, mapper, validator, null, null);
    }

    public int run(String[] arguments) {
        try {
            PreparedDeploymentInvocationBO prepared = prepare(arguments, mapper, validator);
            var manifest = prepared.getManifest(); var command = prepared.getCommand(); String actor = prepared.getActor();
            var batch = profiles.batchGet(List.of(actor));
            if (!batch.available()) { throw new Rbac3RuleViolation("IDENTITY_DIRECTORY_UNAVAILABLE"); }
            var profile = batch.profiles().get(actor);
            if (profile == null || !actor.equals(profile.subject()) || !"ACTIVE".equals(profile.status())) { throw new Rbac3RuleViolation("IDENTITY_NOT_ACTIVE"); }
            memberships.requireActive(manifest.getTenantId(), actor);
            Object result;
            if (prepared.getFieldRules() == null) {
                result = catalog.registerDeploymentCatalog(command, actor);
            } else {
                if (roleGrants == null || deploymentCatalog == null) { throw new Rbac3RuleViolation("ROLE_FIELD_RULE_WRITER_UNAVAILABLE"); }
                Long applicationId = resolveActiveApplication(prepared.getOwner());
                var fieldCommand = ReplaceRoleFieldRulesCommandDTO.builder()
                        .tenantId(Long.valueOf(manifest.getTenantId())).applicationId(applicationId).roleId(prepared.getRoleId())
                        .expectedRoleVersion(prepared.getExpectedRoleVersion()).actorId(actor).rules(prepared.getFieldRules().getRules()).build();
                new ValidationUtils(validator).validate(fieldCommand, ReplaceRoleFieldRulesCommandDTO.Mutation.class);
                result = roleGrants.replaceFieldRules(fieldCommand);
            }
            try { System.out.println(mapper.writeValueAsString(result)); }
            catch (com.fasterxml.jackson.core.JsonProcessingException outputFailure) {
                throw new IllegalStateException("Committed deployment receipt could not be emitted", outputFailure);
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

    private Long resolveActiveApplication(ApplicationDeploymentBO owner) {
        var businesses = deploymentCatalog.listBusinesses(owner.getBusinessCode()).stream()
                .filter(value -> owner.getBusinessCode().equals(value.bizCode()) && value.enabled()).toList();
        if (businesses.size() != 1) { throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH"); }
        var business = businesses.getFirst();
        var applications = deploymentCatalog.listApplications(business.ddcBusinessId(), owner.getApplicationCode()).stream()
                .filter(value -> owner.getApplicationCode().equals(value.appCode()) && owner.getBusinessCode().equals(value.bizCode())
                        && business.ddcBusinessId().equals(value.ddcBusinessId()) && value.applicationEnabled() && value.businessEnabled()).toList();
        if (applications.size() != 1) { throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH"); }
        var global = catalog.applications().stream().filter(value -> owner.getApplicationCode().equals(value.applicationCode()) && "ACTIVE".equals(value.status())).toList();
        if (global.size() != 1) { throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND"); }
        return positiveId(global.getFirst().applicationId());
    }

    private static PreparedDeploymentInvocationBO prepare(String[] arguments, ObjectMapper mapper, Validator validator) throws IOException {
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
        boolean fieldMode = FIELD_COMMAND.equals(arguments[0]);
        String business = fieldMode ? null : required(options, "--business-code"), application = required(options, "--application-code");
        var owner = manifest.getApplications().stream().filter(value -> (fieldMode || business.equals(value.getBusinessCode())) && application.equals(value.getApplicationCode()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Application is outside the deployment manifest"));
        Path artifact = protectedFile(owner.getArtifactFile());
        if (!artifact.startsWith(root) || !owner.getArtifactSha256().equals(fileDigest(artifact))) { throw new IllegalArgumentException("Deployment artifact binding is invalid"); }
        if (fieldMode) {
            Long tenant = positiveId(required(options, "--tenant-id")), role = positiveId(required(options, "--role-id"));
            long expected = nonnegativeVersion(required(options, "--expected-role-version"));
            if (!tenant.toString().equals(manifest.getTenantId())) { throw new IllegalArgumentException("Field-rule tenant is outside the deployment manifest"); }
            var target = owner.getFieldRuleTargets().stream().filter(value -> role.equals(value.getRoleId()) && expected == value.getExpectedRoleVersion())
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Field-rule role/version is outside the deployment manifest"));
            Path rulesFile = protectedFile(required(options, "--rules-file"));
            if (!rulesFile.startsWith(root) || !rulesFile.equals(protectedFile(target.getRulesFile()))) { throw new IllegalArgumentException("Field-rule file is outside its deployment target"); }
            byte[] rulesBytes = readBounded(rulesFile);
            if (!target.getRulesSha256().equals(digest(rulesBytes))) { throw new IllegalArgumentException("Field-rule file binding is invalid"); }
            RoleFieldRulesFileDTO rules = reader.forType(RoleFieldRulesFileDTO.class).readValue(rulesBytes);
            new ValidationUtils(validator).validate(rules, ReplaceRoleFieldRulesCommandDTO.Mutation.class);
            return new ValidationUtils(validator).validate(new PreparedDeploymentInvocationBO(manifest, null, actor, rules, owner, role, expected));
        }
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
        return new ValidationUtils(validator).validate(new PreparedDeploymentInvocationBO(manifest, command, actor, null, owner, null, null));
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    private static class PreparedDeploymentInvocationBO {
        @NotNull @Valid private DeploymentManifestBO manifest;
        @Valid @ConvertGroup(from = Default.class, to = DeploymentCatalogRegistrationCommand.Registration.class)
        private DeploymentCatalogRegistrationCommand command;
        @NotBlank private String actor;
        @Valid @ConvertGroup(from = Default.class, to = ReplaceRoleFieldRulesCommandDTO.Mutation.class)
        private RoleFieldRulesFileDTO fieldRules;
        @NotNull @Valid private ApplicationDeploymentBO owner;
        private Long roleId;
        private Long expectedRoleVersion;
        @JsonIgnore @AssertTrue public boolean isOperationValid() {
            return command != null && fieldRules == null && roleId == null && expectedRoleVersion == null
                    || command == null && fieldRules != null && roleId != null && roleId > 0 && expectedRoleVersion != null && expectedRoleVersion >= 0;
        }
    }

    private static Map<String, String> parse(String[] arguments) {
        if (arguments == null || arguments.length < 1 || !(COMMAND.equals(arguments[0]) || FIELD_COMMAND.equals(arguments[0])) || (arguments.length - 1) % 2 != 0) {
            throw new IllegalArgumentException("Invalid deployment catalog command");
        }
        Map<String, String> result = new LinkedHashMap<>();
        Set<String> permitted = FIELD_COMMAND.equals(arguments[0]) ? FIELD_OPTIONS : OPTIONS;
        for (int index = 1; index < arguments.length; index += 2) {
            String key = arguments[index], value = arguments[index + 1];
            if (!permitted.contains(key) || value == null || value.isBlank() || !value.equals(value.strip()) || result.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("Invalid or duplicate deployment catalog option");
            }
        }
        for (String key : permitted) { if (!key.equals("--manifest")) { required(result, key); } }
        return result;
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) { throw new IllegalArgumentException("Required deployment option is absent"); }
        return value;
    }

    private static Long positiveId(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) { throw new IllegalArgumentException("Deployment identifier must be a positive decimal"); }
        return Long.valueOf(value);
    }

    private static long nonnegativeVersion(String value) {
        if (value == null || !value.matches("0|[1-9][0-9]{0,18}")) { throw new IllegalArgumentException("Deployment version must be nonnegative"); }
        return Long.parseLong(value);
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
        @NotBlank @Size(max = 64) @Pattern(regexp = "[\\x21-\\x7E]{1,64}") private String identitySub;
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
        @NotNull @Size(max = 256) @Valid @Builder.Default private List<@NotNull FieldRuleDeploymentBO> fieldRuleTargets = List.of();
        @JsonIgnore @AssertTrue public boolean isFieldRuleTargetSetValid() {
            return fieldRuleTargets != null && fieldRuleTargets.stream().noneMatch(java.util.Objects::isNull)
                    && fieldRuleTargets.stream().map(FieldRuleDeploymentBO::getRoleId).distinct().count() == fieldRuleTargets.size();
        }
        public ApplicationDeploymentBO(String businessCode, String applicationCode, String buildId, String artifactFile, String artifactSha256, String catalogSha256) {
            this(businessCode, applicationCode, buildId, artifactFile, artifactSha256, catalogSha256, List.of());
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class FieldRuleDeploymentBO {
        @NotNull @jakarta.validation.constraints.Positive private Long roleId;
        @NotNull @Min(0) private Long expectedRoleVersion;
        @NotBlank @Size(max = 4096) private String rulesFile;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String rulesSha256;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class RoleFieldRulesFileDTO {
        @NotNull(groups = ReplaceRoleFieldRulesCommandDTO.Mutation.class)
        @Size(max = 256, groups = ReplaceRoleFieldRulesCommandDTO.Mutation.class)
        @Valid private List<@NotNull(groups = ReplaceRoleFieldRulesCommandDTO.Mutation.class) RoleFieldRuleDTO> rules;
        @JsonIgnore @AssertTrue(groups = ReplaceRoleFieldRulesCommandDTO.Mutation.class)
        public boolean isRuleSetValid() { return new ReplaceRoleFieldRulesCommandDTO().setRules(rules).isRuleSetValid(); }
    }
}
