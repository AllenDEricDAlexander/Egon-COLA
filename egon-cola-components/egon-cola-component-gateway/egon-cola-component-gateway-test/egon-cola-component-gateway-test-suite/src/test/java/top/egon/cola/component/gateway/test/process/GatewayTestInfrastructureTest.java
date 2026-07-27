package top.egon.cola.component.gateway.test.process;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayTestInfrastructureTest {

    private final String previousInfrastructure = System.getProperty(
            "gateway.live.infrastructure"
    );

    @AfterEach
    void restoreInfrastructureSelection() {
        if (previousInfrastructure == null) {
            System.clearProperty("gateway.live.infrastructure");
            return;
        }
        System.setProperty(
                "gateway.live.infrastructure",
                previousInfrastructure
        );
    }

    @Test
    void selectsLocalInfrastructureFromSystemProperty() {
        System.setProperty("gateway.live.infrastructure", "local");

        assertThat(new GatewayTestInfrastructure().type())
                .isEqualTo("local");
    }
}
