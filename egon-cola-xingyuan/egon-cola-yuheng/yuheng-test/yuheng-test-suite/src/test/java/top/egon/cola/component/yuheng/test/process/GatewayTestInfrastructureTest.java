package top.egon.cola.component.yuheng.test.process;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayTestInfrastructureTest {

    private final String previousInfrastructure = System.getProperty(
            "yuheng.live.infrastructure"
    );

    @AfterEach
    void restoreInfrastructureSelection() {
        if (previousInfrastructure == null) {
            System.clearProperty("yuheng.live.infrastructure");
            return;
        }
        System.setProperty(
                "yuheng.live.infrastructure",
                previousInfrastructure
        );
    }

    @Test
    void selectsLocalInfrastructureFromSystemProperty() {
        System.setProperty("yuheng.live.infrastructure", "local");

        assertThat(new GatewayTestInfrastructure().type())
                .isEqualTo("local");
    }
}
