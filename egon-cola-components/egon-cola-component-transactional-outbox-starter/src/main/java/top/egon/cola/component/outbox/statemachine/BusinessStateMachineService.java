package top.egon.cola.component.outbox.statemachine;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.autoconfigure.OutboxStateMachineProperties;
import top.egon.cola.component.outbox.common.exception.OutboxStateMachineException;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.outbox.serialization.OutboxMessageFingerprint;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/** Applies one delivered business fact through its registered local state machine. */
@Slf4j
@Validated
@RequiredArgsConstructor
public class BusinessStateMachineService {

    @Qualifier("outboxBusinessStateMachineDefinitions")
    private final Map<String, BusinessStateMachineDefinitionStrategy> definitions;

    @Qualifier("outboxBusinessStateMachineContextExecutor")
    private final BusinessStateMachineContextExecutor contextExecutor;

    @Qualifier("outboxBusinessStateMachineTransaction")
    private final TransactionTemplate transactionTemplate;

    @Qualifier("outboxStateMachineExecutionService")
    private final StateMachineExecutionService executionService;

    @Qualifier("outboxStateMachineProperties")
    private final OutboxStateMachineProperties properties;

    @Qualifier("outboxStateMachineObjectMapper")
    private final ObjectMapper objectMapper;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Qualifier("outboxStateMachineClock")
    private final Clock clock;

    public DeliveryResult process(
            @NotNull @Valid BusinessStateMachineEvent event,
            @NotNull DeliveryContext context
    ) {
        if (event == null || context == null) {
            return eventInvalid();
        }

        BusinessStateMachineEvent stableEvent;
        try {
            validationUtils.validate(event);
            stableEvent = copyEvent(event);
        } catch (ConstraintViolationException | IllegalArgumentException failure) {
            return eventInvalid();
        }
        DeliveryResult contextFailure = validateContext(stableEvent, context);
        if (contextFailure != null) {
            return contextFailure;
        }

        String destination = destination(stableEvent);
        BusinessStateMachineDefinitionStrategy definition = definitions.get(destination);
        if (definition == null) {
            return permanentFailure("OUTBOX_SM_DEFINITION_MISSING", "No business state-machine definition is registered");
        }

        String fingerprint;
        try {
            fingerprint = fingerprint(stableEvent, destination);
        } catch (JsonProcessingException | IllegalArgumentException failure) {
            return eventInvalid();
        }
        Instant businessDeadline;
        try {
            businessDeadline = clock.instant().plus(properties.getBusiness().getTimeout());
        } catch (ArithmeticException failure) {
            return permanentFailure("OUTBOX_SM_EXECUTION_FAILED", "Business state-machine timeout is invalid");
        }
        Instant deadline = context.deadline().isBefore(businessDeadline) ? context.deadline() : businessDeadline;

        try {
            DeliveryResult result = contextExecutor.execute(stableEvent, () -> {
                try {
                    definition.validateEvent(stableEvent);
                } catch (ConstraintViolationException failure) {
                    return eventInvalid();
                }
                DeliveryResult transactionResult = transactionTemplate.execute(status ->
                        processInBusinessTransaction(stableEvent, definition, fingerprint, deadline));
                return transactionResult == null
                        ? permanentFailure("OUTBOX_SM_EXECUTION_FAILED", "Business state-machine transaction returned no result")
                        : transactionResult;
            });
            return result == null
                    ? permanentFailure("OUTBOX_SM_EXECUTION_FAILED", "Business state-machine context returned no result")
                    : result;
        } catch (RuntimeException failure) {
            return classifyFailure(destination, stableEvent, failure);
        }
    }

    private BusinessStateMachineEvent copyEvent(BusinessStateMachineEvent event) {
        ObjectNode payload = event.getPayload();
        return BusinessStateMachineEvent.builder()
                .eventId(event.getEventId())
                .tenantId(event.getTenantId())
                .machineKey(event.getMachineKey())
                .definitionVersion(event.getDefinitionVersion())
                .businessId(event.getBusinessId())
                .eventType(event.getEventType())
                .expectedVersion(event.getExpectedVersion())
                .occurredAt(event.getOccurredAt())
                .payload(payload == null ? null : payload.deepCopy())
                .build();
    }

