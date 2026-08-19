package top.egon.cola.component.outbox.aop;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.outbox.exception.OutboxConfigurationException;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionalMessageMethodValidatorTest {

    private final TransactionalMessageMethodValidator validator =
            new TransactionalMessageMethodValidator("ordersTransactionManager");

    @Test
    void shouldAcceptPublicSynchronousRequiredBoundary() throws Exception {
        Method method = Boundaries.class.getDeclaredMethod("valid");

        assertThatCode(() -> validator.validate(method, Boundaries.class))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectUnsupportedMethodShapes() throws Exception {
        for (String methodName : new String[]{
                "hidden",
                "staticMethod",
                "finalMethod",
                "future",
                "completionStage",
                "publisher"
        }) {
            Method method = Boundaries.class.getDeclaredMethod(methodName);
            assertThatThrownBy(() -> validator.validate(method, Boundaries.class))
                    .as(methodName)
                    .isInstanceOf(OutboxConfigurationException.class);
        }
    }

    @Test
    void shouldRejectReadOnlyNonRequiredAndDifferentTransactionManager() throws Exception {
        for (String methodName : new String[]{"readOnly", "requiresNew", "differentManager"}) {
            Method method = Boundaries.class.getDeclaredMethod(methodName);
            assertThatThrownBy(() -> validator.validate(method, Boundaries.class))
                    .as(methodName)
                    .isInstanceOf(OutboxConfigurationException.class);
        }
    }

    static class Boundaries {

        @Transactional
        public String valid() {
            return "ok";
        }

        String hidden() {
            return "hidden";
        }

        public static String staticMethod() {
            return "static";
        }

        public final String finalMethod() {
            return "final";
        }

        public Future<String> future() {
            return CompletableFuture.completedFuture("future");
        }

        public CompletionStage<String> completionStage() {
            return CompletableFuture.completedFuture("stage");
        }

        public Publisher<String> publisher() {
            return subscriber -> {
            };
        }

        @Transactional(readOnly = true)
        public String readOnly() {
            return "read-only";
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public String requiresNew() {
            return "new";
        }

        @Transactional(transactionManager = "otherTransactionManager")
        public String differentManager() {
            return "other";
        }
    }
}
