package top.egon.cola.component.outbox.store;

import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.util.Objects;

public class OutboxSchemaValidator extends BaseValidator {

    private final OutboxStore outboxStore;
    private final ValidationUtils validationUtils;

    public OutboxSchemaValidator(
            OutboxStore outboxStore,
            ValidationUtils validationUtils
    ) {
        this.outboxStore = Objects.requireNonNull(outboxStore, "outboxStore");
        this.validationUtils = Objects.requireNonNull(validationUtils, "validationUtils");
    }

    @Override
    protected ValidationUtils getValidationUtils() {
        return validationUtils;
    }

    public void validate() {
        // Schema probing is a read-only metadata check on the store; it has no bean to constrain.
        outboxStore.validateSchema();
    }
}
