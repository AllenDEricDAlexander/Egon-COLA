package top.egon.cola.component.accessguard.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.accessguard.api.AccessGuard;
import top.egon.cola.component.accessguard.api.AccessGuardClient;
import top.egon.cola.component.accessguard.api.AccessGuardRejectedException;
import top.egon.cola.component.accessguard.api.GuardKey;
import top.egon.cola.component.accessguard.api.GuardRequest;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardAopAutoConfiguration;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardCoreAutoConfiguration;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardLocalStoreAutoConfiguration;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardTimeLimitAutoConfiguration;
import top.egon.cola.component.accessguard.store.DenyListStore;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessGuardSampleTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    AccessGuardCoreAutoConfiguration.class,
                    AccessGuardLocalStoreAutoConfiguration.class,
                    AccessGuardTimeLimitAutoConfiguration.class,
                    AccessGuardAopAutoConfiguration.class))
            .withPropertyValues(
                    "egon.cola.component.access-guard.key.hmac-secret=sample-secret",
                    "egon.cola.component.access-guard.rules.draw.key.contributors[0]=GLOBAL");

    @Test
    void annotationEntryProtectsARealSpringBean() {
        contextRunner.withUserConfiguration(SampleConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    DrawService service = context.getBean(DrawService.class);

                    assertThat(service.draw("user-1"))
                            .isEqualTo(new DrawResult("draw:user-1"));
                    assertThat(service.calls()).isOne();
                });
    }

    @Test
    void rejectedAnnotationEntryNeverCallsTheBusinessMethod() {
        contextRunner.withUserConfiguration(DeniedSampleConfiguration.class)
                .withPropertyValues(
                        "egon.cola.component.access-guard.rules.draw.deny-list.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    DrawService service = context.getBean(DrawService.class);

                    assertThatThrownBy(() -> service.draw("user-1"))
                            .isInstanceOf(AccessGuardRejectedException.class);
                    assertThat(service.calls()).isZero();
                });
    }

    @Test
    void programmaticEntryUsesTheSameConfiguredRule() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            AccessGuardClient client = context.getBean(AccessGuardClient.class);
            GuardRequest request = new GuardRequest(
                    "draw",
                    new Object[]{"user-1"},
                    Map.of(),
                    DrawResult.class,
                    null);

            DrawResult result = client.execute(
                    request, () -> new DrawResult("draw:user-1"));

            assertThat(result).isEqualTo(new DrawResult("draw:user-1"));
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class SampleConfiguration {

        @Bean
        DrawService drawService() {
            return new DrawService();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class DeniedSampleConfiguration extends SampleConfiguration {

        @Bean
        DenyListStore denyListStore() {
            return (ruleId, dataVersion, keyHash) -> true;
        }
    }

    static class DrawService {

        private final AtomicInteger calls = new AtomicInteger();

        @AccessGuard("draw")
        public DrawResult draw(@GuardKey("user") String userId) {
            calls.incrementAndGet();
            return new DrawResult("draw:" + userId);
        }

        int calls() {
            return calls.get();
        }
    }

    record DrawResult(String value) {
    }
}
