package top.egon.cola.component.gateway.engine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.Clock;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayEngineClockQualificationTest {

    @Test
    void everyGatewayClockDependencyIsQualified() {
        Arrays.stream(GatewayEngineConfiguration.class.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getParameters()))
                .filter(parameter -> parameter.getType() == Clock.class)
                .forEach(parameter -> {
                    Qualifier qualifier = parameter.getAnnotation(
                            Qualifier.class
                    );
                    assertEquals(
                            "gatewayClock",
                            qualifier == null ? null : qualifier.value(),
                            "Clock dependency must select the Gateway clock"
                    );
                });
    }
}
