package top.egon.cola.component.gateway.test.live;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import top.egon.cola.component.gateway.test.process.GatewayTestInfrastructure;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayInfrastructureLiveIT {

    @Test
    @EnabledIfSystemProperty(named = "gateway.live.test", matches = "true")
    void provisionsPhysicallySeparatedStateAndEventInfrastructure() {
        try (GatewayLiveEnvironment environment =
                     new GatewayLiveEnvironment("infrastructure")) {
            environment.startInfrastructure();
            GatewayTestInfrastructure infrastructure =
                    environment.infrastructure();

            assertThat(infrastructure.ddcRedisHost()
                    + ":"
                    + infrastructure.ddcRedisPort())
                    .isNotEqualTo(infrastructure.rateLimitRedisHost()
                            + ":"
                            + infrastructure.rateLimitRedisPort());
            assertThat(infrastructure.jdbcUrl("postgres"))
                    .startsWith("jdbc:postgresql://");
            assertThat(infrastructure.kafkaBootstrapServers())
                    .isNotBlank();
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(
                    "cannot create isolated live environment",
                    failure
            );
        }
    }
}
