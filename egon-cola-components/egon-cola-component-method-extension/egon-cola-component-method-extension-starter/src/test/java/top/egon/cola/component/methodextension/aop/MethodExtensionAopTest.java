package top.egon.cola.component.methodextension.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import top.egon.cola.component.methodextension.annotation.MethodExtension;
import top.egon.cola.component.methodextension.autoconfigure.MethodExtensionProperties;
import top.egon.cola.component.methodextension.context.MethodExtensionContext;
import top.egon.cola.component.methodextension.exception.MethodExtensionConfigurationException;
import top.egon.cola.component.methodextension.handler.MethodExtensionDecision;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandler;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandlerResolver;
import top.egon.cola.component.methodextension.response.MethodExtensionResponseResolver;
import top.egon.cola.component.methodextension.support.MethodExtensionMethodResolver;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class MethodExtensionAopTest {

    private final HandlerFailure handlerFailure = new HandlerFailure("handler failed");

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    @BeforeEach
    void setUp() {
        beanFactory.registerSingleton("objectMapper", new ObjectMapper());
        beanFactory.registerSingleton("allowHandler", new AllowHandler());
        beanFactory.registerSingleton("directRejectHandler", new DirectRejectHandler());
        beanFactory.registerSingleton("jsonRejectHandler", new JsonRejectHandler());
        beanFactory.registerSingleton("nullDecisionHandler", new NullDecisionHandler());
        beanFactory.registerSingleton("throwingHandler", new ThrowingHandler(handlerFailure));
    }

    @Test
    void shouldRunHandlerThenInvokeBusinessExactlyOnceWhenAllowed() {
        SampleService target = new SampleService();
        SampleService proxy = proxy(target, -100);

        String result = proxy.allowed("u-001");

        assertThat(result).isEqualTo("allowed:u-001");
        assertThat(target.businessCalls).hasValue(1);
    }

    @Test
    void shouldSkipBusinessAndReturnDirectResponseWhenRejected() {
        SampleService target = new SampleService();
        SampleService proxy = proxy(target, -100);

        Payload result = proxy.direct();

        assertThat(result).isEqualTo(new Payload("direct"));
        assertThat(target.businessCalls).hasValue(0);
    }

    @Test
    void shouldSkipBusinessAndConvertReturnJsonWhenRejected() {
        SampleService target = new SampleService();
        SampleService proxy = proxy(target, -100);

        Payload result = proxy.json("u-001");

        assertThat(result).isEqualTo(new Payload("json"));
        assertThat(target.businessCalls).hasValue(0);
    }

    @Test
    void shouldLogRejectionMetadataWithoutArgumentsOrReturnJson(CapturedOutput output) {
        SampleService target = new SampleService();
        SampleService proxy = proxy(target, -100);

        proxy.json("sensitive-user");

        assertThat(output.getOut())
                .contains("Method extension rejected")
                .contains(JsonRejectHandler.class.getName())
                .doesNotContain("sensitive-user")
                .doesNotContain("{\"code\":\"json\"}");
    }

    @Test
    void shouldRejectNullDecisionWithoutCallingBusiness() {
        SampleService target = new SampleService();
        SampleService proxy = proxy(target, -100);

        assertThatThrownBy(proxy::nullDecision)
                .isInstanceOf(MethodExtensionConfigurationException.class)
                .hasMessageContaining("returned null");
        assertThat(target.businessCalls).hasValue(0);
    }

    @Test
    void shouldPropagateOriginalHandlerFailureWithoutCallingBusiness() {
        SampleService target = new SampleService();
        SampleService proxy = proxy(target, -100);

        assertThatThrownBy(proxy::failure).isSameAs(handlerFailure);
        assertThat(target.businessCalls).hasValue(0);
    }

    @Test
    void shouldExposeConfiguredAspectOrder() {
        MethodExtensionAop aop = aop(-77);

        assertThat(aop.getOrder()).isEqualTo(-77);
    }

    @Test
    void shouldOnlyMatchPublicMethods() throws NoSuchMethodException {
        MethodExtensionAop aop = aop(-77);

        assertThat(aop.matches(
                SampleService.class.getDeclaredMethod("protectedMethod"),
                SampleService.class
        )).isFalse();
    }

    private SampleService proxy(SampleService target, int order) {
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.addAdvisor(aop(order));
        return proxyFactory.getProxy();
    }

    private MethodExtensionAop aop(int order) {
        MethodExtensionProperties properties = new MethodExtensionProperties();
        properties.setOrder(order);
        return new MethodExtensionAop(
                properties,
                new MethodExtensionMethodResolver(),
                new MethodExtensionHandlerResolver(beanFactory),
                new MethodExtensionResponseResolver(beanFactory.getBeanProvider(ObjectMapper.class))
        );
    }

    public static class SampleService {

        private final AtomicInteger businessCalls = new AtomicInteger();

        @MethodExtension(handler = AllowHandler.class)
        public String allowed(String userId) {
            businessCalls.incrementAndGet();
            return "allowed:" + userId;
        }

        @MethodExtension(handler = DirectRejectHandler.class)
        public Payload direct() {
            businessCalls.incrementAndGet();
            return new Payload("business");
        }

        @MethodExtension(handler = JsonRejectHandler.class, returnJson = "{\"code\":\"json\"}")
        public Payload json(String userId) {
            businessCalls.incrementAndGet();
            return new Payload("business:" + userId);
        }

        @MethodExtension(handler = NullDecisionHandler.class)
        public String nullDecision() {
            businessCalls.incrementAndGet();
            return "business";
        }

        @MethodExtension(handler = ThrowingHandler.class)
        public String failure() {
            businessCalls.incrementAndGet();
            return "business";
        }

        @MethodExtension(handler = AllowHandler.class)
        protected String protectedMethod() {
            businessCalls.incrementAndGet();
            return "business";
        }
    }

    public static class AllowHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            return MethodExtensionDecision.allow();
        }
    }

    public static class DirectRejectHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            return MethodExtensionDecision.reject(new Payload("direct"), "direct rejection");
        }
    }

    public static class JsonRejectHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            return MethodExtensionDecision.rejectWithReason("json rejection");
        }
    }

    public static class NullDecisionHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            return null;
        }
    }

    public static class ThrowingHandler implements MethodExtensionHandler {

        private final HandlerFailure failure;

        ThrowingHandler(HandlerFailure failure) {
            this.failure = failure;
        }

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            throw failure;
        }
    }

    public record Payload(String code) {
    }

    static class HandlerFailure extends RuntimeException {

        HandlerFailure(String message) {
            super(message);
        }
    }
}
