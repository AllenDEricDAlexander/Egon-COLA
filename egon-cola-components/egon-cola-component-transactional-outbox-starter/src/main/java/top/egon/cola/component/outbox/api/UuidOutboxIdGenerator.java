package top.egon.cola.component.outbox.api;

import java.util.UUID;

public class UuidOutboxIdGenerator implements OutboxIdGenerator {

    @Override
    public String nextId() {
        return UUID.randomUUID().toString();
    }
}
