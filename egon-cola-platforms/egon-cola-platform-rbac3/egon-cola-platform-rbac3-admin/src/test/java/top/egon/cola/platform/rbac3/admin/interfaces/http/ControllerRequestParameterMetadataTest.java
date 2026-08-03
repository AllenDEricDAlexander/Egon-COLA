package top.egon.cola.platform.rbac3.admin.interfaces.http;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ControllerRequestParameterMetadataTest {

    @Test
    void preservesNamesForImplicitRequestParametersInPackagedClasses() {
        List<String> missingNames = List.of(
                        TenantUserDirectoryController.class,
                        RolePermissionController.class,
                        AuditSimulationController.class)
                .stream()
                .flatMap(type -> List.of(type.getDeclaredMethods()).stream())
                .flatMap(method -> List.of(method.getParameters()).stream()
                        .filter(ControllerRequestParameterMetadataTest::usesImplicitName)
                        .filter(parameter -> !parameter.isNamePresent())
                        .map(parameter -> signature(method, parameter)))
                .toList();

        assertThat(missingNames)
                .as("implicit @RequestParam names required by Spring MVC")
                .isEmpty();
    }

    private static boolean usesImplicitName(Parameter parameter) {
        RequestParam annotation = parameter.getAnnotation(RequestParam.class);
        return annotation != null && annotation.name().isBlank() && annotation.value().isBlank();
    }

    private static String signature(Method method, Parameter parameter) {
        return method.getDeclaringClass().getSimpleName() + '#'
                + method.getName() + ':' + parameter.getType().getSimpleName();
    }
}
