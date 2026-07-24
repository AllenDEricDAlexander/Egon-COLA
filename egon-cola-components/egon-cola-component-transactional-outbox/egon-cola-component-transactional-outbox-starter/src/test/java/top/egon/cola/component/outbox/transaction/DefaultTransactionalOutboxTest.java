package top.egon.cola.component.outbox.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import top.egon.cola.component.outbox.api.OutboxIdGenerator;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryHandlerRegistry;
import top.egon.cola.component.outbox.serialization.OutboxMessageSerializer;
import top.egon.cola.component.outbox.serialization.SerializedOutboxPayload;
import top.egon.cola.component.outbox.observability.NoopOutboxMetrics;
import top.egon.cola.component.outbox.store.NewOutboxRecord;
import top.egon.cola.component.outbox.store.OutboxStore;
import top.egon.cola.component.outbox.validation.OutboxMessageValidator;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultTransactionalOutboxTest {

    private final OutboxMessageValidator validator = mock(OutboxMessageValidator.class);
    private final OutboxMessageSerializer serializer = mock(OutboxMessageSerializer.class);
    private final OutboxIdGenerator idGenerator = mock(OutboxIdGenerator.class);
    private final OutboxTransactionGuard transactionGuard = mock(OutboxTransactionGuard.class);
    private final OutboxStore store = mock(OutboxStore.class);
    private final OutboxAfterCommitBuffer afterCommitBuffer = mock(OutboxAfterCommitBuffer.class);
    private final DeliveryHandler handler = mock(DeliveryHandler.class);
    private final TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
    private DefaultTransactionalOutbox outbox;

    @BeforeEach
    void setUp() {
        when(handler.channel()).thenReturn("http");
        when(serializer.serialize(any(), any())).thenReturn(new SerializedOutboxPayload("{}", 2));
        when(idGenerator.nextId()).thenReturn("generated-id");
        outbox = new DefaultTransactionalOutbox(
                validator,
                serializer,
                idGenerator,
                new ObjectMapper(),
                transactionGuard,
                store,
                new DeliveryHandlerRegistry(List.of(handler)),
                afterCommitBuffer,
                new NoopOutboxMetrics(),
                properties
        );
    }

    @Test
    void shouldValidateAndSerializeBeforePersistence() {
        OutboxMessage message = message(null);
        when(store.enqueue(any())).thenReturn(new OutboxReceipt("generated-id", "order-1", true));

        outbox.enqueue(message);

        InOrder order = inOrder(transactionGuard, validator, serializer, handler, store);
        order.verify(transactionGuard).requireSelectedTransaction();
        order.verify(validator).validateEnvelope(message);
        order.verify(serializer).serialize(message.payload(), message.contentType());
        order.verify(validator).validateSerialized(any(), any());
        order.verify(handler).validateDestination("orders");
        order.verify(store).enqueue(any());
    }

    @Test
    void shouldValidateDestinationBeforePersistence() {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("destination"))
                .when(handler).validateDestination("orders");

        assertThatThrownBy(() -> outbox.enqueue(message(null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(store, never()).enqueue(any());
    }

    @Test
    void shouldUseOneGeneratedIdForTheRecordReceiptAndWakeSignal() {
        when(store.enqueue(any())).thenAnswer(invocation -> {
            NewOutboxRecord record = invocation.getArgument(0);
            return new OutboxReceipt(record.messageId(), record.idempotencyKey(), true);
        });

        OutboxReceipt receipt = outbox.enqueue(message(null));

        ArgumentCaptor<NewOutboxRecord> recordCaptor = ArgumentCaptor.forClass(NewOutboxRecord.class);
        verify(store).enqueue(recordCaptor.capture());
        assertThat(recordCaptor.getValue().messageId()).isEqualTo("generated-id");
        assertThat(recordCaptor.getValue().headersJson()).isEqualTo("{\"a\":\"1\",\"z\":\"2\"}");
        assertThat(recordCaptor.getValue().maxAttempts())
                .isEqualTo(properties.getRetry().getMaxAttempts());
        assertThat(receipt.messageId()).isEqualTo("generated-id");
        verify(afterCommitBuffer).record("generated-id");
        verify(idGenerator).nextId();
    }

    @Test
    void shouldNotRecordAnotherWakeSignalForAnExistingMessage() {
        when(store.enqueue(any())).thenReturn(new OutboxReceipt("existing-id", "order-1", false));

        OutboxReceipt receipt = outbox.enqueue(message("existing-id"));

        assertThat(receipt.created()).isFalse();
        verify(afterCommitBuffer, never()).record(any());
        verify(idGenerator, never()).nextId();
    }

    @Test
    void shouldPropagateStoreFailures() {
        IllegalStateException failure = new IllegalStateException("storage failed");
        when(store.enqueue(any())).thenThrow(failure);

        assertThatThrownBy(() -> outbox.enqueue(message(null))).isSameAs(failure);

        verify(afterCommitBuffer, never()).record(any());
    }

    private OutboxMessage message(String messageId) {
        return OutboxMessage.builder()
                .messageId(messageId)
                .idempotencyKey("order-1")
                .channel("http")
                .destination("orders")
                .payload(Map.of("orderId", "1"))
                .headers(Map.of("z", "2", "a", "1"))
                .build();
    }
}
