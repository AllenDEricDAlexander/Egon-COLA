package top.egon.cola.platform.tianquan.shoubing.admin.oauth.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.apache.shardingsphere.driver.yaml.YamlJDBCConfiguration;
import org.apache.shardingsphere.infra.util.yaml.YamlEngine;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.routing.EgonColaPhysicalTargetBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingTopologyValidator;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuthManagedSchemaContractTest {

    private static final String SQL_RESOURCE =
            "db/egon-mp/V20261008_001__initialize_oauth_tenant_schema.sql";
    private static final String MANIFEST_RESOURCE =
            "db/egon-mp/repository-manifest.json";
    private static final String NATIVE_RULES_RESOURCE =
            "oauth-sharding-native.yml";
    private static final List<String> BUSINESS_TABLES = List.of(
            "oauth_identity_user",
            "oauth_identity_credential",
            "oauth_identity_tenant",
            "oauth_identity_membership",
            "oauth_realm",
            "oauth_client",
            "oauth_grant",
            "oauth_authorization",
            "oauth_token",
            "oauth_consent",
            "oauth_signing_key",
            "oauth_security_state",
            "oauth_revocation",
            "oauth_audit",
            "oauth_resource"
    );
    private static final List<String> LEGACY_PUBLIC_TABLES = List.of(
            "identity_user", "identity_user_credential", "identity_tenant",
            "identity_tenant_membership", "identity_client", "identity_client_secret",
            "identity_client_redirect_uri", "identity_client_resource_grant",
            "identity_signing_key", "identity_resource_server", "identity_audit_log",
            "identity_outbox_event"
    );
    private static final List<String> LEGACY_FLYWAY_SCRIPTS = List.of(
            "V1__create_idp_schema.sql",
            "V2__add_oauth_resource_servers.sql",
            "V3__create_transactional_outbox_schema.sql",
            "V4__remove_user_token_version.sql",
            "V5__adopt_client_secrets_and_tenant_authority.sql",
            "V6__backfill_confidential_client_app_ids.sql"
    );
    private static final List<String> PROFILE_KEY_PREFIXES = List.of(
            "spring.flyway.enabled",
            "egon.cola.component.mybatis-plus.sharding.",
            "egon.cola.component.mybatis-plus.ddl.",
            "egon.cola.component.mybatis-plus.tenant-id.",
            "egon.cola.component.transactional-outbox."
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void oneManagedVersionAndRoutesAllTables() throws Exception {
        byte[] sqlBytes = requiredResource(SQL_RESOURCE);
        JsonNode manifest = objectMapper.readTree(requiredResource(MANIFEST_RESOURCE));

        assertEquals("tianquan-oauth", manifest.path("family").asText());
        assertEquals(2, manifest.path("scripts").size());
        JsonNode script = manifest.path("scripts").get(0);
        assertEquals("20261008_001", script.path("version").asText());
        assertEquals(SQL_RESOURCE, script.path("path").asText());
        assertEquals(sha256(sqlBytes), script.path("sha256").asText());
        JsonNode correction = manifest.path("scripts").get(1);
        assertEquals("20261009_001", correction.path("version").asText());
        assertEquals("db/egon-mp/V20261009_001__support_oauth_consent_state.sql", correction.path("path").asText());
        assertEquals(sha256(requiredResource(correction.path("path").asText())), correction.path("sha256").asText());

        String sql = new String(sqlBytes, StandardCharsets.UTF_8);
        Matcher tableMatcher = Pattern.compile(
                "(?im)^\\s*CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([a-z0-9_]+)\\b"
        ).matcher(sql);
        Set<String> actualTables = new LinkedHashSet<>();
        while (tableMatcher.find()) {
            actualTables.add(tableMatcher.group(1).toLowerCase());
        }
        assertEquals(new LinkedHashSet<>(BUSINESS_TABLES), actualTables);
        assertTrue(sql.toLowerCase().contains("deleted_at is null"));
        assertTrue(sql.toLowerCase().contains("deleted_at"));
        assertLegacyFlywayHistoryIsUnchanged();
    }

    @Test
    void nativeRoutesCoverBusinessTechnicalAndExactLegacyTables() throws Exception {
        String nativeYaml = new String(
                requiredResource(NATIVE_RULES_RESOURCE),
                StandardCharsets.UTF_8
        );
        YamlJDBCConfiguration parsed = YamlEngine.unmarshal(
                nativeYaml,
                YamlJDBCConfiguration.class
        );
        assertNotNull(parsed);
        assertTrue(nativeYaml.contains("databaseName: egon"));
        assertTrue(nativeYaml.contains("defaultType: LOCAL"));
        assertFalse(nativeYaml.contains("public.*"));

        for (String table : BUSINESS_TABLES) {
            assertTrue(nativeYaml.contains(
                    "oauth_primary.tianquan_oauth." + table
            ), "Missing NATIVE route for " + table);
        }
        assertTrue(nativeYaml.contains(
                "oauth_primary.egon_outbox.egon_cola_outbox_message"
        ));
        assertTrue(nativeYaml.contains("oauth_primary.egon_outbox.ddl_history"));
        for (String table : LEGACY_PUBLIC_TABLES) {
            assertTrue(nativeYaml.contains("oauth_primary.public." + table),
                    "Missing legacy source route for " + table);
        }
        assertFalse(nativeYaml.contains("identity_client_jwk"));
        assertFalse(nativeYaml.contains("identity_client_audience"));
        assertEquals(12, countMatches(nativeYaml,
                Pattern.compile("oauth_primary\\.public\\.[a-z0-9_]+")));
        assertEquals(15, countMatches(
                nativeYaml,
                Pattern.compile("oauth_primary\\.tianquan_oauth\\.[a-z0-9_]+")
        ));
    }

    @Test
    void nativeTopologyRoutesOutboxToItsSingleManagedSchema() throws Exception {
        EgonColaShardingProperties properties = EgonColaShardingProperties.builder()
                .mode(EgonColaShardingProperties.ModeEnum.SHARDING)
                .configStyle(EgonColaShardingProperties.ConfigStyleEnum.NATIVE)
                .nativeRulesResource("classpath:" + NATIVE_RULES_RESOURCE)
                .dataSources(List.of(new EgonColaShardingProperties.PhysicalDataSourceProperties(
                        "oauth_primary", "oauth_primary", EgonColaShardingProperties.DataSourceRoleEnum.PRIMARY,
                        "org.postgresql.Driver", "jdbc:postgresql://127.0.0.1:5432/topology_only", "topology_only", "")))
                .build();
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var topology = new EgonColaShardingTopologyValidator(new ValidationUtils(validators.getValidator()))
                    .validate(properties, requiredResource(NATIVE_RULES_RESOURCE));

            assertEquals(BUSINESS_TABLES.size() + LEGACY_PUBLIC_TABLES.size() + 2, topology.profiles().size());
            var outbox = topology.profiles().get("egon_cola_outbox_message");
            assertEquals(EgonColaRoutingProfileBO.TableKindEnum.SINGLE, outbox.kind());
            assertEquals(1, outbox.actualNodes().size());
            assertEquals(List.of(new EgonColaPhysicalTargetBO("oauth_primary", "egon_outbox", "egon_cola_outbox_message")),
                    outbox.actualNodes().get(new EgonColaRoutingProfileBO.PartitionKeyBO(0, 0)));
        }
    }

    @Test
    void baseAndLocalPersistenceProfilesHaveTheSameKeyStructure() throws Exception {
        Set<String> base = profileKeys("application.yml");
        Set<String> local = profileKeys("application-local.yml");
        Set<String> basePersistenceKeys = underPrefixes(base, PROFILE_KEY_PREFIXES);
        Set<String> localPersistenceKeys = underPrefixes(local, PROFILE_KEY_PREFIXES);

        assertEquals(basePersistenceKeys, localPersistenceKeys);
        assertTrue(basePersistenceKeys.contains(
                "egon.cola.component.mybatis-plus.sharding.enabled"
        ));
        assertTrue(basePersistenceKeys.contains(
                "egon.cola.component.mybatis-plus.ddl.enabled"
        ));
        assertTrue(basePersistenceKeys.contains(
                "egon.cola.component.transactional-outbox.storage.mp.manifest-resource"
        ));
        assertTrue(basePersistenceKeys.contains(
                "egon.cola.component.transactional-outbox.state-machine.business.enabled"
        ));
    }

    @Test
    void applicationFactoryRunsBusinessDdlBeforeOutboxAndLogicalDataSource()
            throws IOException {
        Path root = repositoryRoot();
        Path configuration = root.resolve(
                "egon-cola-xingyuan/egon-cola-tianquan-shoubing/"
                        + "egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/"
                        + "platform/tianquan/shoubing/admin/oauth/config/"
                        + "OAuthPersistenceConfiguration.java"
        );
        assertTrue(Files.isRegularFile(configuration));
        String source = Files.readString(configuration);
        int oauthDdl = source.indexOf("oauthManagedDdlComponent.initialize");
        int outboxFactory = source.indexOf("new OutboxLogicalDataSourceFactory");
        assertTrue(oauthDdl >= 0);
        assertTrue(outboxFactory > oauthDdl);
        assertFalse(source.contains("YamlShardingSphereDataSourceFactory"));
    }

    @Test
    void scopedDdlConnectionRestoresSchemaAfterSuccessAndRestoreFailure()
            throws Exception {
        DataSource physical = mock(DataSource.class);
        java.sql.Connection connection = mock(java.sql.Connection.class);
        when(physical.getConnection()).thenReturn(connection);
        when(connection.getSchema()).thenReturn("public");
        when(connection.isClosed()).thenReturn(false);

        DataSource scoped = schemaScopedDataSource(physical);
        java.sql.Connection ddlConnection = scoped.getConnection();
        ddlConnection.close();

        var order = inOrder(connection);
        order.verify(connection).getSchema();
        order.verify(connection).setSchema("tianquan_oauth");
        order.verify(connection).setSchema("public");
        order.verify(connection).close();

        DataSource failingPhysical = mock(DataSource.class);
        java.sql.Connection failingConnection = mock(java.sql.Connection.class);
        when(failingPhysical.getConnection()).thenReturn(failingConnection);
        when(failingConnection.getSchema()).thenReturn("public");
        when(failingConnection.isClosed()).thenReturn(false);
        doThrow(new java.sql.SQLException("schema restore failed"))
                .when(failingConnection).setSchema("public");
        java.sql.Connection failingDdlConnection = schemaScopedDataSource(
                failingPhysical
        ).getConnection();

        assertThrows(java.sql.SQLException.class, failingDdlConnection::close);
        org.mockito.Mockito.verify(failingConnection).close();
    }

    private static byte[] requiredResource(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        assertTrue(resource.exists(), "Missing classpath resource: " + path);
        try (var input = resource.getInputStream()) {
            return input.readAllBytes();
        }
    }

    private static String sha256(byte[] content) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content)
        );
    }

    private static int countMatches(String value, Pattern pattern) {
        Matcher matcher = pattern.matcher(value);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static Set<String> profileKeys(String resource) throws IOException {
        ClassPathResource input = new ClassPathResource(resource);
        assertTrue(input.exists(), "Missing profile resource: " + resource);
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(
                resource,
                input
        );
        Set<String> names = new TreeSet<>();
        for (PropertySource<?> source : sources) {
            if (source instanceof EnumerablePropertySource<?> enumerable) {
                names.addAll(Arrays.asList(enumerable.getPropertyNames()));
            }
        }
        return names;
    }

    private static Set<String> underPrefixes(
            Set<String> keys,
            List<String> prefixes
    ) {
        Set<String> result = new TreeSet<>();
        for (String key : keys) {
            if (prefixes.stream().anyMatch(prefix -> key.equals(prefix)
                    || key.startsWith(prefix.endsWith(".")
                    ? prefix
                    : prefix + "."))) {
                result.add(key);
            }
        }
        return result;
    }

    private static void assertLegacyFlywayHistoryIsUnchanged() throws Exception {
        Path root = repositoryRoot();
        List<String> command = new ArrayList<>(List.of(
                "git", "diff", "--exit-code", "HEAD", "--"
        ));
        for (String script : LEGACY_FLYWAY_SCRIPTS) {
            command.add("egon-cola-xingyuan/egon-cola-tianquan-shoubing/"
                    + "egon-cola-tianquan-shoubing-admin/src/main/resources/db/"
                    + "migration/" + script);
        }
        Process process = new ProcessBuilder(command)
                .directory(root.toFile())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        assertEquals(0, process.waitFor(), "Legacy Flyway migration changed");
    }

    private static Path repositoryRoot() {
        Path cursor = Path.of(System.getProperty("user.dir"))
                .toAbsolutePath()
                .normalize();
        while (cursor != null) {
            if (Files.isRegularFile(cursor.resolve("pom.xml"))
                    && Files.isDirectory(cursor.resolve("egon-cola-xingyuan"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("Repository root was not found");
    }

    private static DataSource schemaScopedDataSource(DataSource delegate)
            throws Exception {
        Class<?> type = Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.config."
                        + "OAuthManagedDdlComponent$SchemaScopedDataSource"
        );
        Constructor<?> constructor = type.getDeclaredConstructor(
                DataSource.class,
                String.class
        );
        constructor.setAccessible(true);
        return (DataSource) constructor.newInstance(delegate, "tianquan_oauth");
    }
}
