package top.egon.cola.component.outbox.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.statemachine.transition.TransitionKind;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.common.exception.OutboxConfigurationException;
import top.egon.cola.component.outbox.delivery.statemachine.BusinessStateMachineDeliveryHandler;
import top.egon.cola.component.outbox.statemachine.BusinessStateMachineContextExecutor;
import top.egon.cola.component.outbox.statemachine.BusinessStateMachineDefinitionStrategy;
import top.egon.cola.component.outbox.statemachine.BusinessStateMachineRepository;
import top.egon.cola.component.outbox.statemachine.BusinessStateMachineService;
import top.egon.cola.component.outbox.statemachine.StateMachineExecutionService;

import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Conditional auto-configuration for business-owned state-machine event consumers. */
@Slf4j
@AutoConfiguration(after = {TransactionalOutboxAutoConfiguration.class, OutboxStateMachineAutoConfiguration.class})
@ConditionalOnClass(StateMachine.class)
@ConditionalOnProperty(
        prefix = OutboxStateMachineProperties.PREFIX + ".business",
        name = "enabled",
        havingValue = "true"
)
public class OutboxBusinessStateMachineAutoConfiguration {

    private static final String DESTINATION_PATTERN = "[a-z][a-z0-9.-]{0,63}:[1-9][0-9]*";

    @Bean("outboxBusinessStateMachineDefinitions")
    @ConditionalOnMissingBean(name = "outboxBusinessStateMachineDefinitions")
    public Map<String, BusinessStateMachineDefinitionStrategy> outboxBusinessStateMachineDefinitions(
            ObjectProvider<BusinessStateMachineDefinitionStrategy> strategies,
            ObjectProvider<BusinessStateMachineContextExecutor> contextExecutors,
            @Qualifier("egonColaValidationUtils") ValidationUtils validationUtils
    ) {
        List<BusinessStateMachineContextExecutor> configuredExecutors = contextExecutors.orderedStream().toList();
        if (configuredExecutors.size() != 1) {
            throw new OutboxConfigurationException(
                    "Business state-machine context executor must have exactly one bean when enabled");
        }
        List<BusinessStateMachineDefinitionStrategy> configured = strategies.orderedStream().toList();
        if (configured.isEmpty()) {
            throw new OutboxConfigurationException(
                    "At least one business state-machine definition must be registered when enabled");
        }
        LinkedHashMap<String, BusinessStateMachineDefinitionStrategy> definitions = new LinkedHashMap<>();
        for (BusinessStateMachineDefinitionStrategy strategy : configured) {
            validationUtils.validate(strategy);
            String destination = strategy.destination();
            StateMachineFactory<String, String> factory = strategy.stateMachineFactory();
            BusinessStateMachineRepository repository = strategy.repository();
            if (destination == null || !destination.matches(DESTINATION_PATTERN)
                    || factory == null || repository == null) {
                throw new OutboxConfigurationException("Business state-machine definition contract is invalid");
            }
            validateFactory(destination, factory);
            if (definitions.putIfAbsent(destination, strategy) != null) {
                throw new OutboxConfigurationException(
                        "Duplicate business state-machine destination: " + destination);
            }
        }
        return Collections.unmodifiableMap(definitions);
    }

