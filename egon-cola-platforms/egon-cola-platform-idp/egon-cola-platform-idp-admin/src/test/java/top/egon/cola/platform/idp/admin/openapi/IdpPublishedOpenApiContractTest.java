package top.egon.cola.platform.idp.admin.openapi;

import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.gateway.openapi.config.GatewayOpenApiProperties;
import top.egon.cola.component.gateway.openapi.customizer.EgonOperationCustomizer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IdpPublishedOpenApiContractTest {

    @Test
    void allPublishedHandlersCanBeCustomizedForTheDeclaredGroup() throws Exception {
        GatewayOpenApiProperties properties = new GatewayOpenApiProperties();
        properties.setPublishedGroups(List.of("idp"));
        OperationCustomizer customizer = new EgonOperationCustomizer(properties);
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        int operations = 0;
        for (var bean : scanner.findCandidateComponents("top.egon.cola.platform.idp.admin")) {
            String name = bean.getBeanClassName();
            if (!(!name.endsWith(".InternalRefreshTokenController"))) {
                continue;
            }
            Class<?> controller = Class.forName(name);
            Object instance = mock(controller);
            assertThat(controller.getAnnotation(EgonApiCatalog.class).interfaceGroupCode())
                    .as("published Group of %s", name).isEqualTo("idp");
            for (var method : controller.getDeclaredMethods()) {
                if (AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class) == null) {
                    continue;
                }
                Operation annotation = AnnotatedElementUtils.findMergedAnnotation(method, Operation.class);
                assertThat(annotation).as("OpenAPI operation for %s", method).isNotNull();
                if (annotation.hidden()) {
                    continue;
                }
                var operation = new io.swagger.v3.oas.models.Operation().operationId(annotation.operationId());
                customizer.customize(operation, new HandlerMethod(instance, method));
                assertThat(operation.getExtensions()).containsKey("x-egon");
                var extension = (java.util.Map<?, ?>) operation.getExtensions().get("x-egon");
                var catalog = (java.util.Map<?, ?>) extension.get("catalog");
                assertThat(catalog.get("interfaceGroupCode")).isEqualTo("idp");
                operations++;
            }
        }
        assertThat(operations).isGreaterThan(20);
    }
}
