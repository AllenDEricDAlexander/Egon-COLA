package top.egon.cola.component.methodextension.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import top.egon.cola.component.methodextension.annotation.MethodExtension;
import top.egon.cola.component.methodextension.context.MethodExtensionContext;
import top.egon.cola.component.methodextension.event.MethodExtensionEvent;
import top.egon.cola.component.methodextension.exception.MethodExtensionConfigurationException;
import top.egon.cola.component.methodextension.handler.MethodExtensionDecision;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandler;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandlerResolver;
import top.egon.cola.component.methodextension.response.MethodExtensionResponseResolver;
import top.egon.cola.component.methodextension.support.MethodExtensionMethodResolver;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MethodExtensionExecutionServiceTest {

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    private final List<MethodExtensionEvent> events = new ArrayList<>();
    private MethodExtensionExecutionService service;

    @BeforeEach
    void setUp() {
        beanFactory.registerSingleton("objectMapper", new ObjectMapper());
        beanFactory.registerSingleton("allow", new AllowHandler());
        beanFactory.registerSingleton("reject", new RejectHandler());
        beanFactory.registerSingleton("json", new JsonHandler());
        beanFactory.registerSingleton("nullDecision", new NullDecisionHandler());
        service = new MethodExtensionExecutionService(
                new MethodExtensionMethodResolver(),
                new MethodExtensionHandlerResolver(beanFactory),
                new MethodExtensionResponseResolver(
                        beanFactory.getBeanProvider(ObjectMapper.class)),
                events::add
        );
    }

    @Test
    void evaluatesAllowDirectRejectAndJsonRejectThroughOneSemanticSeam() throws Throwable {
        Target target = new Target();
        Object[] arguments = {"sensitive"};

        MethodExtensionExecutionResult allowed = evaluate(target, "allowed", arguments);
        MethodExtensionExecutionResult rejected = evaluate(target, "rejected", new Object[0]);
        MethodExtensionExecutionResult json = evaluate(target, "json", new Object[0]);

        assertThat(allowed.proceed()).isTrue();
        assertThat(rejected).isEqualTo(MethodExtensionExecutionResult.reject("blocked"));
        assertThat(json.rejectionValue()).isEqualTo(new Payload("json"));
        assertThat(arguments).containsExactly("sensitive");
        assertThat(events).extracting(MethodExtensionEvent::outcome)
                .containsExactly("ALLOW", "REJECT", "REJECT");
        assertThat(events.toString()).doesNotContain("sensitive").doesNotContain("{\"code\"");
    }

    @Test
    void rejectsNullDecisionAndPublishesBoundedError() throws Exception {
        Target target = new Target();
        Method method = Target.class.getDeclaredMethod("nullDecision");

        assertThatThrownBy(() -> service.evaluate(target, method, new Object[0]))
                .isInstanceOf(MethodExtensionConfigurationException.class)
                .hasMessageContaining("returned null");
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.outcome()).isEqualTo("ERROR");
            assertThat(event.reason()).doesNotContain("password");
        });
    }

    private MethodExtensionExecutionResult evaluate(
            Target target,
            String methodName,
            Object[] arguments
    ) throws Throwable {
        Method method = Target.class.getDeclaredMethod(
                methodName, methodName.equals("allowed") ? new Class<?>[]{String.class}
                        : new Class<?>[0]);
        return service.evaluate(target, method, arguments);
    }

    static class Target {

        @MethodExtension(handler = AllowHandler.class)
        public String allowed(String value) {
            return value;
        }

        @MethodExtension(handler = RejectHandler.class)
        public String rejected() {
            return "business";
        }

        @MethodExtension(handler = JsonHandler.class, returnJson = "{\"code\":\"json\"}")
        public Payload json() {
            return new Payload("business");
        }

        @MethodExtension(handler = NullDecisionHandler.class)
        public String nullDecision() {
            return "business";
        }
    }

    static class AllowHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            return MethodExtensionDecision.allow();
        }
    }

    static class RejectHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            return MethodExtensionDecision.reject("blocked", "policy");
        }
    }

    static class JsonHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            return MethodExtensionDecision.rejectWithReason("json");
        }
    }

    static class NullDecisionHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            return null;
        }
    }

    record Payload(String code) {
    }
}
