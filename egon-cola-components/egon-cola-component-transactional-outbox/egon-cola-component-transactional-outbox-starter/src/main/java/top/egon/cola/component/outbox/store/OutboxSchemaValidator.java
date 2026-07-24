package top.egon.cola.component.outbox.store;

public class OutboxSchemaValidator {

    private final OutboxStore outboxStore;

    public OutboxSchemaValidator(OutboxStore outboxStore) {
        this.outboxStore = outboxStore;
    }

    public void validate() {
        outboxStore.validateSchema();
    }
}
