package top.egon.cola.platform.rbac3.admin.iam.business.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import top.egon.cola.platform.rbac3.admin.iam.business.service.BusinessCatalogService;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessCatalogControllerTest {

    @Test
    void declaresTheDdcBackedCatalogRoutes() {
        RequestMapping mapping = BusinessCatalogController.class
                .getAnnotation(RequestMapping.class);
        assertThat(mapping.value()).containsExactly("/api/rbac3/v1/iam/catalog");
        assertThat(Arrays.stream(BusinessCatalogController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(GetMapping.class))
                .map(method -> method.getAnnotation(GetMapping.class).value()[0]))
                .containsExactlyInAnyOrder(
                        "/businesses", "/businesses/{ddcBusinessId}/applications");
    }

    @Test
    void canBeConstructedWithTheNarrowCatalogService() {
        assertThat(new BusinessCatalogController(
                (BusinessCatalogService) null)).isNotNull();
    }
}
