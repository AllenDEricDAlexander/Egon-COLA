package top.egon.cola.component.yuheng.admin.mcp.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Arrays;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

import static org.assertj.core.api.Assertions.assertThat;

class McpControlPlaneServiceConstructorTest {

    @BeforeAll
    static void bindTheProcessWideEngine() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
    }

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