    private DeliveryResult validateContext(BusinessStateMachineEvent event, DeliveryContext context) {
        if (context.messageId() == null || context.messageId().isBlank() || context.messageId().length() > 64
                || !"statemachine".equals(context.channel())
                || !destination(event).equals(context.destination())
                || !"application/json".equals(context.contentType())
                || !"1".equals(context.schemaVersion())
                || context.attempt() < 0 || context.maxAttempts() < 1 || context.deadline() == null) {
            return eventInvalid();
        }
        Map<String, String> headers = context.headers();
        if (!event.getEventId().equals(headers.get("egon-event-id"))
                || !event.getMachineKey().equals(headers.get("egon-machine-key"))
                || !event.getDefinitionVersion().toString().equals(headers.get("egon-definition-version"))) {
            return eventInvalid();
        }
        if (!event.getTenantId().toString().equals(headers.get("egon-tenant-id"))) {
            return permanentFailure("OUTBOX_SM_TENANT_REJECTED", "Business event tenant header does not match its body");
        }
        if (!clock.instant().isBefore(context.deadline())) {
            return retryableFailure("OUTBOX_SM_TIMEOUT", "Business state-machine delivery deadline has expired");
        }
        return null;
    }

    private DeliveryResult processInBusinessTransaction(
            BusinessStateMachineEvent event,
            BusinessStateMachineDefinitionStrategy definition,
            String fingerprint,
            Instant deadline
    ) {
        if (!clock.instant().isBefore(deadline)) {
            return retryableFailure("OUTBOX_SM_TIMEOUT", "Business state-machine delivery deadline has expired");
        }
        BusinessStateMachineSnapshotBO snapshot = definition.repository().lockAndLoad(event, fingerprint);
        if (snapshot == null) {
            return permanentFailure("OUTBOX_SM_RESOURCE_NOT_FOUND", "Business state-machine resource was not found");
        }
        try {
            validationUtils.validate(snapshot);
        } catch (ConstraintViolationException failure) {
            throw new OutboxStateMachineException(
                    "OUTBOX_FSM_EXECUTION_FAILED", false, "Business repository returned an invalid snapshot", failure);
        }

        String appliedFingerprint = snapshot.getAppliedFingerprint();
        if (appliedFingerprint != null) {
            return appliedFingerprint.equals(fingerprint)
                    ? DeliveryResult.success()
                    : permanentFailure("OUTBOX_SM_EVENT_CONFLICT", "Business event identity has a different fingerprint");
        }
        if (!event.getDefinitionVersion().equals(snapshot.getDefinitionVersion())) {
            return permanentFailure("OUTBOX_SM_DEFINITION_MISSING", "Business state-machine definition version does not match");
        }
        Long expectedVersion = event.getExpectedVersion();
        if (expectedVersion != null && expectedVersion < snapshot.getVersion()) {
            return permanentFailure("OUTBOX_SM_STALE_VERSION", "Business event expected an older aggregate version");
        }
        if (expectedVersion != null && expectedVersion > snapshot.getVersion()) {
            return retryableFailure("OUTBOX_SM_VERSION_GAP", "Business event is ahead of the aggregate version");
        }

        Duration timeout = remaining(deadline);
        if (timeout.isNegative() || timeout.isZero()) {
            return retryableFailure("OUTBOX_SM_TIMEOUT", "Business state-machine delivery deadline has expired");
        }
        StateMachine<String, String> machine = definition.stateMachineFactory().getStateMachine();
        if (machine == null
                || machine.getStateMachineAccessor().withAllRegions() == null
                || machine.getStateMachineAccessor().withAllRegions().size() != 1) {
            return permanentFailure("OUTBOX_SM_EXECUTION_FAILED", "Business state-machine factory returned an invalid machine");
        }
        ObjectNode facts = snapshot.getFacts();
        if (facts == null) {
            throw new OutboxStateMachineException(
                    "OUTBOX_FSM_EXECUTION_FAILED", false, "Business repository returned null state-machine facts");
        }
        Message<String> message = MessageBuilder.withPayload(event.getEventType())
                .setHeader("businessStateMachineEvent", event)
                .setHeader("businessStateMachineFacts", facts)
                .build();
        String targetState = executionService.execute(
                machine, snapshot.getCurrentState(), message, timeout);
        if (!clock.instant().isBefore(deadline)) {
            throw new OutboxStateMachineException(
                    "OUTBOX_FSM_TIMEOUT", true, "Business state-machine delivery deadline expired before persistence");
        }
        definition.repository().saveTransition(event, snapshot, targetState, fingerprint);
        if (!clock.instant().isBefore(deadline)) {
            throw new OutboxStateMachineException(
                    "OUTBOX_FSM_TIMEOUT", true, "Business state-machine delivery deadline expired before commit");
        }
        return DeliveryResult.success();
    }

