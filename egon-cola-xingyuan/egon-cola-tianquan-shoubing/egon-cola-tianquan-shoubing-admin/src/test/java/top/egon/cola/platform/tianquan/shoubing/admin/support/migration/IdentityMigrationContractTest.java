package top.egon.cola.platform.tianquan.shoubing.admin.support.migration;

import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityMigrationContractTest {

    private static final String RUNNER_CLASS =
            "top.egon.cola.platform.tianquan.shoubing.admin.support.migration.OAuthMigrationRunner";
    private static final String PLATFORM_CONFIGURATION_CLASS =
            "top.egon.cola.platform.tianquan.shoubing.admin.support.runtime.IdpPlatformConfiguration";

    @Test
    void sourceCredentialHashCannotBeSerializedOrLogged() throws Exception {
        var row = top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO
                .CredentialRowDTO.builder().id("credential-1").passwordHash("private-test-hash").build();
        assertThat(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(row))
                .doesNotContain("passwordHash", "private-test-hash");
        assertThat(row.toString()).doesNotContain("private-test-hash");
    }

    @Test
    void dedicatedMigrationProfileIsNonWebAndDisabledByDefault() throws Exception {
        Path profilePath = Path.of("src/main/resources/application-oauth-migration.yml");
        assertThat(Files.exists(profilePath)).isTrue();
        String profile = Files.readString(profilePath);

        assertThat(profile).contains("spring:", "web-application-type:", "none");
        assertThat(profile).contains("enabled:", "false");
        assertThat(profile).contains("rpc:", "registry:", "http:");
        assertThat(profile).doesNotContain("server.port", "jdbc:", "password:", "secret:");

        Class<?> platformConfiguration = load(PLATFORM_CONFIGURATION_CLASS);
        Profile platformProfile = platformConfiguration.getAnnotation(Profile.class);
        assertThat(platformProfile).isNotNull();
        assertThat(platformProfile.value()).containsExactly("!oauth-migration");
    }

    @Test
    void legacyMapperIsStaticAndCoversOnlyTheReviewedSourceTables() throws Exception {
        Path mapperPath = Path.of("src/main/resources/mapper/oauth/IdentityMigrationDAO.xml");
        assertThat(Files.exists(mapperPath)).isTrue();
        String mapper = Files.readString(mapperPath);

        assertThat(mapper).contains(
                "public.identity_user",
                "public.identity_user_credential",
                "public.identity_tenant",
                "public.identity_tenant_membership",
                "public.identity_client",
                "public.identity_client_secret",
                "public.identity_client_redirect_uri",
                "public.identity_client_resource_grant",
                "public.identity_resource_server",
                "public.identity_signing_key",
                "public.identity_audit_log",
                "public.identity_outbox_event"
        );
        assertThat(mapper).doesNotContain("public.*", "${table}", "${sourceTable}", "secret_hash",
                "encrypted_private_key");
        assertThat(mapper).contains("LIMIT", "lastSourceId");
        try (InputStream input = Files.newInputStream(mapperPath)) {
            Configuration configuration = new Configuration();
            for (String dependency : new String[]{
                    "src/main/resources/mapper/tenant/IdentityTenantDAO.xml",
                    "src/main/resources/mapper/audit/OAuthAuditMapper.xml"}) {
                try (InputStream dependencyInput = Files.newInputStream(Path.of(dependency))) {
                    new XMLMapperBuilder(dependencyInput, configuration, dependency,
                            configuration.getSqlFragments()).parse();
                }
            }
            new XMLMapperBuilder(input, configuration, mapperPath.toString(), configuration.getSqlFragments()).parse();
            assertThat(configuration.hasMapper(load(
                    "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.IdentityMigrationDAO"))).isTrue();
            String namespace = "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.IdentityMigrationDAO.";
            assertThat(configuration.getMappedStatement(namespace + "selectTenantByExternalId")
                    .getResultMaps().getFirst().getResultMappings().stream()
                    .filter(mapping -> "settings".equals(mapping.getProperty())).findFirst().orElseThrow()
                    .getTypeHandler().getClass().getSimpleName())
                    .isEqualTo("IdentityTenantSettingsJsonbTypeHandler");
            assertThat(configuration.getMappedStatement(namespace + "selectAuditByEventId")
                    .getResultMaps().getFirst().getResultMappings().stream()
                    .filter(mapping -> "payload".equals(mapping.getProperty())).findFirst().orElseThrow()
                    .getTypeHandler().getClass().getSimpleName()).isEqualTo("JacksonTypeHandler");
        }
        assertThat(mapper).contains("CAST(#{row.payload", " AS jsonb)");

        Class<?> migrationDao = load(
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.IdentityMigrationDAO");
        assertThat(migrationDao.getDeclaredMethods()).isNotEmpty();
        for (java.lang.reflect.Method method : migrationDao.getDeclaredMethods()) {
            InterceptorIgnore ignore = method.getAnnotation(InterceptorIgnore.class);
            assertThat(ignore).as(method.getName()).isNotNull();
            assertThat(ignore.tenantLine()).as(method.getName()).isEqualTo("true");
            if (method.getName().startsWith("insert")) {
                org.springframework.validation.annotation.Validated validation = method.getAnnotation(
                        org.springframework.validation.annotation.Validated.class);
                assertThat(validation).as(method.getName()).isNotNull();
                assertThat(validation.value()).containsExactly(
                        top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups.Persisted.class);
            }
        }
    }

    @Test
    void migrationCommandAndRunnerAreProfileScoped() throws Exception {
        Class<?> runner = load(RUNNER_CLASS);
        assertThat(runner.getAnnotation(Profile.class)).isNotNull();
        assertThat(runner.getAnnotation(Profile.class).value()).containsExactly("oauth-migration");
        assertThat(runner.getInterfaces())
                .extracting(Class::getName)
                .contains("org.springframework.boot.ApplicationRunner");
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("Missing planned migration contract: " + name, exception);
        }
    }
}