    @Bean("outboxBusinessStateMachineTransaction")
    @ConditionalOnMissingBean(name = "outboxBusinessStateMachineTransaction")
    public TransactionTemplate outboxBusinessStateMachineTransaction(
            @Qualifier("outboxInfrastructure") OutboxInfrastructure infrastructure,
            @Qualifier("outboxStateMachineProperties") OutboxStateMachineProperties properties,
            @Qualifier("egonColaValidationUtils") ValidationUtils validationUtils
    ) {
        validationUtils.validate(properties);
        Duration timeout = properties.getBusiness().getTimeout();
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new OutboxConfigurationException(
                    "Business state-machine transaction timeout must be positive");
        }
        long timeoutMillis;
        try {
            timeoutMillis = timeout.toMillis();
        } catch (ArithmeticException failure) {
            throw new OutboxConfigurationException("Business state-machine transaction timeout is too large", failure);
        }
        long timeoutSeconds = timeoutMillis / 1000 + (timeoutMillis % 1000 == 0 ? 0 : 1);
        TransactionTemplate transaction = new TransactionTemplate(infrastructure.transactionManager());
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        try {
            transaction.setTimeout(Math.toIntExact(Math.max(1, timeoutSeconds)));
        } catch (ArithmeticException failure) {
            throw new OutboxConfigurationException("Business state-machine transaction timeout is too large", failure);
        }
        return transaction;
    }

    @Bean("outboxBusinessStateMachineService")
    @ConditionalOnMissingBean(name = "outboxBusinessStateMachineService")
    public BusinessStateMachineService outboxBusinessStateMachineService(
            @Qualifier("outboxBusinessStateMachineDefinitions")
            Map<String, BusinessStateMachineDefinitionStrategy> definitions,
            @Qualifier("outboxBusinessStateMachineContextExecutor")
            BusinessStateMachineContextExecutor contextExecutor,
            @Qualifier("outboxBusinessStateMachineTransaction")
            TransactionTemplate transactionTemplate,
            @Qualifier("outboxStateMachineExecutionService")
            StateMachineExecutionService executionService,
            @Qualifier("outboxStateMachineProperties")
            OutboxStateMachineProperties properties,
            @Qualifier("outboxStateMachineObjectMapper")
            ObjectMapper objectMapper,
            @Qualifier("egonColaValidationUtils")
            ValidationUtils validationUtils,
            @Qualifier("outboxStateMachineClock")
            Clock clock
    ) {
        return new BusinessStateMachineService(
                definitions, contextExecutor, transactionTemplate, executionService,
                properties, objectMapper, validationUtils, clock);
    }

    @Bean("outboxBusinessStateMachineDeliveryHandler")
    @ConditionalOnMissingBean(name = "outboxBusinessStateMachineDeliveryHandler")
    public BusinessStateMachineDeliveryHandler outboxBusinessStateMachineDeliveryHandler(
            @Qualifier("outboxBusinessStateMachineService") BusinessStateMachineService service,
            @Qualifier("outboxBusinessStateMachineDefinitions")
            Map<String, BusinessStateMachineDefinitionStrategy> definitions,
            @Qualifier("outboxStateMachineObjectMapper") ObjectMapper objectMapper,
            @Qualifier("egonColaValidationUtils") ValidationUtils validationUtils
    ) {
        return new BusinessStateMachineDeliveryHandler(service, definitions, objectMapper, validationUtils);
    }

    private static void validateFactory(String destination, StateMachineFactory<String, String> factory) {
        StateMachine<String, String> first = factory.getStateMachine();
        StateMachine<String, String> second = factory.getStateMachine();
        if (first == null || second == null || first == second
                || first.getStateMachineAccessor().withAllRegions() == null
                || first.getStateMachineAccessor().withAllRegions().size() != 1
                || first.getStates() == null || first.getStates().isEmpty()
                || first.getTransitions() == null || first.getTransitions().isEmpty()) {
            throw new OutboxConfigurationException(
                    "Business state-machine factory must create fresh, stopped, single-region machines: " + destination);
        }
        for (State<String, String> state : first.getStates()) {
            if (state == null || state.getId() == null || state.getId().isBlank()
                    || !state.isSimple() || state.isComposite() || state.isOrthogonal() || state.isSubmachineState()
                    || state.getDeferredEvents() != null && !state.getDeferredEvents().isEmpty()) {
                throw new OutboxConfigurationException(
                        "Business state-machine graph must be flat with no deferred events: " + destination);
            }
        }
        for (Transition<String, String> transition : first.getTransitions()) {
            if (transition.getKind() == TransitionKind.INITIAL) {
                continue;
            }
            if (transition.getTrigger() == null || transition.getTrigger().getEvent() == null) {
                throw new OutboxConfigurationException(
                        "Business state-machine transitions must use explicit event signals: " + destination);
            }
        }
    }
}
