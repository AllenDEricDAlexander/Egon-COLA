package top.egon.cola.component.gateway.admin.application;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GatewayApplicationServiceTest {

    @Test
    void springSelectsTheProductionConstructor() {
        new ApplicationContextRunner()
                .withBean(
                        GatewayApplicationRepository.class,
                        () -> mock(GatewayApplicationRepository.class)
                )
                .withBean(
                        GatewayAuditLogRepository.class,
                        () -> mock(GatewayAuditLogRepository.class)
                )
                .withBean(GatewayApplicationService.class)
                .run(context -> assertThat(context)
                        .hasSingleBean(GatewayApplicationService.class));
    }
}
