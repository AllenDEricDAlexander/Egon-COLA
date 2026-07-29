package top.egon.cola.component.accessguard.execution;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.api.AccessGuardRejectedException;
import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.GuardEntryType;
import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.GuardInvocationKind;
import top.egon.cola.component.accessguard.core.GuardOutcome;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultRejectionHandlerTest {

    @Test
    void throwModeUsesTheStableAccessGuardException() throws Exception {
        DefaultRejectionHandler handler = handler(new ObjectMapper(), (invocation, outcome, fallback) -> null);
        GuardOutcome outcome = rejected();

        assertThatThrownBy(() -> handler.resolve(invocation(String.class), outcome, rejection(RejectionMode.THROW, "")))
                .isInstanceOf(AccessGuardRejectedException.class)
                .satisfies(exception -> assertThat(((AccessGuardRejectedException) exception).outcome()).isSameAs(outcome));
    }

    @Test
    void returnJsonUsesTheInjectedApplicationObjectMapper() throws Throwable {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new JsonDeserializer<>() {
            @Override
            public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                return "custom:" + parser.getValueAsString();
            }
        });
        mapper.registerModule(module);
        DefaultRejectionHandler handler = handler(mapper, (invocation, outcome, fallback) -> null);

        Object value = handler.resolve(
                invocation(String.class),
                rejected(),
                rejection(RejectionMode.RETURN_JSON, "\"denied\""));

        assertThat(value).isEqualTo("custom:denied");
    }

    @Test
    void invalidJsonAndPrimitiveNullAreRejectedWithoutBusinessExecution() throws Exception {
        AtomicInteger businessCalls = new AtomicInteger();
        DefaultRejectionHandler handler = handler(new ObjectMapper(), (invocation, outcome, fallback) -> null);

        assertThatThrownBy(() -> handler.resolve(
                invocation(String.class, businessCalls),
                rejected(),
                rejection(RejectionMode.RETURN_JSON, "{")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> handler.resolve(
                invocation(int.class, businessCalls),
                rejected(),
                rejection(RejectionMode.RETURN_NULL, "")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(businessCalls).hasValue(0);
    }

    @Test
    void fallbackModeDelegatesOnce() throws Throwable {
        AtomicInteger calls = new AtomicInteger();
        DefaultRejectionHandler handler = handler(new ObjectMapper(), (invocation, outcome, fallback) -> {
            calls.incrementAndGet();
            return "fallback";
        });

        assertThat(handler.resolve(
                invocation(String.class),
                rejected(),
                new ExecutionConfig.RejectionConfig(RejectionMode.FALLBACK, "fallback", "")))
                .isEqualTo("fallback");
        assertThat(calls).hasValue(1);
    }

    private static DefaultRejectionHandler handler(ObjectMapper mapper, FallbackHandler fallback) {
        return new DefaultRejectionHandler(fallback, new JsonRejectValueParser(mapper));
    }

    private static ExecutionConfig.RejectionConfig rejection(RejectionMode mode, String json) {
        return new ExecutionConfig.RejectionConfig(mode, "", json);
    }

    private static GuardOutcome rejected() {
        return GuardOutcome.rejected("draw", GuardDecision.RATE_LIMITED, "rate-limit", 1L);
    }

    private static GuardInvocation invocation(Class<?> returnType) throws Exception {
        return invocation(returnType, new AtomicInteger());
    }

    private static GuardInvocation invocation(Class<?> returnType, AtomicInteger businessCalls) throws Exception {
        Method method = returnType == int.class
                ? ReturnTypes.class.getDeclaredMethod("primitiveValue")
                : ReturnTypes.class.getDeclaredMethod("stringValue");
        return new GuardInvocation(
                "draw", new ReturnTypes(), ReturnTypes.class, method, new Object[0], Map.of(),
                GuardEntryType.AOP, GuardInvocationKind.METHOD,
                () -> {
                    businessCalls.incrementAndGet();
                    return "business";
                });
    }

    static class ReturnTypes {

        String stringValue() {
            return "value";
        }

        int primitiveValue() {
            return 1;
        }
    }
}
