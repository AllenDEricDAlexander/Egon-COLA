package top.egon.cola.component.outbox.serialization;

public interface OutboxMessageSerializer {

    SerializedOutboxPayload serialize(Object payload, String contentType);
}
