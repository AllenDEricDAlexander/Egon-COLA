package ${package}.infrastructure.config;

import ${package}.domain.teaching.service.CourseCacheService;
import ${package}.domain.teaching.service.CourseDomainService;
import ${package}.domain.teaching.service.SchoolClassDomainService;
import ${package}.domain.teaching.service.TeachingEventPublisher;
import ${package}.domain.teaching.service.TeachingQueryService;
import ${package}.domain.user.service.PermissionDomainService;
import ${package}.domain.user.service.RoleDomainService;
import ${package}.domain.user.service.UserCacheService;
import ${package}.domain.user.service.UserDomainService;
import ${package}.domain.user.service.UserEventPublisher;
import ${package}.domain.user.service.UserQueryService;
import ${package}.infrastructure.teaching.service.impl.CourseDomainServiceImpl;
import ${package}.infrastructure.teaching.service.impl.SchoolClassDomainServiceImpl;
import ${package}.infrastructure.user.service.impl.PermissionDomainServiceImpl;
import ${package}.infrastructure.user.service.impl.RoleDomainServiceImpl;
import ${package}.infrastructure.user.service.impl.UserDomainServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class LocalAdapterConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues(
                    "app.integrations.rabbitmq.enabled=false",
                    "app.integrations.redis.enabled=false",
                    "app.integrations.external-http.enabled=false")
            .withUserConfiguration(
                    TransactionCompletionExecutor.class,
                    LocalAdapterConfiguration.class,
                    UserDomainServiceImpl.class,
                    RoleDomainServiceImpl.class,
                    PermissionDomainServiceImpl.class,
                    SchoolClassDomainServiceImpl.class,
                    CourseDomainServiceImpl.class);

    @Test
    void assembles_one_local_implementation_for_every_domain_port() {
        contextRunner.run(context -> {
            assertSingleInfrastructureBean(context, UserDomainService.class);
            assertSingleInfrastructureBean(context, RoleDomainService.class);
            assertSingleInfrastructureBean(context, PermissionDomainService.class);
            assertSingleInfrastructureBean(context, SchoolClassDomainService.class);
            assertSingleInfrastructureBean(context, CourseDomainService.class);
            assertSingleInfrastructureBean(context, UserQueryService.class);
            assertSingleInfrastructureBean(context, UserCacheService.class);
            assertSingleInfrastructureBean(context, UserEventPublisher.class);
            assertSingleInfrastructureBean(context, TeachingQueryService.class);
            assertSingleInfrastructureBean(context, CourseCacheService.class);
            assertSingleInfrastructureBean(context, TeachingEventPublisher.class);
        });
    }

    private <T> void assertSingleInfrastructureBean(
            org.springframework.context.ApplicationContext context, Class<T> type) {
        String[] names = context.getBeanNamesForType(type);
        assertThat(names).hasSize(1);
        assertThat(context.getBean(names[0]).getClass().getPackageName())
                .startsWith("${package}.infrastructure");
    }
}
