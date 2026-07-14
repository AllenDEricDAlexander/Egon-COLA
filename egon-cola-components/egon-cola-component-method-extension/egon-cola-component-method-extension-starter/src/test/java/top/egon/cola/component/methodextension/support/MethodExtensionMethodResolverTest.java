package top.egon.cola.component.methodextension.support;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.methodextension.annotation.MethodExtension;
import top.egon.cola.component.methodextension.context.MethodExtensionContext;
import top.egon.cola.component.methodextension.exception.MethodExtensionConfigurationException;
import top.egon.cola.component.methodextension.handler.MethodExtensionDecision;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandler;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MethodExtensionMethodResolverTest {

    private final MethodExtensionMethodResolver resolver = new MethodExtensionMethodResolver();

    @Test
    void shouldPreferImplementationAnnotation() throws NoSuchMethodException {
        Method invoked = ServiceContract.class.getMethod("call", String.class);

        MethodExtensionMethodResolver.ResolvedMethodExtension resolved = resolver.resolve(invoked, new ServiceImpl());

        assertThat(resolved.method().getDeclaringClass()).isEqualTo(ServiceImpl.class);
        assertThat(resolved.annotation().handler()).isEqualTo(ImplementationHandler.class);
    }

    @Test
    void shouldFallBackToInterfaceAnnotation() throws NoSuchMethodException {
        Method invoked = InterfaceOnlyContract.class.getMethod("call", String.class);

        MethodExtensionMethodResolver.ResolvedMethodExtension resolved = resolver.resolve(
                invoked,
                new InterfaceOnlyImpl()
        );

        assertThat(resolved.method().getDeclaringClass()).isEqualTo(InterfaceOnlyImpl.class);
        assertThat(resolved.annotation().handler()).isEqualTo(InterfaceHandler.class);
    }

    @Test
    void shouldResolveGenericBridgeMethod() throws NoSuchMethodException {
        Method invoked = GenericContract.class.getMethod("convert", Object.class);

        MethodExtensionMethodResolver.ResolvedMethodExtension resolved = resolver.resolve(invoked, new StringService());

        assertThat(resolved.method().getParameterTypes()).containsExactly(String.class);
        assertThat(resolved.annotation().handler()).isEqualTo(ImplementationHandler.class);
    }

    @Test
    void shouldResolveInheritedAnnotation() throws NoSuchMethodException {
        Method invoked = BaseService.class.getMethod("inherited", String.class);

        MethodExtensionMethodResolver.ResolvedMethodExtension resolved = resolver.resolve(invoked, new ChildService());

        assertThat(resolved.method()).isEqualTo(invoked);
        assertThat(resolved.annotation().handler()).isEqualTo(InterfaceHandler.class);
    }

    @Test
    void shouldRejectMethodWithoutAnnotation() throws NoSuchMethodException {
        Method invoked = PlainService.class.getMethod("call", String.class);

        assertThatThrownBy(() -> resolver.resolve(invoked, new PlainService()))
                .isInstanceOf(MethodExtensionConfigurationException.class)
                .hasMessageContaining("No @MethodExtension found");
    }

    interface ServiceContract {

        @MethodExtension(handler = InterfaceHandler.class)
        String call(String value);
    }

    static class ServiceImpl implements ServiceContract {

        @Override
        @MethodExtension(handler = ImplementationHandler.class)
        public String call(String value) {
            return value;
        }
    }

    interface InterfaceOnlyContract {

        @MethodExtension(handler = InterfaceHandler.class)
        String call(String value);
    }

    static class InterfaceOnlyImpl implements InterfaceOnlyContract {

        @Override
        public String call(String value) {
            return value;
        }
    }

    interface GenericContract<T> {

        T convert(T value);
    }

    static class StringService implements GenericContract<String> {

        @Override
        @MethodExtension(handler = ImplementationHandler.class)
        public String convert(String value) {
            return value;
        }
    }

    static class BaseService {

        @MethodExtension(handler = InterfaceHandler.class)
        public String inherited(String value) {
            return value;
        }
    }

    static class ChildService extends BaseService {
    }

    static class PlainService {

        public String call(String value) {
            return value;
        }
    }

    static class InterfaceHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            return MethodExtensionDecision.allow();
        }
    }

    static class ImplementationHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            return MethodExtensionDecision.allow();
        }
    }
}
