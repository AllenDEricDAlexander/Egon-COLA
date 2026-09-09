package top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.controller.ApplicationResourceController;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceCrudControllerTest {

    @Test
    void resourceCatalogUsesGlobalIamRouteWithoutTenantPath() {
        RequestMapping mapping = ApplicationResourceController.class
                .getAnnotation(RequestMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/api/tianquan-jianshen/v1/iam/resource-catalog");
        assertThat(ApplicationResourceController.class.getDeclaredMethods())
                .noneMatch(method -> java.util.Arrays.stream(method.getParameters())
                        .anyMatch(parameter -> parameter.getName().equals("tenantId")));
    }
}
