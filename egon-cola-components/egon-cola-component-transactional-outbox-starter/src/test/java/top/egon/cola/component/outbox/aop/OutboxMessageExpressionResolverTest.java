package top.egon.cola.component.outbox.aop;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.exception.OutboxMessageResolutionException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxMessageExpressionResolverTest {

    private final OutboxMessageExpressionResolver resolver =
            new OutboxMessageExpressionResolver();
    private final Method method = SampleService.class.getDeclaredMethod(
            "create",
            Request.class
    );
    private final OutboxMessage requestMessage = message("request");
    private final OutboxMessage resultMessage = message("result");
    private final Request request = new Request(requestMessage);
    private final Result result = new Result(resultMessage);

    OutboxMessageExpressionResolverTest() throws NoSuchMethodException {
    }

    @Test
    void shouldResolveOnlyAllowedArgumentAndResultMethods() {
        Object[] arguments = {request};

        assertThat(resolver.resolve("#p0.outboxMessage()", method, arguments, null))
                .isEqualTo(requestMessage);
        assertThat(resolver.resolve("#a0.outboxMessage()", method, arguments, null))
                .isEqualTo(requestMessage);
        assertThat(resolver.resolve("#request.outboxMessage()", method, arguments, null))
                .isEqualTo(requestMessage);
        assertThat(resolver.resolve("#result.outboxMessage()", method, arguments, result))
                .isEqualTo(resultMessage);
    }

    @Test
    void shouldRejectUnsafeNullAndWrongTypeExpressionsWithoutEchoingPayload() {
        Object[] arguments = {request};

        for (String expression : new String[]{
                "#result",
                "#p0.label()",
                "@environment",
                "T(java.lang.System).getenv()",
                "new java.lang.String()",
                "#p0.getClass()",
                "T(java.util.Collections).emptyMap()"
        }) {
            assertThatThrownBy(() -> resolver.resolve(
                    expression,
                    method,
                    arguments,
                    null
            ))
                    .isInstanceOf(OutboxMessageResolutionException.class)
                    .hasMessageNotContaining("secret-payload")
                    .hasMessageNotContaining(expression);
        }
    }

    private OutboxMessage message(String id) {
        return OutboxMessage.builder()
                .idempotencyKey(id)
                .channel("http")
                .destination("orders")
                .payload(Map.of("value", "secret-payload"))
                .build();
    }

    static class SampleService {

        Result create(Request request) {
            return null;
        }
    }

    record Request(OutboxMessage message) {

        public OutboxMessage outboxMessage() {
            return message;
        }

        public String label() {
            return "wrong-type";
        }
    }

    record Result(OutboxMessage message) {

        public OutboxMessage outboxMessage() {
            return message;
        }
    }
}
