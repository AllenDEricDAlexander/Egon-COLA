package top.egon.cola.platform.rbac3.admin.bootstrap.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class Rbac3DevelopmentResourceBootstrapTest {

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
