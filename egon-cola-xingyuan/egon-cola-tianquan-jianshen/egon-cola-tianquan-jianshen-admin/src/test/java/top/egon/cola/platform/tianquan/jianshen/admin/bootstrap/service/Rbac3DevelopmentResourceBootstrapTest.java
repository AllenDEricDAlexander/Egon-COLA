package top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Rbac3DevelopmentResourceBootstrapTest {

    @Test
    void adminGrantsAreLimitedToMappedBootstrapCapabilitiesAndGrantableResourceTypes() {
        EntityManager entityManager = mock(EntityManager.class);
        Query resources = mock(Query.class, RETURNS_SELF);
        Query roles = mock(Query.class, RETURNS_SELF);
        when(resources.getResultList()).thenReturn(List.of());
        when(roles.getResultList()).thenReturn(List.of());
        when(entityManager.createNativeQuery(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("from rbac3_resource")) {
                assertThat(sql).contains(
                        "p.id = r.required_permission_id",
                        "p.application_id = r.application_id",
                        "r.status = 'ACTIVE' and p.status = 'ACTIVE'",
                        "r.resource_type in ('ROUTE', 'ACTION', 'API')",
                        "p.id in (:permissionIds)",
                        "r.source_build_id = :buildId");
                return resources;
            }
            assertThat(sql).contains("from rbac3_role");
            return roles;
        });
        var bootstrap = new Rbac3DevelopmentResourceBootstrap(
                entityManager, new ObjectMapper(), mock(LongIdGenerator.class));

        ReflectionTestUtils.invokeMethod(bootstrap, "ensureLocalAdminResourceGrants",
                Instant.parse("2026-09-06T05:00:00Z"), Set.of(101L, 102L));

        verify(resources).setParameter("permissionIds", Set.of(101L, 102L));
        verify(resources).setParameter("buildId", "local-rbac3-resource-catalog-v1");
    }

    @Test
    void localCatalogContainsUniqueResourceCodesAndCompleteRouteBindings() throws IOException {
        JsonNode definitions = new ObjectMapper().readTree(
                new ClassPathResource(
                        Rbac3DevelopmentResourceBootstrap.RESOURCE_DEFINITIONS)
                        .getInputStream());

        assertThat(definitions.isArray()).isTrue();
        Set<String> resourceKeys = new HashSet<>();
        for (JsonNode definition : definitions) {
            String kind = definition.path("kind").asText();
            String code = definition.path("code").asText();
            assertThat(resourceKeys.add(kind + ':' + code))
                    .as("duplicate local resource %s:%s", kind, code)
                    .isTrue();
            if ("ROUTE".equals(kind)) {
                assertThat(definition.path("path").asText()).startsWith("/");
                assertThat(definition.path("componentKey").asText()).isNotBlank();
            }
            if ("ACTION".equals(kind)) {
                assertThat(definition.path("routeCode").asText()).isNotBlank();
            }
        }
        assertThat(resourceKeys).contains("MENU:iam", "ROUTE:iam.users");
    }

    @Test
    void resourceBootstrapIsIndependentFromTopologyBootstrap() {
        ConditionalOnProperty[] conditions = Rbac3DevelopmentResourceBootstrap.class
                .getAnnotationsByType(ConditionalOnProperty.class);
        assertThat(conditions).hasSize(1);
        assertThat(conditions[0].name())
                .containsExactly("auto-activate-local-admin-roles");
    }
}
