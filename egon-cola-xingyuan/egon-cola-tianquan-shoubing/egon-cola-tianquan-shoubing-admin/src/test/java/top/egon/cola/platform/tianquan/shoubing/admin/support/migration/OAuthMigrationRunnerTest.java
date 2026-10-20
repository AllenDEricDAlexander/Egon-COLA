package top.egon.cola.platform.tianquan.shoubing.admin.support.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.result.OAuthMigrationReportResult;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthMigrationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class OAuthMigrationRunnerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void normalPropertyScanningDoesNotRequireMigrationOnlyInputs() {
        new ApplicationContextRunner().withUserConfiguration(MigrationPropertyScanning.class)
                .run(context -> assertThat(context).hasNotFailed()
                        .doesNotHaveBean(OAuthMigrationRunner.MigrationProperties.class));
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    @org.springframework.boot.context.properties.ConfigurationPropertiesScan(basePackageClasses = OAuthMigrationRunner.class)
    static class MigrationPropertyScanning { }

    @Test
    void qualifiedRunnerDependenciesResolveWithoutLoadingTheApplication() {
        new ApplicationContextRunner().withUserConfiguration(RunnerWiring.class)
                .withPropertyValues(
                        "spring.profiles.active=oauth-migration",
                        "egon.tianquan-shoubing.oauth.migration.enabled=false",
                        "egon.tianquan-shoubing.oauth.migration.realm-key=platform",
                        "egon.tianquan-shoubing.oauth.migration.source-snapshot-id=snapshot-1",
                        "egon.tianquan-shoubing.oauth.migration.dry-run=true",
                        "egon.tianquan-shoubing.oauth.migration.freeze-marker-file=" + temporaryDirectory.resolve("freeze"),
                        "egon.tianquan-shoubing.oauth.migration.ownership-map-file=" + temporaryDirectory.resolve("owners"),
                        "egon.tianquan-shoubing.oauth.migration.checkpoint-file=" + temporaryDirectory.resolve("checkpoint"),
                        "egon.tianquan-shoubing.oauth.migration.secret-distribution-directory=" + temporaryDirectory,
                        "egon.tianquan-shoubing.oauth.migration.report-file=" + temporaryDirectory.resolve("report"))
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(OAuthMigrationRunner.class));
    }

    @Test
    void runnerIsAvailableOnlyInTheExplicitMigrationProfile() throws Exception {
        Class<?> runner = load(
                "top.egon.cola.platform.tianquan.shoubing.admin.support.migration.OAuthMigrationRunner"
        );

        assertThat(ApplicationRunner.class.isAssignableFrom(runner)).isTrue();
        assertThat(runner.getAnnotation(Profile.class)).isNotNull();
        assertThat(runner.getAnnotation(Profile.class).value()).containsExactly("oauth-migration");
    }

    @Test
    void runnerRejectsDefaultOrUnsafeInvocationBeforeServiceWork() throws Exception {
        Class<?> runner = load(
                "top.egon.cola.platform.tianquan.shoubing.admin.support.migration.OAuthMigrationRunner"
        );

        assertThat(runner.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .contains("properties", "environment", "migrationService", "objectMapper");
        assertThat(runner.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .contains("run", "validateInvocation");
    }

    @Test
    void migrationProfileMustBeLastWhenLocalLoopbackCallbacksAreEnabled() throws Exception {
        Environment environment = org.mockito.Mockito.mock(Environment.class);
        OAuthMigrationRunner.MigrationProperties properties = new OAuthMigrationRunner.MigrationProperties(
                false, null, null, true, null, null, null, null, null, null);
        OAuthMigrationRunner runner = new OAuthMigrationRunner(environment, properties,
                org.mockito.Mockito.mock(OAuthMigrationService.class), new ObjectMapper(), mock(ConfigurableApplicationContext.class));
        java.lang.reflect.Method method = OAuthMigrationRunner.class.getDeclaredMethod("validMigrationProfiles");
        method.setAccessible(true);

        org.mockito.Mockito.when(environment.getActiveProfiles()).thenReturn(new String[]{"local", "oauth-migration"});
        assertThat(method.invoke(runner)).isEqualTo(true);
        org.mockito.Mockito.when(environment.getActiveProfiles()).thenReturn(new String[]{"oauth-migration", "local"});
        assertThat(method.invoke(runner)).isEqualTo(false);
        org.mockito.Mockito.when(environment.getActiveProfiles()).thenReturn(new String[]{"oauth-migration", "test"});
        assertThat(method.invoke(runner)).isEqualTo(false);
    }

    @Test
    void writesOwnerOnlyJsonReportWithTheDigestNeededForApply() throws Exception {
        Path evidence = temporaryDirectory.resolve("evidence");
        Files.createDirectory(evidence);
        setPermissions(evidence, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE);
        Path secretDirectory = evidence.resolve("secrets");
        Files.createDirectory(secretDirectory);
        setPermissions(secretDirectory, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE);
        Path freezeMarker = evidence.resolve("freeze.marker");
        Path ownershipMap = evidence.resolve("owners.json");
        writeOwnerOnly(freezeMarker, "realm=platform\nsnapshot=snapshot-1\n");
        writeOwnerOnly(ownershipMap, "{\"schemaVersion\":1,\"sourceSnapshotId\":\"snapshot-1\",\"assignments\":[]}");
        Path reportFile = evidence.resolve("dry-run-report.json");

        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"oauth-migration"});
        when(environment.getProperty("spring.main.web-application-type")).thenReturn("none");
        when(environment.getProperty("egon.cola.component.rpc.enabled", Boolean.class)).thenReturn(false);
        when(environment.getProperty("egon.cola.component.tianshu.registry.http.enabled", Boolean.class))
                .thenReturn(false);
        OAuthMigrationService migrationService = mock(OAuthMigrationService.class);
        String reportDigest = "a".repeat(64);
        when(migrationService.importRealm(any())).thenReturn(OAuthMigrationReportResult.builder()
                .realmKey("platform")
                .sourceSnapshotId("snapshot-1")
                .dryRun(true)
                .reportSha256(reportDigest)
                .sourceCount(12L)
                .conflictCount(0L)
                .conflictCodes(List.of())
                .build());
        OAuthMigrationRunner.MigrationProperties properties = new OAuthMigrationRunner.MigrationProperties(
                true, "platform", "snapshot-1", true, freezeMarker, ownershipMap,
                evidence.resolve("checkpoint.json"), secretDirectory, reportFile, null);

        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        OAuthMigrationRunner runner = new OAuthMigrationRunner(environment, properties,
                migrationService, new ObjectMapper(), context);
        runner.run(new DefaultApplicationArguments(new String[0]));

        assertThat(Files.getPosixFilePermissions(reportFile))
                .containsExactlyInAnyOrder(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
        assertThat(new ObjectMapper().readTree(reportFile.toFile()).path("reportSha256").asText())
                .isEqualTo(reportDigest);
        assertThat(Files.readString(reportFile)).doesNotContain("clientSecret", "refreshToken", "passwordHash");
        verify(migrationService).importRealm(any());
        verify(context).close();
    }

    @Test
    void refusesToOverwriteAnExistingReportBeforeCallingTheImporter() throws Exception {
        Path evidence = temporaryDirectory.resolve("occupied-report");
        Files.createDirectory(evidence);
        setPermissions(evidence, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE);
        Path secretDirectory = evidence.resolve("secrets");
        Files.createDirectory(secretDirectory);
        setPermissions(secretDirectory, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE);
        Path freezeMarker = evidence.resolve("freeze.marker");
        Path ownershipMap = evidence.resolve("owners.json");
        writeOwnerOnly(freezeMarker, "realm=platform\nsnapshot=snapshot-1\n");
        writeOwnerOnly(ownershipMap, "{\"schemaVersion\":1,\"sourceSnapshotId\":\"snapshot-1\",\"assignments\":[]}");
        Path reportFile = evidence.resolve("previous-report.json");
        writeOwnerOnly(reportFile, "preserve-me");
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"oauth-migration"});
        when(environment.getProperty("spring.main.web-application-type")).thenReturn("none");
        when(environment.getProperty("egon.cola.component.rpc.enabled", Boolean.class)).thenReturn(false);
        when(environment.getProperty("egon.cola.component.tianshu.registry.http.enabled", Boolean.class))
                .thenReturn(false);
        OAuthMigrationService migrationService = mock(OAuthMigrationService.class);
        OAuthMigrationRunner.MigrationProperties properties = new OAuthMigrationRunner.MigrationProperties(
                true, "platform", "snapshot-1", true, freezeMarker, ownershipMap,
                evidence.resolve("checkpoint.json"), secretDirectory, reportFile, null);
        OAuthMigrationRunner runner = new OAuthMigrationRunner(environment, properties,
                migrationService, new ObjectMapper(), mock(ConfigurableApplicationContext.class));

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("report file must be a new path");
        assertThat(Files.readString(reportFile)).isEqualTo("preserve-me");
        verifyNoInteractions(migrationService);
    }

    private static void writeOwnerOnly(Path path, String content) throws Exception {
        Files.writeString(path, content);
        setPermissions(path, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
    }

    private static void setPermissions(Path path, PosixFilePermission... permissions) throws Exception {
        Files.setPosixFilePermissions(path, Set.of(permissions));
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("Missing planned migration entrypoint: " + name, exception);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(OAuthMigrationRunner.class)
    static class RunnerWiring {
        @Bean("oauthMigrationService")
        OAuthMigrationService migrationService() {
            return mock(OAuthMigrationService.class);
        }

        @Bean("jacksonObjectMapper")
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
