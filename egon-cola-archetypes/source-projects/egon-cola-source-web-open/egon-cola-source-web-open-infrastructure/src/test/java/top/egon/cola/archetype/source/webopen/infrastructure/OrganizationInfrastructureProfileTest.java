package top.egon.cola.archetype.source.webopen.infrastructure;

import top.egon.cola.archetype.source.webopen.domain.client.CommandIdempotencyPort;
import top.egon.cola.archetype.source.webopen.domain.user.client.UserCachePort;
import top.egon.cola.archetype.source.webopen.infrastructure.cache.InMemoryCommandIdempotencyAdapter;
import top.egon.cola.archetype.source.webopen.infrastructure.user.cache.InMemoryUserCache;
import top.egon.cola.archetype.source.webopen.infrastructure.config.OrganizationIntegrationProperties;
import top.egon.cola.archetype.source.webopen.infrastructure.config.OrganizationLocalFallbackConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

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
                assertThat(context).hasSingleBean(InMemoryUserCache.class);
                assertThat(context).hasSingleBean(InMemoryCommandIdempotencyAdapter.class);
                assertThat(context).hasSingleBean(UserCachePort.class);
                assertThat(context).hasSingleBean(CommandIdempotencyPort.class);
            });
    }
}
