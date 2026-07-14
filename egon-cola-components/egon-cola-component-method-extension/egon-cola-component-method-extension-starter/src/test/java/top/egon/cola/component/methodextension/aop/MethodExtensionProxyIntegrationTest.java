package top.egon.cola.component.methodextension.aop;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
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

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class MethodExtensionProxyIntegrationTest {

    private final ApplicationContextRunner jdkRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class, MethodExtensionAutoConfiguration.class))
            .withPropertyValues("spring.aop.proxy-target-class=false")
            .withUserConfiguration(JdkProxyConfiguration.class);

    private final ApplicationContextRunner cglibRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class, MethodExtensionAutoConfiguration.class))
            .withPropertyValues("spring.aop.proxy-target-class=true")
            .withUserConfiguration(CglibProxyConfiguration.class);

    @Test
    void shouldInterceptAnnotationDeclaredOnJdkProxyInterface() {
        jdkRunner.run(context -> {
            InterfaceAnnotatedService service = context.getBean(InterfaceAnnotatedService.class);
            CountingHandler handler = context.getBean(CountingHandler.class);

            assertThat(AopUtils.isJdkDynamicProxy(service)).isTrue();
            assertThat(service.call("value")).isEqualTo("interface:value");
            assertThat(handler.calls).hasValue(1);
        });
    }

    @Test
    void shouldInterceptImplementationAnnotationBehindJdkProxy() {
        jdkRunner.run(context -> {
            ImplementationAnnotatedService service = context.getBean(ImplementationAnnotatedService.class);
            CountingHandler handler = context.getBean(CountingHandler.class);

            assertThat(AopUtils.isJdkDynamicProxy(service)).isTrue();
            assertThat(service.call("value")).isEqualTo("implementation:value");
            assertThat(handler.calls).hasValue(1);
        });
    }

    @Test
    void shouldLeaveUnannotatedMethodUnaffected() {
        jdkRunner.run(context -> {
            InterfaceAnnotatedService service = context.getBean(InterfaceAnnotatedService.class);
            CountingHandler handler = context.getBean(CountingHandler.class);

            assertThat(service.plain("value")).isEqualTo("plain:value");
            assertThat(handler.calls).hasValue(0);
        });
    }

    @Test
    void shouldBypassHandlerWhenComponentIsDisabled() {
        jdkRunner.withPropertyValues("egon.cola.component.method-extension.enabled=false")
                .run(context -> {
                    InterfaceAnnotatedService service = context.getBean(InterfaceAnnotatedService.class);
                    CountingHandler handler = context.getBean(CountingHandler.class);

                    assertThat(AopUtils.isAopProxy(service)).isFalse();
                    assertThat(service.call("value")).isEqualTo("interface:value");
                    assertThat(handler.calls).hasValue(0);
                });
    }

    @Test
    void shouldInterceptInheritedMethodOnCglibProxy() {
        cglibRunner.run(context -> {
            InheritedService service = context.getBean(InheritedService.class);
            CountingHandler handler = context.getBean(CountingHandler.class);

            assertThat(AopUtils.isCglibProxy(service)).isTrue();
            assertThat(service.inherited("value")).isEqualTo("inherited:value");
            assertThat(handler.calls).hasValue(1);
        });
    }

    @Test
    void shouldLeaveSelfInvocationUnintercepted() {
        cglibRunner.run(context -> {
            SelfInvokingService service = context.getBean(SelfInvokingService.class);
            CountingHandler handler = context.getBean(CountingHandler.class);

            assertThat(AopUtils.isCglibProxy(service)).isTrue();
            assertThat(service.outer("value")).isEqualTo("inner:value");
            assertThat(handler.calls).hasValue(0);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class JdkProxyConfiguration {

        @Bean
        CountingHandler countingHandler() {
            return new CountingHandler();
        }

        @Bean
        InterfaceAnnotatedService interfaceAnnotatedService() {
            return new InterfaceAnnotatedServiceImpl();
        }

        @Bean
        ImplementationAnnotatedService implementationAnnotatedService() {
            return new ImplementationAnnotatedServiceImpl();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CglibProxyConfiguration {

        @Bean
        CountingHandler countingHandler() {
            return new CountingHandler();
        }

        @Bean
        InheritedService inheritedService() {
            return new InheritedService();
        }

        @Bean
        SelfInvokingService selfInvokingService() {
            return new SelfInvokingService();
        }
    }

    interface InterfaceAnnotatedService {

        @MethodExtension(handler = CountingHandler.class)
        String call(String value);

        String plain(String value);
    }

    static class InterfaceAnnotatedServiceImpl implements InterfaceAnnotatedService {

        @Override
        public String call(String value) {
            return "interface:" + value;
        }

        @Override
        public String plain(String value) {
            return "plain:" + value;
        }
    }

    interface ImplementationAnnotatedService {

        String call(String value);
    }

    static class ImplementationAnnotatedServiceImpl implements ImplementationAnnotatedService {

        @Override
        @MethodExtension(handler = CountingHandler.class)
        public String call(String value) {
            return "implementation:" + value;
        }
    }

    static class BaseService {

        @MethodExtension(handler = CountingHandler.class)
        public String inherited(String value) {
            return "inherited:" + value;
        }
    }

    static class InheritedService extends BaseService {
    }

    static class SelfInvokingService {

        public String outer(String value) {
            return inner(value);
        }

        @MethodExtension(handler = CountingHandler.class)
        public String inner(String value) {
            return "inner:" + value;
        }
    }

    static class CountingHandler implements MethodExtensionHandler {

        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            calls.incrementAndGet();
            return MethodExtensionDecision.allow();
        }
    }
}
