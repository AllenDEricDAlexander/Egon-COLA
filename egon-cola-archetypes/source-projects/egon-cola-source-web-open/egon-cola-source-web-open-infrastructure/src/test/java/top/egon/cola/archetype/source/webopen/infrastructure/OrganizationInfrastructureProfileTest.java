package top.egon.cola.archetype.source.webopen.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.archetype.source.webopen.domain.service.CommandIdempotencyService;
import top.egon.cola.archetype.source.webopen.infrastructure.config.OrganizationIntegrationProperties;
import top.egon.cola.archetype.source.webopen.infrastructure.config.OrganizationLocalFallbackConfig;
import top.egon.cola.archetype.source.webopen.infrastructure.mq.MqMessageService;
import top.egon.cola.archetype.source.webopen.infrastructure.service.impl.InMemoryCommandIdempotencyServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A profile without the external system still boots on the same Domain Service and MQ boundaries;
 * only the implementation behind those two ports is swapped.
 */
class OrganizationInfrastructureProfileTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(OrganizationLocalFallbackConfig.class)
        .withBean(OrganizationIntegrationProperties.class);

    @Test
    void localProfileCreatesOnlyInMemoryPorts() {
        contextRunner.withPropertyValues(
                "organization.integrations.redis.enabled=false",
                "organization.integrations.rabbit.enabled=false")
            .run(context -> {
                assertThat(context).hasSingleBean(CommandIdempotencyService.class);
                assertThat(context.getBean(CommandIdempotencyService.class))
                        .isInstanceOf(InMemoryCommandIdempotencyServiceImpl.class);
                assertThat(context).hasSingleBean(MqMessageService.class);
                assertThat(context.getBean(MqMessageService.class))
                        .isInstanceOf(OrganizationLocalFallbackConfig.LocalMqMessageService.class);
            });
    }

    @Test
    void enabledProfileLeavesThePortsToTheirIntegrationOwners() {
        contextRunner.withPropertyValues(
                "organization.integrations.redis.enabled=true",
                "organization.integrations.rabbit.enabled=true")
            .run(context -> assertThat(context).doesNotHaveBean(CommandIdempotencyService.class));
    }
}
