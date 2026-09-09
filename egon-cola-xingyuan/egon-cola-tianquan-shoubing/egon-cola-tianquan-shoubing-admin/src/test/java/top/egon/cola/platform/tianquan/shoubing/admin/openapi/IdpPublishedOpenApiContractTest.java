package top.egon.cola.platform.tianquan.shoubing.admin.openapi;

import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.util.AntPathMatcher;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.config.GatewayOpenApiProperties;
import top.egon.cola.component.yuheng.openapi.customizer.EgonOperationCustomizer;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IdpPublishedOpenApiContractTest {

    private static final Map<String, String> DOMAIN_GROUPS = Map.of(
            "identity", "tianquan-shoubing-identity", "identity-audit", "tianquan-shoubing-audit",
            "identity-auth", "tianquan-shoubing-auth", "identity-profile", "tianquan-shoubing-profile",
            "oauth-protocol", "tianquan-shoubing-oauth");

    @Test
    void allPublishedHandlersCanBeCustomizedForTheDeclaredGroup() throws Exception {
        GatewayOpenApiProperties properties = new GatewayOpenApiProperties();
        properties.setPublishedGroups(List.copyOf(DOMAIN_GROUPS.values()));
        PropertySource<?> yaml = new YamlPropertySourceLoader().load(
                "tianquan-shoubing", new ClassPathResource("application.yml")).getFirst();
        assertThat(indexed(yaml, "egon.cola.component.yuheng.openapi.published-groups"))
                .containsExactlyInAnyOrderElementsOf(DOMAIN_GROUPS.values());
        OperationCustomizer customizer = new EgonOperationCustomizer(properties);
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        int operations = 0;
        for (var bean : scanner.findCandidateComponents("top.egon.cola.platform.tianquan.shoubing.admin")) {
            String name = bean.getBeanClassName();
            if (name.endsWith(".InternalRefreshTokenController")) {
                continue;
            }
            Class<?> controller = Class.forName(name);
            Object instance = mock(controller);
            EgonApiCatalog catalogAnnotation = controller.getAnnotation(EgonApiCatalog.class);
            String expectedGroup = DOMAIN_GROUPS.get(catalogAnnotation.entityDomainCode());
            assertThat(expectedGroup).as("catalog domain of %s", name).isNotNull();
            assertThat(catalogAnnotation.interfaceGroupCode())
                    .as("published Group of %s", name).isEqualTo(expectedGroup);
            for (var method : controller.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null) {
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
                assertThat(catalog.get("interfaceGroupCode")).isEqualTo(expectedGroup);
                RequestMapping parent = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
                for (String prefix : paths(parent)) {
                    for (String suffix : paths(mapping)) {
                        assertThat(matchingGroups(yaml, prefix + suffix))
                                .as("Springdoc Group for %s", prefix + suffix).containsExactly(expectedGroup);
                    }
                }
                operations++;
            }
        }
        assertThat(operations).isGreaterThan(20);
    }

    private List<String> paths(RequestMapping mapping) {
        return mapping == null || mapping.value().length == 0 ? List.of("") : List.of(mapping.value());
    }

    private List<String> indexed(PropertySource<?> yaml, String key) {
        List<String> values = new ArrayList<>();
        for (int index = 0; yaml.getProperty(key + "[" + index + "]") != null; index++) {
            values.add(String.valueOf(yaml.getProperty(key + "[" + index + "]")));
        }
        return values;
    }

    private List<String> matchingGroups(PropertySource<?> yaml, String path) {
        AntPathMatcher matcher = new AntPathMatcher();
        List<String> groups = new ArrayList<>();
        for (int index = 0; index < DOMAIN_GROUPS.size(); index++) {
            String prefix = "springdoc.group-configs[" + index + "]";
            boolean matches = indexed(yaml, prefix + ".paths-to-match").stream()
                    .anyMatch(pattern -> matcher.match(pattern, path));
            boolean excluded = indexed(yaml, prefix + ".paths-to-exclude").stream()
                    .anyMatch(pattern -> matcher.match(pattern, path));
            if (matches && !excluded) {
                groups.add(String.valueOf(yaml.getProperty(prefix + ".group")));
            }
        }
        return groups;
    }
}
