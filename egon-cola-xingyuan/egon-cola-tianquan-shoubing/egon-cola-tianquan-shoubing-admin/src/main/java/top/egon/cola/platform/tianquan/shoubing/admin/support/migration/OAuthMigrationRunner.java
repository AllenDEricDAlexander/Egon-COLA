package top.egon.cola.platform.tianquan.shoubing.admin.support.migration;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.Arrays;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.command.OAuthMigrationCommand;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.result.OAuthMigrationReportResult;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthMigrationService;

/** Profile-only adapter from explicit command-line properties to the migration use case. */
@Component("oauthMigrationRunner")
@Profile("oauth-migration")
@EnableConfigurationProperties(OAuthMigrationRunner.MigrationProperties.class)
@RequiredArgsConstructor
@Slf4j
public class OAuthMigrationRunner implements ApplicationRunner {

    private static final String PROFILE = "oauth-migration";
    private static final String RPC_ENABLED = "egon.cola.component.rpc.enabled";
    private static final String HTTP_REGISTRATION_ENABLED = "egon.cola.component.tianshu.registry.http.enabled";

    @Qualifier("environment")
    private final Environment environment;
    @Qualifier("egon.tianquan-shoubing.oauth.migration-top.egon.cola.platform.tianquan.shoubing.admin.support.migration.OAuthMigrationRunner$MigrationProperties")
    private final MigrationProperties properties;
    @Qualifier("oauthMigrationService")
    private final OAuthMigrationService migrationService;
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;
    @Qualifier("applicationContext")
    private final ConfigurableApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments arguments) {
        validateInvocation();
        OAuthMigrationReportResult report = migrationService.importRealm(OAuthMigrationCommand.builder()
                .realmKey(properties.realmKey())
                .sourceSnapshotId(properties.sourceSnapshotId())
                .dryRun(properties.dryRun())
                .freezeMarkerFile(properties.freezeMarkerFile())
                .ownershipMapFile(properties.ownershipMapFile())
                .checkpointFile(properties.checkpointFile())
                .secretDistributionDirectory(properties.secretDistributionDirectory())
                .approvedReportSha256(properties.approvedReportSha256())
                .build());
        writeReport(report);
        log.info("OAuth migration report written: realmKey={}, snapshot={}, dryRun={}, sourceCount={}, importedCount={}, "
                        + "skippedCount={}, conflictCount={}, reportSha256={}, checkpoint={}, file={}",
                report.getRealmKey(),
                report.getSourceSnapshotId(),
                report.isDryRun(),
                report.getSourceCount(),
                report.getImportedCount(),
                report.getSkippedCount(),
                report.getConflictCount(),
                report.getReportSha256(),
                report.getCheckpointId(),
                properties.reportFile());
        applicationContext.close();
    }

    void validateInvocation() {
        require(properties.enabled(), "migration must be explicitly enabled");
        require(validMigrationProfiles(),
                "migration profile must be last; only migration or local,migration profiles are allowed");
        require("none".equalsIgnoreCase(environment.getProperty("spring.main.web-application-type")),
                "migration requires non-web application mode");
        require(Boolean.FALSE.equals(environment.getProperty(RPC_ENABLED, Boolean.class)),
                "RPC must be disabled in migration mode");
        require(Boolean.FALSE.equals(environment.getProperty(HTTP_REGISTRATION_ENABLED, Boolean.class)),
                "Tianshu HTTP registration must be disabled in migration mode");
        require(nonBlank(properties.realmKey()), "realm key is required");
        require(nonBlank(properties.sourceSnapshotId()), "source snapshot id is required");
        require(properties.freezeMarkerFile() != null, "freeze marker file is required");
        require(properties.ownershipMapFile() != null, "client ownership map file is required");
        require(properties.checkpointFile() != null, "checkpoint file is required");
        require(properties.secretDistributionDirectory() != null, "secret distribution directory is required");
        require(properties.reportFile() != null, "report file is required");
        require(properties.dryRun() || nonBlank(properties.approvedReportSha256()),
                "apply requires an approved dry-run report SHA-256");
        require(validFreezeMarker(), "freeze marker does not match the requested realm and snapshot");
        require(validProtectedFile(properties.ownershipMapFile()), "ownership map must be an owner-only regular file");
        require(validProtectedPath(properties.checkpointFile(), false), "checkpoint path must be absolute and protected");
        require(validProtectedPath(properties.secretDistributionDirectory(), true),
                "secret distribution directory must be absolute and owner-only");
        require(validNewReportPath(properties.reportFile()),
                "report file must be a new path in an owner-only directory");
    }

    private boolean validFreezeMarker() {
        Path marker = properties.freezeMarkerFile();
        if (marker == null || !marker.isAbsolute() || Files.isSymbolicLink(marker)
                || !Files.isRegularFile(marker, LinkOption.NOFOLLOW_LINKS) || !hasOwnerOnlyPermissions(marker)) {
            return false;
        }
        try {
            String expected = "realm=" + properties.realmKey() + "\nsnapshot=" + properties.sourceSnapshotId() + "\n";
            return expected.equals(Files.readString(marker));
        } catch (IOException exception) {
            return false;
        }
    }

    private boolean validMigrationProfiles() {
        String[] activeProfiles = environment.getActiveProfiles();
        return Arrays.equals(activeProfiles, new String[]{PROFILE})
                || Arrays.equals(activeProfiles, new String[]{"local", PROFILE});
    }

    private static boolean validProtectedFile(Path path) {
        return path != null && path.isAbsolute() && !Files.isSymbolicLink(path)
                && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) && hasOwnerOnlyPermissions(path);
    }

    private static boolean validProtectedPath(Path path, boolean directory) {
        if (path == null || !path.isAbsolute() || Files.isSymbolicLink(path)) {
            return false;
        }
        boolean correctType = directory
                ? Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)
                : !Files.exists(path, LinkOption.NOFOLLOW_LINKS)
                || Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS);
        return correctType && (!Files.exists(path, LinkOption.NOFOLLOW_LINKS) || hasOwnerOnlyPermissions(path));
    }

    private static boolean validNewReportPath(Path path) {
        if (path == null || !path.isAbsolute() || Files.isSymbolicLink(path)
                || Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        Path parent = path.getParent();
        return parent != null && Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)
                && !Files.isSymbolicLink(parent) && hasOwnerOnlyPermissions(parent);
    }

    private void writeReport(OAuthMigrationReportResult report) {
        Path path = properties.reportFile();
        if (!validNewReportPath(path)) {
            throw new IllegalStateException("migration report path is not a new owner-only file");
        }
        Path temporary = null;
        try {
            byte[] json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(report);
            temporary = Files.createTempFile(path.getParent(), ".oauth-migration-report-", ".tmp");
            Files.setPosixFilePermissions(temporary, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE));
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                channel.write(ByteBuffer.wrap(json));
                channel.force(true);
            }
            Files.createLink(path, temporary);
            Files.delete(temporary);
            temporary = null;
            try (FileChannel directory = FileChannel.open(path.getParent(), StandardOpenOption.READ)) {
                directory.force(true);
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("migration report serialization failed", exception);
        } catch (IOException | UnsupportedOperationException exception) {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException cleanupException) {
                    log.warn("Could not remove temporary OAuth migration report file");
                }
            }
            throw new IllegalStateException("migration report write failed", exception);
        }
    }

    private static boolean hasOwnerOnlyPermissions(Path path) {
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS);
            Set<PosixFilePermission> ownerOnlyFile = Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
            );
            Set<PosixFilePermission> ownerOnlyDirectory = Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE
            );
            return permissions.equals(ownerOnlyFile) || permissions.equals(ownerOnlyDirectory);
        } catch (IOException | UnsupportedOperationException exception) {
            return false;
        }
    }

    private static boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    @Validated
    @Profile("oauth-migration")
    @ConfigurationProperties(prefix = "egon.tianquan-shoubing.oauth.migration")
    public record MigrationProperties(
            boolean enabled,
            @NotBlank @Pattern(regexp = "(?:platform|tenant:[1-9][0-9]{0,18})") String realmKey,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9._:-]{1,118}") String sourceSnapshotId,
            boolean dryRun,
            @NotNull Path freezeMarkerFile,
            @NotNull Path ownershipMapFile,
            @NotNull Path checkpointFile,
            @NotNull Path secretDistributionDirectory,
            @NotNull Path reportFile,
            String approvedReportSha256
    ) {
    }
}