    private String fingerprint(BusinessStateMachineEvent event, String destination) throws JsonProcessingException {
        ObjectMapper fingerprintMapper = objectMapper.copy();
        fingerprintMapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(
                JsonInclude.Include.ALWAYS, JsonInclude.Include.ALWAYS));
        JsonNode canonicalEvent = canonicalize(fingerprintMapper.valueToTree(event), fingerprintMapper);
        String canonicalPayload = fingerprintMapper.writeValueAsString(canonicalEvent);
        return OutboxMessageFingerprint.sha256(
                "statemachine", destination, canonicalPayload, "application/json", "1", Map.of());
    }

    private JsonNode canonicalize(JsonNode value, ObjectMapper mapper) {
        if (value.isObject()) {
            ObjectNode sorted = mapper.createObjectNode();
            Map<String, JsonNode> fields = new TreeMap<>();
            Iterator<Map.Entry<String, JsonNode>> iterator = value.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                fields.put(entry.getKey(), entry.getValue());
            }
            fields.forEach((name, child) -> sorted.set(name, canonicalize(child, mapper)));
            return sorted;
        }
        if (value.isArray()) {
            ArrayNode sorted = mapper.createArrayNode();
            value.forEach(child -> sorted.add(canonicalize(child, mapper)));
            return sorted;
        }
        return value.deepCopy();
    }

    private Duration remaining(Instant deadline) {
        return Duration.between(clock.instant(), deadline);
    }

    private static String destination(BusinessStateMachineEvent event) {
        return event.getMachineKey() + ":" + event.getDefinitionVersion();
    }

    private DeliveryResult classifyFailure(
            String destination,
            BusinessStateMachineEvent event,
            RuntimeException failure
    ) {
        if (failure instanceof OutboxStateMachineException stateMachineFailure) {
            if ("OUTBOX_FSM_TIMEOUT".equals(stateMachineFailure.getReason())) {
                return retryableFailure("OUTBOX_SM_TIMEOUT", "Business state-machine execution timed out");
            }
            if ("OUTBOX_FSM_REJECTED".equals(stateMachineFailure.getReason())) {
                return permanentFailure("OUTBOX_SM_TRANSITION_REJECTED", "Business state-machine rejected the event");
            }
            if ("OUTBOX_FSM_INPUT_INVALID".equals(stateMachineFailure.getReason())) {
                return eventInvalid();
            }
            return stateMachineFailure.isRetryable()
                    ? retryableFailure("OUTBOX_SM_EXECUTION_FAILED", "Business state-machine execution must be retried")
                    : permanentFailure("OUTBOX_SM_EXECUTION_FAILED", "Business state-machine execution failed");
        }
        if (failure instanceof SecurityException) {
            return permanentFailure("OUTBOX_SM_TENANT_REJECTED", "Business state-machine tenant was rejected");
        }
        if (failure instanceof DuplicateKeyException) {
            return retryableFailure("OUTBOX_SM_CONCURRENT_CHANGE", "Business state-machine receipt changed concurrently");
        }
        if (failure instanceof TransientDataAccessException || failure instanceof TransactionSystemException) {
            return retryableFailure("OUTBOX_SM_STORAGE_RETRY", "Business state-machine storage result must be retried");
        }
        if (failure instanceof ConstraintViolationException || failure instanceof IllegalArgumentException) {
            return eventInvalid();
        }
        if (failure instanceof DataAccessException) {
            log.warn("Business state-machine storage rejected event {} for destination {}",
                    event.getEventId(), destination);
        } else {
            log.error("Business state-machine execution failed for event {} and destination {}",
                    event.getEventId(), destination, failure.getClass().getSimpleName());
        }
        return permanentFailure("OUTBOX_SM_EXECUTION_FAILED", "Business state-machine execution failed");
    }

    private static DeliveryResult eventInvalid() {
        return permanentFailure("OUTBOX_SM_EVENT_INVALID", "Business state-machine event envelope is invalid");
    }

    private static DeliveryResult permanentFailure(String code, String message) {
        return DeliveryResult.permanentFailure(code, message);
    }

    private static DeliveryResult retryableFailure(String code, String message) {
        return DeliveryResult.retryableFailure(code, message);
    }
}
