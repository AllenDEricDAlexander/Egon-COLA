package top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategyRegistry;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService;
import top.egon.cola.platform.tianquan.jianshen.starter.field.Rbac3FieldJacksonModule;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.aop.framework.ProxyFactory;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RequiresPermission;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.DefaultAuthorizationService;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Rbac3StarterAutoConfigurationTest {
    @Test
    void serviceTokenClientSelectsItsIdentityPropertiesWhenTianshuAlsoBindsTheSameType() {
        runner.withPropertyValues("egon.cola.platform.tianquan.jianshen.enabled=true",
                        "egon.cola.platform.tianquan.jianshen.authorization.service-token.enabled=true",
                        "egon.cola.platform.tianquan.jianshen.authorization.service-token.resource-uri=https://api.example/permission",
                        "egon.cola.platform.tianquan.jianshen.authorization.service-token.scopes=iam:authorization:read",
                        "egon.cola.platform.tianquan.jianshen.authorization.endpoint=https://api.example/authorization")
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(SensitiveStrategyRegistry.class, SensitiveStrategyRegistry::defaults)
                .withBean("idpServiceOAuth2Client", top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client.class,
                        () -> mock(top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client.class))
                .withBean("idpStarterProperties", top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.class,
                        top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties::new)
                .withBean("egon.cola.platform.tianquan.shoubing-top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties",
                        top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.class,
                        top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties::new)
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(
                        top.egon.cola.platform.tianquan.jianshen.starter.client.Rbac3AuthorizationClient.class));
    }

    @Test
    void allHostProfilesBindTheReviewedPermissionKeysWithoutLosingColons() throws Exception {
        var root = java.nio.file.Path.of("").toAbsolutePath();
        while (root != null && !java.nio.file.Files.isDirectory(root.resolve("docs/egon/plan"))) { root = root.getParent(); }
        assertThat(root).isNotNull();
        var profiles = Map.of(
                "egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/application.yml", 6,
                "egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/application-local.yml", 6,
                "egon-cola-yuheng/yuheng-admin/src/main/resources/application.yml", 17,
                "egon-cola-yuheng/yuheng-admin/src/main/resources/application-local.yml", 17,
                "egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml", 21,
                "egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml", 21,
                "egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/resources/application.yml", 52,
                "egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/resources/application-local.yml", 52,
                "egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/application-test.yml", 0,
                "egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/resources/application-local-it.yml", 0);
        String prefix = "egon.cola.platform.tianquan.jianshen.user-scope-policy";
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            for (var entry : profiles.entrySet()) {
                var values = new java.util.LinkedHashMap<String, Object>();
                var file = new org.springframework.core.io.FileSystemResource(root.resolve("egon-cola-xingyuan").resolve(entry.getKey()));
                for (var source : new org.springframework.boot.env.YamlPropertySourceLoader().load("hostScopePolicy", file)) {
                    ((org.springframework.core.env.MapPropertySource) source).getSource().forEach((key, value) -> {
                        if (key.startsWith(prefix + ".")) { values.put(key, value instanceof org.springframework.boot.origin.OriginTrackedValue tracked ? tracked.getValue() : value); }
                    });
                }
                var policy = new org.springframework.boot.context.properties.bind.Binder(new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(values))
                        .bind(prefix, org.springframework.boot.context.properties.bind.Bindable.of(Rbac3StarterProperties.UserScopePolicyProperties.class)).get();
                assertThat(validator.validate(policy)).as(entry.getKey()).isEmpty();
                assertThat(policy.isEnabled()).isEqualTo(entry.getValue() > 0);
                assertThat(policy.getPermissionScopes()).as(entry.getKey()).hasSize(entry.getValue());
                policy.getPermissionScopes().forEach((key, value) -> assertThat(value).containsExactly(key));
                if (entry.getValue() == 17 || entry.getValue() == 21 || entry.getValue() == 52) {
                    assertThat(policy.getPermissionScopes().keySet()).allSatisfy(key -> assertThat(key).contains(":"));
                }
            }
        }
    }

    @Test
    void configuredManagerAndAdvisorEnforceScopesThroughTheActualBean() {
        Instant now = Instant.parse("2026-10-12T00:00:00Z");
        var identity = new IdentityPrincipal("10001", "1001", "jti-1", Set.of("https://api.example/fixture"), now,
                now.plusSeconds(300), AuthenticationContext.password());
        var snapshot = new SystemAuthorizationSnapshot("1001", "10001", "20001", "fixture", 1, 2, List.of("50001"), null, null,
                Set.of("fixture:read"), Set.of(), Map.of(), Map.of(), "sha256:fixture", now, now.plusSeconds(300));
        var original = new DefaultAuthorizationService(() -> new AuthorizationService.RuntimeAuthorizationContext(identity, snapshot, false),
                request -> AuthorizationService.OperationSodResult.allowed(), request -> AuthorizationService.FenceResult.allowed(now), Clock.fixed(now, ZoneOffset.UTC));
        try {
            runner.withPropertyValues("egon.cola.platform.tianquan.jianshen.enabled=true",
                            "egon.cola.platform.tianquan.jianshen.user-scope-policy.enabled=true")
                    .withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource(
                            "exactScopeMap", Map.of("egon.cola.platform.tianquan.jianshen.user-scope-policy.permission-scopes[fixture:read][0]", "fixture:read"))))
                    .withBean("defaultValidator", jakarta.validation.Validator.class, LocalValidatorFactoryBean::new)
                    .withBean(ObjectMapper.class, ObjectMapper::new).withBean(SensitiveStrategyRegistry.class, SensitiveStrategyRegistry::defaults)
                    .withBean("consumerAuthorization", AuthorizationService.class, () -> original).run(context -> {
                        assertThat(context).hasNotFailed();
                        var advisor = context.getBean(AuthorizationManagerBeforeMethodInterceptor.class);
                        assertThat(advisor.getOrder()).isEqualTo(org.springframework.security.authorization.method.AuthorizationInterceptorsOrder.PRE_AUTHORIZE.getOrder() + 1);
                        assertThat(context.getBeanFactory().getBeanDefinition("rbac3MethodAuthorizationInterceptor").getRole())
                                .isEqualTo(org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE);
                        var target = new ScopeFixture(); var factory = new ProxyFactory(target); factory.addAdvisor(advisor);
                        var proxy = (ScopeFixture) factory.getProxy();
                        SecurityContextHolder.getContext().setAuthentication(new IdpAuthenticationToken(identity));
                        assertThatThrownBy(proxy::read).isInstanceOf(AccessDeniedException.class); assertThat(target.calls).isZero();
                        SecurityContextHolder.getContext().setAuthentication(new IdpAuthenticationToken(identity, List.of(new SimpleGrantedAuthority("SCOPE_fixture:read"))));
                        assertThat(proxy.read()).isEqualTo("original-result"); assertThat(target.calls).isOne();
                    });
        } finally { SecurityContextHolder.clearContext(); }
    }

    public static class ScopeFixture {
        private int calls;
        @RequiresPermission("fixture:read") public String read() { calls++; return "original-result"; }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(Rbac3StarterAutoConfiguration.class));

    @Test
    void staysDisabledByDefault() {
        runner.run(context -> assertThat(context).doesNotHaveBean(AuthorizationService.class));
    }

    @Test
    void isOptInAndBacksOffForConsumerAuthorizationService() {
        AuthorizationService consumer = mock(AuthorizationService.class);
        runner.withPropertyValues("egon.cola.platform.tianquan.jianshen.enabled=true")
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(SensitiveStrategyRegistry.class,
                        SensitiveStrategyRegistry::defaults)
                .withBean(AuthorizationService.class, () -> consumer)
                .run(context -> {
                    assertThat(context.getBean(AuthorizationService.class))
                            .isSameAs(consumer);
                    assertThat(context)
                            .hasSingleBean(AuthorizationManagerBeforeMethodInterceptor.class)
                            .hasSingleBean(Rbac3FieldJacksonModule.class)
                            .doesNotHaveBean("rbac3MethodAuthorizationAspect");
                });
    }
}
