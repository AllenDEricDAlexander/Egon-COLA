package top.egon.cola.component.outbox.transaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.outbox.api.OutboxIdGenerator;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryHandlerRegistry;
import top.egon.cola.component.outbox.exception.OutboxSerializationException;
import top.egon.cola.component.outbox.observability.OutboxMetrics;
import top.egon.cola.component.outbox.serialization.OutboxMessageFingerprint;
import top.egon.cola.component.outbox.serialization.OutboxMessageSerializer;
import top.egon.cola.component.outbox.serialization.SerializedOutboxPayload;
import top.egon.cola.component.outbox.store.NewOutboxRecord;
import top.egon.cola.component.outbox.store.OutboxStore;
import top.egon.cola.component.outbox.validation.OutboxMessageValidator;

import java.util.TreeMap;

public class DefaultTransactionalOutbox implements TransactionalOutbox {

    private final OutboxMessageValidator validator;
    private final OutboxMessageSerializer serializer;
    private final OutboxIdGenerator idGenerator;
    private final ObjectMapper objectMapper;
    private final OutboxTransactionGuard transactionGuard;
    private final OutboxStore store;
    private final DeliveryHandlerRegistry handlerRegistry;
    private final OutboxAfterCommitBuffer afterCommitBuffer;
    private final OutboxMetrics metrics;
    private final TransactionalOutboxProperties properties;

    public DefaultTransactionalOutbox(
            OutboxMessageValidator validator,
            OutboxMessageSerializer serializer,
            OutboxIdGenerator idGenerator,
            ObjectMapper objectMapper,
            OutboxTransactionGuard transactionGuard,
            OutboxStore store,
            DeliveryHandlerRegistry handlerRegistry,
            OutboxAfterCommitBuffer afterCommitBuffer,
            OutboxMetrics metrics,
            TransactionalOutboxProperties properties
    ) {
        this.validator = validator;
        this.serializer = serializer;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
        this.transactionGuard = transactionGuard;
        this.store = store;
        this.handlerRegistry = handlerRegistry;
        this.afterCommitBuffer = afterCommitBuffer;
        this.metrics = metrics;
        this.properties = properties;
    }

    @Override
    public OutboxReceipt enqueue(OutboxMessage message) {
        transactionGuard.requireSelectedTransaction();
        validator.validateEnvelope(message);
        String messageId = message.messageId() == null ? idGenerator.nextId() : message.messageId();
        SerializedOutboxPayload serializedPayload =
                serializer.serialize(message.payload(), message.contentType());
        validator.validateSerialized(serializedPayload, message.headers());
        DeliveryHandler handler = handlerRegistry.required(message.channel());
        handler.validateDestination(message.destination());
        String headersJson = serializeHeaders(message);
        String fingerprint = OutboxMessageFingerprint.sha256(
                message.channel(),
                message.destination(),
                serializedPayload.text(),
                message.contentType(),
                message.schemaVersion(),
                message.headers()
        );
        OutboxReceipt receipt = store.enqueue(new NewOutboxRecord(
                messageId,
                message.idempotencyKey(),
                fingerprint,
                message.channel(),
                message.destination(),
                serializedPayload.text(),
                message.contentType(),
                message.schemaVersion(),
                headersJson,
                message.traceId(),
                message.availableAt(),
                properties.getRetry().getMaxAttempts()
        ));
        metrics.enqueue(receipt.created());
        if (receipt.created()) {
            afterCommitBuffer.record(receipt.messageId());
        }
        return receipt;
    }

    private String serializeHeaders(OutboxMessage message) {
        try {
            return objectMapper.writeValueAsString(new TreeMap<>(message.headers()));
        } catch (JsonProcessingException exception) {
            throw new OutboxSerializationException("Failed to serialize outbox headers", exception);
        }
    }
}
