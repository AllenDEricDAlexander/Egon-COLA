package top.egon.cola.component.methodextension.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.methodextension.annotation.MethodExtension;
import top.egon.cola.component.methodextension.autoconfigure.MethodExtensionAutoConfiguration;
import top.egon.cola.component.methodextension.context.MethodExtensionContext;
import top.egon.cola.component.methodextension.handler.MethodExtensionDecision;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandler;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MethodExtensionSampleTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class, MethodExtensionAutoConfiguration.class))
            .withPropertyValues("spring.aop.proxy-target-class=true")
            .withUserConfiguration(SampleConfiguration.class);

    @Test
    void shouldUseReturnJsonForBlacklistedUserAndAllowOtherUsers() {
        contextRunner.run(context -> {
            UserQueryService service = context.getBean(UserQueryService.class);

            assertThat(service.query("bbb"))
                    .isEqualTo(new UserResponse(1111, "access rejected", null));
            assertThat(service.query("aaa"))
                    .isEqualTo(new UserResponse(0, "success", "user:aaa"));
            assertThat(service.calls()).isEqualTo(1);
        });
    }

    @Test
    void shouldUseDirectResponseOutsideGrayCohort() {
        contextRunner.run(context -> {
            FeatureService service = context.getBean(FeatureService.class);

            assertThat(service.feature("legacy-user"))
                    .isEqualTo(new FeatureResponse(false, "legacy path"));
            assertThat(service.feature("gray-001"))
                    .isEqualTo(new FeatureResponse(true, "new path"));
            assertThat(service.calls()).isEqualTo(1);
        });
    }

    @Test
    void shouldPropagateTemporaryValidationFailure() {
        contextRunner.run(context -> {
            TemporaryService service = context.getBean(TemporaryService.class);

            assertThatThrownBy(() -> service.submit(" "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("request must not be blank");
            assertThat(service.calls()).isZero();
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class SampleConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        BlacklistHandler blacklistHandler() {
            return new BlacklistHandler();
        }

        @Bean
        GrayReleaseHandler grayReleaseHandler() {
            return new GrayReleaseHandler();
        }

        @Bean
        TemporaryValidationHandler temporaryValidationHandler() {
            return new TemporaryValidationHandler();
        }

        @Bean
        UserQueryService userQueryService() {
            return new UserQueryService();
        }

        @Bean
        FeatureService featureService() {
            return new FeatureService();
        }

        @Bean
        TemporaryService temporaryService() {
            return new TemporaryService();
        }
    }

    static class BlacklistHandler implements MethodExtensionHandler {

        private static final Set<String> BLOCKED_USERS = Set.of("bbb", "222");

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            String userId = (String) context.arguments()[0];
            return BLOCKED_USERS.contains(userId)
                    ? MethodExtensionDecision.rejectWithReason("blacklist hit")
                    : MethodExtensionDecision.allow();
        }
    }

    static class GrayReleaseHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            String userId = (String) context.arguments()[0];
            return userId.startsWith("gray-")
                    ? MethodExtensionDecision.allow()
                    : MethodExtensionDecision.reject(new FeatureResponse(false, "legacy path"), "outside gray cohort");
        }
    }

    static class TemporaryValidationHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            String request = (String) context.arguments()[0];
            if (request.isBlank()) {
                throw new IllegalArgumentException("request must not be blank");
            }
            return MethodExtensionDecision.allow();
        }
    }

    static class UserQueryService {

        private final AtomicInteger calls = new AtomicInteger();

        @MethodExtension(
                handler = BlacklistHandler.class,
                returnJson = "{\"code\":1111,\"message\":\"access rejected\",\"name\":null}"
        )
        public UserResponse query(String userId) {
            calls.incrementAndGet();
            return new UserResponse(0, "success", "user:" + userId);
        }

        int calls() {
            return calls.get();
        }
    }

    static class FeatureService {

        private final AtomicInteger calls = new AtomicInteger();

        @MethodExtension(handler = GrayReleaseHandler.class)
        public FeatureResponse feature(String userId) {
            calls.incrementAndGet();
            return new FeatureResponse(true, "new path");
        }

        int calls() {
            return calls.get();
        }
    }

    static class TemporaryService {

        private final AtomicInteger calls = new AtomicInteger();

        @MethodExtension(handler = TemporaryValidationHandler.class)
        public String submit(String request) {
            calls.incrementAndGet();
            return request;
        }

        int calls() {
            return calls.get();
        }
    }

    record UserResponse(int code, String message, String name) {
    }

    record FeatureResponse(boolean enabled, String path) {
    }
}
