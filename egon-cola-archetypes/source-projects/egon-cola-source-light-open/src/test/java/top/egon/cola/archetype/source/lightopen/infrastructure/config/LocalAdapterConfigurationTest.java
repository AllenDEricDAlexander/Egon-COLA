package top.egon.cola.archetype.source.lightopen.infrastructure.config;

import top.egon.cola.archetype.source.lightopen.domain.teaching.client.CourseCachePort;
import top.egon.cola.archetype.source.lightopen.domain.teaching.event.TeachingEventPublisher;
import top.egon.cola.archetype.source.lightopen.domain.teaching.gateway.TeachingQueryGateway;
import top.egon.cola.archetype.source.lightopen.domain.user.client.UserCachePort;
import top.egon.cola.archetype.source.lightopen.domain.user.event.UserEventPublisher;
import top.egon.cola.archetype.source.lightopen.domain.user.gateway.UserQueryGateway;
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
