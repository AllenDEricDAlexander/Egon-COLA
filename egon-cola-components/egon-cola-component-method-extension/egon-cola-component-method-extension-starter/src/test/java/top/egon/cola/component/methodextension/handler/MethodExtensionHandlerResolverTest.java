package top.egon.cola.component.methodextension.handler;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import top.egon.cola.component.methodextension.context.MethodExtensionContext;
import top.egon.cola.component.methodextension.exception.MethodExtensionConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MethodExtensionHandlerResolverTest {

    @Test
    void shouldResolveUniqueHandlerBean() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        AllowHandler handler = new AllowHandler();
        beanFactory.registerSingleton("allowHandler", handler);

        MethodExtensionHandler result = new MethodExtensionHandlerResolver(beanFactory)
                .resolve(AllowHandler.class);

        assertThat(result).isSameAs(handler);
    }

    @Test
    void shouldResolveHandlerByItsTargetClassWhenBeanUsesJdkProxy() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        ProxyFactory proxyFactory = new ProxyFactory(new AllowHandler());
        proxyFactory.setProxyTargetClass(false);
        MethodExtensionHandler proxy = (MethodExtensionHandler) proxyFactory.getProxy();
        beanFactory.registerSingleton("proxiedAllowHandler", proxy);

        MethodExtensionHandler result = new MethodExtensionHandlerResolver(beanFactory)
                .resolve(AllowHandler.class);

        assertThat(result).isSameAs(proxy);
    }

    @Test
    void shouldRejectMissingHandlerBean() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        assertThatThrownBy(() -> new MethodExtensionHandlerResolver(beanFactory).resolve(AllowHandler.class))
                .isInstanceOf(MethodExtensionConfigurationException.class)
                .hasMessageContaining("No MethodExtensionHandler bean found")
                .hasMessageContaining(AllowHandler.class.getName());
    }

    @Test
    void shouldRejectAmbiguousHandlerBeans() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("allowHandlerOne", new AllowHandler());
        beanFactory.registerSingleton("allowHandlerTwo", new AllowHandler());

        assertThatThrownBy(() -> new MethodExtensionHandlerResolver(beanFactory).resolve(AllowHandler.class))
                .isInstanceOf(MethodExtensionConfigurationException.class)
                .hasMessageContaining("Multiple MethodExtensionHandler beans found")
                .hasMessageContaining("allowHandlerOne")
                .hasMessageContaining("allowHandlerTwo");
    }

    static class AllowHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            return MethodExtensionDecision.allow();
        }
    }
}
