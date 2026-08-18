package top.egon.cola.platform.rbac3.admin.iam.resource;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import top.egon.cola.platform.rbac3.admin.iam.resource.controller.ApplicationResourceController;

import static org.assertj.core.api.Assertions.assertThat;

class FieldCrudControllerTest {

    @Test
    void exposesFieldDefinitionReadCreateAndStatusOperations() {
        assertThat(java.util.Arrays.stream(ApplicationResourceController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("fields")
                        || method.getName().equals("resourceFields")))
                .allMatch(method -> method.isAnnotationPresent(GetMapping.class));
        assertThat(java.util.Arrays.stream(ApplicationResourceController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("createField")))
                .anyMatch(method -> method.isAnnotationPresent(PostMapping.class));
        assertThat(java.util.Arrays.stream(ApplicationResourceController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("changeFieldStatus")))
                .anyMatch(method -> method.isAnnotationPresent(PutMapping.class));
    }
}
