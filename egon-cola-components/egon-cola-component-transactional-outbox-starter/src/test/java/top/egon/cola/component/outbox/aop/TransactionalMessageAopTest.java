package top.egon.cola.component.outbox.aop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import top.egon.cola.component.outbox.annotation.TransactionalMessage;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;
import top.egon.cola.component.outbox.exception.OutboxStorageException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransactionalMessageAopTest {

    private final TransactionalOutbox outbox = mock(TransactionalOutbox.class);
    private final RecordingTransactionManager transactionManager =
            new RecordingTransactionManager();
    private OrderService target;
    private OrderService proxy;

    @BeforeEach
    void setUp() {
        target = new OrderService();
        TransactionalMessageAop advisor = new TransactionalMessageAop(
                outbox,
                transactionManager,
                "ordersTransactionManager",
                new OutboxMessageExpressionResolver(),
                new TransactionalMessageMethodValidator("ordersTransactionManager"),
                new TransactionalOutboxProperties()
        );
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvisor(advisor);
        proxy = (OrderService) proxyFactory.getProxy();
    }

    @Test
    void shouldInvokeTargetThenResolveResultAndEnqueueInsideOneRequiredTransaction() {
        CreateOrderResult result = proxy.create(new CreateOrderRequest("O-1"));

        assertThat(result.orderId()).isEqualTo("O-1");
        assertThat(target.calls()).isEqualTo(1);
        verify(outbox).enqueue(result.outboxMessage());
        assertThat(transactionManager.commits).isEqualTo(1);
        assertThat(transactionManager.lastDefinition.getPropagationBehavior())
                .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    @Test
    void shouldRollBackAndPreserveCheckedBusinessException() {
        assertThatThrownBy(proxy::checkedFailure).isSameAs(target.checkedException);

        verifyNoInteractions(outbox);
        assertThat(transactionManager.rollbacks).isEqualTo(1);
    }

    @Test
    void shouldRollBackBusinessWorkWhenEnqueueFailsWithoutInvokingTwice() {
        when(outbox.enqueue(any())).thenThrow(new OutboxStorageException("insert failed"));

        assertThatThrownBy(() -> proxy.create(new CreateOrderRequest("O-1")))
                .isInstanceOf(OutboxStorageException.class);

        assertThat(target.calls()).isEqualTo(1);
        assertThat(transactionManager.rollbacks).isEqualTo(1);
    }

    static class OrderService {

        private int calls;
        private final CheckedBusinessException checkedException =
                new CheckedBusinessException("business");

        @TransactionalMessage(message = "#result.outboxMessage()")
        public CreateOrderResult create(CreateOrderRequest request) {
            calls++;
            return new CreateOrderResult(request.orderId(), message(request.orderId()));
        }

        @TransactionalMessage(message = "#result.outboxMessage()")
        public CreateOrderResult checkedFailure() throws CheckedBusinessException {
            calls++;
            throw checkedException;
        }

        int calls() {
            return calls;
        }

        private static OutboxMessage message(String orderId) {
            return OutboxMessage.builder()
                    .idempotencyKey(orderId)
                    .channel("http")
                    .destination("orders")
                    .payload(Map.of("orderId", orderId))
                    .build();
        }
    }

    record CreateOrderRequest(String orderId) {
    }

    record CreateOrderResult(String orderId, OutboxMessage outboxMessage) {
    }

    static class CheckedBusinessException extends Exception {

        CheckedBusinessException(String message) {
            super(message);
        }
    }

    private static final class RecordingTransactionManager
            implements org.springframework.transaction.PlatformTransactionManager {

        private int commits;
        private int rollbacks;
        private TransactionDefinition lastDefinition;

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition)
                throws TransactionException {
            lastDefinition = definition;
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) throws TransactionException {
            if (status.isRollbackOnly()) {
                rollbacks++;
            } else {
                commits++;
            }
        }

        @Override
        public void rollback(TransactionStatus status) throws TransactionException {
            rollbacks++;
        }
    }
}
