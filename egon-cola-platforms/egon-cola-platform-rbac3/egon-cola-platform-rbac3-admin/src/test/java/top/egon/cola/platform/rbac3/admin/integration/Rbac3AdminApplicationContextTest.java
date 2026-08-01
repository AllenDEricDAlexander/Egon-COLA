package top.egon.cola.platform.rbac3.admin.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import top.egon.cola.platform.rbac3.admin.integration.flyway.Rbac3FlywayConfiguration;
import top.egon.cola.platform.rbac3.admin.integration.runtime.Rbac3ReadinessIndicator;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class Rbac3AdminApplicationContextTest {

    @Test
    void usesIndependentFlywayHistoriesOnTheSameDataSource() {
        DataSource dataSource = mock(DataSource.class);
        Flyway rbac3 = Rbac3FlywayConfiguration.buildRbac3Flyway(dataSource);
        Flyway outbox = Rbac3FlywayConfiguration.buildOutboxFlyway(dataSource);

        assertThat(rbac3.getConfiguration().getDataSource()).isSameAs(dataSource);
        assertThat(outbox.getConfiguration().getDataSource()).isSameAs(dataSource);
        assertThat(rbac3.getConfiguration().getTable())
                .isEqualTo("flyway_schema_history_rbac3");
        assertThat(outbox.getConfiguration().getTable())
                .isEqualTo("flyway_schema_history_outbox");
        assertThat(rbac3.getConfiguration().getLocations())
                .extracting(Object::toString)
                .containsExactly("classpath:db/migration");
        assertThat(outbox.getConfiguration().getLocations())
                .extracting(Object::toString)
                .containsExactly("classpath:db/transactional-outbox/postgresql");
    }

    @Test
    void readinessFailsClosedWhenAnyApplicationPrerequisiteFails() {
        var indicator = new Rbac3ReadinessIndicator(
                List.of(
                        new Rbac3ReadinessIndicator.ReadinessCheck(
                                "rbac3Flyway", () -> true),
                        new Rbac3ReadinessIndicator.ReadinessCheck(
                                "outboxFlyway", () -> false)),
                () -> "UNKNOWN");

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(indicator.health().getDetails())
                .containsEntry("failedCheck", "outboxFlyway")
                .containsEntry("gatewayRouteability", "UNKNOWN");
    }

    @Test
    void routeabilityIsReportedButDoesNotCreateAStartupDeadlock() {
        var indicator = new Rbac3ReadinessIndicator(
                List.of(new Rbac3ReadinessIndicator.ReadinessCheck(
                        "application", () -> true)),
                () -> "NOT_ROUTABLE");

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
        assertThat(indicator.health().getDetails())
                .containsEntry("gatewayRouteability", "NOT_ROUTABLE");

        indicator.stopAcceptingTraffic();
        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(indicator.health().getDetails())
                .containsEntry("failedCheck", "trafficAcceptance");
    }
}
