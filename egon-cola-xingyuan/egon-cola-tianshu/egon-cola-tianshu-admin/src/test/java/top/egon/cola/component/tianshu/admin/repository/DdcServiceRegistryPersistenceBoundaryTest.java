package top.egon.cola.component.tianshu.admin.repository;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.tianshu.model.registry.DdcServiceInstance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DdcServiceRegistryPersistenceBoundaryTest {

    @Test
    void serviceRegistryIsRedisOnlyAndDoesNotIntroduceJpaTables() throws IOException {
        assertThat(hasJpaEntityAnnotation(DdcServiceInstance.class)).isFalse();
        assertThat(hasJpaEntityAnnotation(DdcServiceRegistryRedisRepository.class)).isFalse();

        for (String dialect : List.of("postgresql", "sqlite")) {
            String schema = resource("db/" + dialect + "/V1__create_ddc_schema.sql")
                    + resource("db/" + dialect + "/V2__add_lease_and_sync_publish.sql");
            assertThat(schema)
                    .doesNotContain("ddc_service_instance")
                    .doesNotContain("ddc_service_registry");
        }
    }

    private String resource(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }

    private boolean hasJpaEntityAnnotation(Class<?> type) {
        return java.util.Arrays.stream(type.getAnnotations())
                .anyMatch(annotation -> "jakarta.persistence.Entity".equals(annotation.annotationType().getName()));
    }
}
