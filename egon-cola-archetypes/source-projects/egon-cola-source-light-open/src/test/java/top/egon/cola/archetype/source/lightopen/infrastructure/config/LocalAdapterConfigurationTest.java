package top.egon.cola.archetype.source.lightopen.infrastructure.config;

import top.egon.cola.archetype.source.lightopen.domain.teaching.service.CourseIdempotencyService;
import top.egon.cola.archetype.source.lightopen.domain.teaching.service.TeachingEventService;
import top.egon.cola.archetype.source.lightopen.domain.teaching.service.TeachingQueryService;
import top.egon.cola.archetype.source.lightopen.domain.user.service.UserEventService;
import top.egon.cola.archetype.source.lightopen.domain.user.service.UserIdempotencyService;
import top.egon.cola.archetype.source.lightopen.domain.user.service.UserQueryService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalAdapterConfigurationTest {

    @Test
    void local_configuration_keeps_non_persistence_ports_in_domain_contracts() {
        assertThat(CourseIdempotencyService.class).isInterface();
        assertThat(TeachingEventService.class).isInterface();
        assertThat(TeachingQueryService.class).isInterface();
        assertThat(UserIdempotencyService.class).isInterface();
        assertThat(UserEventService.class).isInterface();
        assertThat(UserQueryService.class).isInterface();
    }
}
