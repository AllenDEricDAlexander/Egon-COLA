package top.egon.cola.component.methodextension.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import top.egon.cola.component.methodextension.exception.MethodExtensionConfigurationException;
import top.egon.cola.component.methodextension.exception.MethodExtensionResponseException;
import top.egon.cola.component.methodextension.handler.MethodExtensionDecision;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MethodExtensionResponseResolverTest {

    @Test
    void shouldPreferCompatibleDirectResponseWithoutObjectMapper() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("payload");
        Payload response = new Payload("direct");

        Object result = resolver().resolve(method, MethodExtensionDecision.reject(response), "{\"code\":\"json\"}");

        assertThat(result).isSameAs(response);
    }

    @Test
    void shouldAcceptBoxedValueForPrimitiveReturnType() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("count");

        Object result = resolver().resolve(method, MethodExtensionDecision.reject(7), "");

        assertThat(result).isEqualTo(7);
    }

    @Test
    void shouldReturnCompatibleAsyncWrapperProvidedByHandler() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("asyncPayload");
        CompletableFuture<Payload> response = CompletableFuture.completedFuture(new Payload("direct"));

        Object result = resolver().resolve(method, MethodExtensionDecision.reject(response), "");

        assertThat(result).isSameAs(response);
    }

    @Test
    void shouldConvertJsonWithGenericReturnType() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("payloads");

        Object result = resolver(new ObjectMapper()).resolve(
                method,
                MethodExtensionDecision.reject(),
                "[{\"code\":\"limited\"}]"
        );

        assertThat(result).isEqualTo(List.of(new Payload("limited")));
    }

    @Test
    void shouldReturnRawJsonForStringWithoutObjectMapper() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("text");

        Object result = resolver().resolve(method, MethodExtensionDecision.reject(), "{\"code\":1111}");

        assertThat(result).isEqualTo("{\"code\":1111}");
    }

    @Test
    void shouldReturnNullForVoidRejectionWithoutJson() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("nothing");

        Object result = resolver().resolve(method, MethodExtensionDecision.reject(), "");

        assertThat(result).isNull();
    }

    @Test
    void shouldRejectJsonForVoidReturnType() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("nothing");

        assertThatThrownBy(() -> resolver().resolve(method, MethodExtensionDecision.reject(), "{}"))
                .isInstanceOf(MethodExtensionConfigurationException.class)
                .hasMessageContaining("void method");
    }

    @Test
    void shouldRequireObjectMapperOnlyForObjectJsonConversion() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("payload");

        assertThatThrownBy(() -> resolver().resolve(method, MethodExtensionDecision.reject(), "{\"code\":\"x\"}"))
                .isInstanceOf(MethodExtensionConfigurationException.class)
                .hasMessageContaining("No ObjectMapper bean found");
    }

    @Test
    void shouldRejectMissingFallbackForNonVoidMethod() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("payload");

        assertThatThrownBy(() -> resolver().resolve(method, MethodExtensionDecision.reject(), " "))
                .isInstanceOf(MethodExtensionConfigurationException.class)
                .hasMessageContaining("rejected without a response or returnJson");
    }

    @Test
    void shouldRejectAmbiguousObjectMappers() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("payload");

        assertThatThrownBy(() -> resolver(new ObjectMapper(), new ObjectMapper())
                .resolve(method, MethodExtensionDecision.reject(), "{\"code\":\"x\"}"))
                .isInstanceOf(MethodExtensionConfigurationException.class)
                .hasMessageContaining("Multiple ObjectMapper beans found");
    }

    @Test
    void shouldRejectIncompatibleDirectResponse() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("payload");

        assertThatThrownBy(() -> resolver().resolve(method, MethodExtensionDecision.reject("wrong"), ""))
                .isInstanceOf(MethodExtensionResponseException.class)
                .hasMessageContaining(String.class.getName())
                .hasMessageContaining(Payload.class.getName());
    }

    @Test
    void shouldRejectMalformedJson() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("payload");

        assertThatThrownBy(() -> resolver(new ObjectMapper())
                .resolve(method, MethodExtensionDecision.reject(), "not-json"))
                .isInstanceOf(MethodExtensionResponseException.class)
                .hasMessageContaining("Failed to convert returnJson");
    }

    @Test
    void shouldRejectUnresolvedTypeVariable() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("generic");

        assertThatThrownBy(() -> resolver(new ObjectMapper())
                .resolve(method, MethodExtensionDecision.reject(), "{}"))
                .isInstanceOf(MethodExtensionResponseException.class)
                .hasMessageContaining("unresolved type variable");
    }

    private MethodExtensionResponseResolver resolver(ObjectMapper... objectMappers) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        for (int index = 0; index < objectMappers.length; index++) {
            beanFactory.registerSingleton("objectMapper" + index, objectMappers[index]);
        }
        return new MethodExtensionResponseResolver(beanFactory.getBeanProvider(ObjectMapper.class));
    }

    static class SampleMethods {

        Payload payload() {
            return null;
        }

        int count() {
            return 0;
        }

        List<Payload> payloads() {
            return List.of();
        }

        CompletableFuture<Payload> asyncPayload() {
            return CompletableFuture.completedFuture(new Payload("business"));
        }

        String text() {
            return "";
        }

        void nothing() {
        }

        <T> T generic() {
            return null;
        }
    }

    record Payload(String code) {
    }
}
