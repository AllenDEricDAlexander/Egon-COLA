package ${package}.infrastructure.config;

import ${package}.domain.teaching.client.CourseCachePort;
import ${package}.domain.teaching.event.TeachingEventPublisher;
import ${package}.domain.teaching.gateway.TeachingQueryGateway;
import ${package}.domain.user.client.UserCachePort;
import ${package}.domain.user.event.UserEventPublisher;
import ${package}.domain.user.gateway.UserQueryGateway;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalAdapterConfigurationTest {

    @Test
    void local_configuration_keeps_non_persistence_ports_in_domain_contracts() {
        assertThat(CourseCachePort.class).isInterface();
        assertThat(TeachingEventPublisher.class).isInterface();
        assertThat(TeachingQueryGateway.class).isInterface();
        assertThat(UserCachePort.class).isInterface();
        assertThat(UserEventPublisher.class).isInterface();
        assertThat(UserQueryGateway.class).isInterface();
    }
}
