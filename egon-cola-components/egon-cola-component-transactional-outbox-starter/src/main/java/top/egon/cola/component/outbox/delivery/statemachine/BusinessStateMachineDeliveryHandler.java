package top.egon.cola.component.outbox.delivery.statemachine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.outbox.statemachine.BusinessStateMachineDefinitionStrategy;
import top.egon.cola.component.outbox.statemachine.BusinessStateMachineEvent;
import top.egon.cola.component.outbox.statemachine.BusinessStateMachineService;

import java.util.Map;

/** Adapts the existing outbox delivery SPI to an optional business state-machine consumer. */
@Slf4j
@Validated
@RequiredArgsConstructor
public class BusinessStateMachineDeliveryHandler implements DeliveryHandler {

    public static final String CHANNEL = "statemachine";

    @Qualifier("outboxBusinessStateMachineService")
    private final BusinessStateMachineService service;

    @Qualifier("outboxBusinessStateMachineDefinitions")
    private final Map<String, BusinessStateMachineDefinitionStrategy> definitions;

    @Qualifier("outboxStateMachineObjectMapper")
    private final ObjectMapper objectMapper;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public void validateDestination(@NotBlank String destination) {
        if (destination == null || destination.isBlank() || !definitions.containsKey(destination)) {
            log.warn("Rejecting unknown business state-machine destination");
            throw new IllegalArgumentException("OUTBOX_SM_DEFINITION_MISSING");
        }
    }

    @Override
    public DeliveryResult deliver(@NotNull @Valid DeliveryContext context) {
        BusinessStateMachineEvent event;
        try {
            if (context == null || !CHANNEL.equals(context.channel())
                    || !"application/json".equals(context.contentType())
                    || !"1".equals(context.schemaVersion())) {
                return eventInvalid();
            }
            ObjectReader reader = objectMapper.readerFor(BusinessStateMachineEvent.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            event = reader.readValue(context.payload());
            validationUtils.validate(event);
        } catch (JsonProcessingException | ConstraintViolationException | IllegalArgumentException failure) {
            return eventInvalid();
        }

        String destination = event.getMachineKey() + ":" + event.getDefinitionVersion();
        if (!destination.equals(context.destination())) {
            return eventInvalid();
        }
        BusinessStateMachineDefinitionStrategy definition = definitions.get(destination);
        if (definition == null) {
            log.warn("Rejecting business state-machine delivery for unknown destination {}", destination);
            return DeliveryResult.permanentFailure(
                    "OUTBOX_SM_DEFINITION_MISSING",
                    "Business state-machine definition is not registered");
        }
        var headers = context.headers();
        if (!event.getEventId().equals(headers.get("egon-event-id"))
                || !event.getMachineKey().equals(headers.get("egon-machine-key"))
                || !event.getDefinitionVersion().toString().equals(headers.get("egon-definition-version"))) {
            return eventInvalid();
        }
        if (!event.getTenantId().toString().equals(headers.get("egon-tenant-id"))) {
            log.warn("Rejecting business state-machine event with mismatched tenant header");
            return DeliveryResult.permanentFailure(
                    "OUTBOX_SM_TENANT_REJECTED",
                    "Business event tenant header does not match its body");
        }
        return service.process(event, context);
    }

    private DeliveryResult eventInvalid() {
        log.warn("Rejecting invalid business state-machine delivery envelope");
        return DeliveryResult.permanentFailure(
                "OUTBOX_SM_EVENT_INVALID",
                "Business state-machine event envelope is invalid");
    }
}
