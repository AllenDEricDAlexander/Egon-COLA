package ${package}.infrastructure;

import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.domain.user.client.UserCachePort;
import ${package}.infrastructure.cache.InMemoryCommandIdempotencyAdapter;
import ${package}.infrastructure.user.cache.InMemoryUserCache;
import ${package}.infrastructure.config.OrganizationIntegrationProperties;
import ${package}.infrastructure.config.OrganizationLocalFallbackConfig;
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
