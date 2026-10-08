package top.egon.cola.platform.tianquan.shoubing.admin.identity.repo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.egon.cola.platform.tianquan.shoubing.core.identity.PasswordCredential;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityMpPersistenceContractTest {

    private static final long PLATFORM_PARTITION_ID = 9_001L;
    private static final String TENANT_MDC_KEY = "tenantId";
    private static final List<String> DIRECTORY_MAPPERS = List.of(
            "mapper/identity/IdentityUserDAO.xml",
            "mapper/identity/IdentityCredentialDAO.xml",
            "mapper/tenant/IdentityTenantDAO.xml",
            "mapper/tenant/IdentityTenantMembershipDAO.xml"
    );

    @Test
    void trustedDirectoryScopeRestoresMdcAfterSuccessAndFailure() throws Exception {
        Class<?> contextType = Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.IdentityDirectoryContext"
        );
        Constructor<?> constructor = contextType.getConstructor(long.class, String.class);
        Object context = constructor.newInstance(PLATFORM_PARTITION_ID, TENANT_MDC_KEY);
        Method withPartition = contextType.getMethod(
                "withPlatformDirectoryPartition",
                Supplier.class
        );

        MDC.put(TENANT_MDC_KEY, "31007");
        MDC.put("userId", "operator-7");
        try {
            Object actual = withPartition.invoke(context, (Supplier<String>) () ->
                    MDC.get(TENANT_MDC_KEY) + "|" + MDC.get("userId"));
            assertEquals(PLATFORM_PARTITION_ID + "|operator-7", actual);
            assertEquals("31007", MDC.get(TENANT_MDC_KEY));
            assertEquals("operator-7", MDC.get("userId"));

            InvocationTargetException failure = assertThrows(
                    InvocationTargetException.class,
                    () -> withPartition.invoke(context, (Supplier<String>) () -> {
                        assertEquals(Long.toString(PLATFORM_PARTITION_ID),
                                MDC.get(TENANT_MDC_KEY));
                        assertEquals("operator-7", MDC.get("userId"));
                        throw new IllegalStateException("expected directory failure");
                    })
            );
            assertInstanceOf(IllegalStateException.class, failure.getCause());
            assertEquals("31007", MDC.get(TENANT_MDC_KEY));
            assertEquals("operator-7", MDC.get("userId"));

            MDC.remove("userId");
            Object fallbackActor = withPartition.invoke(context, (Supplier<String>) () ->
                    MDC.get("userId"));
            assertEquals("service:tianquan-shoubing", fallbackActor);
            assertNull(MDC.get("userId"));
        } finally {
            MDC.remove(TENANT_MDC_KEY);
            MDC.remove("userId");
        }
    }

    @Test
    void allFourDirectoryMappersUseTheTrustedPartitionAndActiveRows() throws IOException {
        Path root = repositoryRoot();
        for (String mapper : DIRECTORY_MAPPERS) {
            Path path = root.resolve(
                    "egon-cola-xingyuan/egon-cola-tianquan-shoubing/"
                            + "egon-cola-tianquan-shoubing-admin/src/main/resources/"
                            + mapper
            );
            assertTrue(Files.isRegularFile(path), "Missing mapper: " + mapper);
            String xml = Files.readString(path).toLowerCase();
            assertTrue(xml.contains("tenant_id"), "Missing trusted partition predicate: " + mapper);
            assertTrue(xml.contains("deleted_at is null"), "Missing active-row predicate: " + mapper);
            assertTrue(xml.contains("updateexpectedversion"), "Missing optimistic update: " + mapper);
        }
    }

    @Test
    void identityAdapterNoLongerDependsOnJpa() throws IOException {
        Path source = repositoryRoot().resolve(
                "egon-cola-xingyuan/egon-cola-tianquan-shoubing/"
                        + "egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/"
                        + "platform/tianquan/shoubing/admin/identity/repo/IdentityPersistenceAdapter.java"
        );
        assertTrue(Files.isRegularFile(source));
        String java = Files.readString(source);

        assertFalse(java.contains("EntityManager"));
        assertFalse(java.contains("IdentityUserEntity"));
        assertFalse(java.contains("IdentityCredentialEntity"));
        assertTrue(java.contains("IdentityUserRepository"));
        assertTrue(java.contains("IdentityCredentialRepository"));
    }

    @Test
    void platformPartitionIsRequiredAndSharedByBothProfiles() throws IOException {
        String property = "identity-directory:\n"
                + "      platform-storage-partition-id: "
                + "${EGON_TIANQUAN_SHOUBING_PLATFORM_STORAGE_PARTITION_ID}";
        for (String profile : List.of("application.yml", "application-local.yml")) {
            String yaml = Files.readString(repositoryRoot()
                    .resolve("egon-cola-xingyuan/egon-cola-tianquan-shoubing/"
                            + "egon-cola-tianquan-shoubing-admin/src/main/resources/")
                    .resolve(profile));
            assertTrue(yaml.contains(property), "Missing required P property in " + profile);
            assertFalse(yaml.contains("platform-storage-partition-id: "
                    + "${EGON_TIANQUAN_SHOUBING_PLATFORM_STORAGE_PARTITION_ID:"),
                    "P must not have a default in " + profile);
        }
    }

    @Test
    void platformPartitionRejectsNonPositiveValues() throws Exception {
        Class<?> contextType = Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.IdentityDirectoryContext"
        );
        Constructor<?> constructor = contextType.getConstructor(long.class, String.class);

        InvocationTargetException failure = assertThrows(
                InvocationTargetException.class,
                () -> constructor.newInstance(0L, TENANT_MDC_KEY)
        );
        assertInstanceOf(IllegalArgumentException.class, failure.getCause());
    }

    @Test
    void mpNamedClockAliasUsesTheIdpClockInstance() throws IOException {
        Path configuration = repositoryRoot().resolve(
                "egon-cola-xingyuan/egon-cola-tianquan-shoubing/"
                        + "egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/"
                        + "platform/tianquan/shoubing/admin/oauth/config/OAuthPersistenceConfiguration.java"
        );
        String source = Files.readString(configuration);

        assertTrue(source.contains("@Bean(\"egonColaMybatisPlusClock\")"));
        assertTrue(source.contains("@Qualifier(\"idpClock\") Clock idpClock"));
        assertTrue(source.contains("return idpClock;"));
        assertFalse(source.contains("Clock.systemUTC()"));
    }

    @Test
    void passwordValueObjectRedactsHashAndSubjectFromToString() {
        String subject = "subject-that-must-not-be-logged";
        String hash = "{argon2}encoded-secret-hash";
        PasswordCredential credential = new PasswordCredential(
                subject, hash, java.time.Instant.EPOCH, false,
                PasswordCredential.Status.ACTIVE, 0L
        );

        assertFalse(credential.toString().contains(hash));
        assertFalse(credential.toString().contains(subject));
    }

    @Test
    void tenantSettingsUseJsonbObjectBinding() throws Exception {
        Class<?> handlerType = Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.admin.tenant.dao.IdentityTenantSettingsJsonbTypeHandler"
        );
        Constructor<?> constructor = handlerType.getConstructor();
        Object handler = constructor.newInstance();
        Method setValue = handlerType.getDeclaredMethod(
                "setNonNullParameter", PreparedStatement.class, int.class, JsonNode.class, JdbcType.class
        );
        setValue.setAccessible(true);
        PreparedStatement statement = mock(PreparedStatement.class);
        JsonNode settings = new ObjectMapper().readTree("{\"region\":\"cn\"}");

        setValue.invoke(handler, statement, 1, settings, JdbcType.OTHER);
        verify(statement).setObject(1, settings.toString(), Types.OTHER);

        Method readValue = handlerType.getDeclaredMethod(
                "getNullableResult", ResultSet.class, String.class
        );
        readValue.setAccessible(true);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("settings")).thenReturn(settings.toString());
        JsonNode actual = (JsonNode) readValue.invoke(handler, resultSet, "settings");
        assertTrue(actual.isObject());
        assertEquals("cn", actual.path("region").asText());

        when(resultSet.getString("settings")).thenReturn("[1]");
        InvocationTargetException invalidRoot = assertThrows(
                InvocationTargetException.class,
                () -> readValue.invoke(handler, resultSet, "settings")
        );
        assertInstanceOf(SQLException.class, invalidRoot.getCause());
    }

    @Test
    void fourPersistenceModelsAndTheDirectoryContextAreInTheAdminModule() throws Exception {
        List<String> types = List.of(
                "top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.po.IdentityUserPO",
                "top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.po.IdentityCredentialPO",
                "top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.po.IdentityTenantPO",
                "top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.po.IdentityTenantMembershipPO"
        );
        for (String type : types) {
            assertNotNull(Class.forName(type));
        }
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
}
