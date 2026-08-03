package top.egon.cola.component.gateway.admin.mcp.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class McpControlPlaneServiceConstructorTest {

    @Test
    void selectsTheProductionConstructorForSpringInjection() {
        var constructors = Arrays.stream(
                        McpControlPlaneService.class.getDeclaredConstructors()
                )
                .filter(constructor -> constructor.isAnnotationPresent(
                        Autowired.class
                ))
                .toList();

        assertThat(constructors).singleElement()
                .satisfies(constructor -> assertThat(
                        constructor.getParameterCount()
                ).isEqualTo(13));
    }
}
