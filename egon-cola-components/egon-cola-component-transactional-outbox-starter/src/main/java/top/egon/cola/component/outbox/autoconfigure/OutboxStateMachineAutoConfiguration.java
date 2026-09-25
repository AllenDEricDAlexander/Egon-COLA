package top.egon.cola.component.outbox.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.AliasRegistry;
import org.springframework.statemachine.StateMachine;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration;
import top.egon.cola.component.outbox.common.exception.OutboxConfigurationException;
import top.egon.cola.component.outbox.statemachine.OutboxLifecycleService;
import top.egon.cola.component.outbox.statemachine.OutboxLifecycleStateMachineFactory;
import top.egon.cola.component.outbox.statemachine.StateMachineExecutionService;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@AutoConfiguration(after = {
        EgonColaMybatisPlusAutoConfiguration.class,
        TransactionalOutboxAutoConfiguration.class
}, afterName = "org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration")
@EnableConfigurationProperties(OutboxStateMachineProperties.class)
@ConditionalOnClass(StateMachine.class)
@ConditionalOnBean(TransactionalOutboxProperties.class)
public class OutboxStateMachineAutoConfiguration {

    @Bean(name = "outboxStateMachineExecutionService")
    @ConditionalOnMissingBean(name = "outboxStateMachineExecutionService")
    public StateMachineExecutionService outboxStateMachineExecutionService(
            @Qualifier("egonColaValidationUtils")
            ValidationUtils validationUtils
    ) {
        return new StateMachineExecutionService(validationUtils);
    }

    @Bean(name = "outboxLifecycleStateMachineFactory")
    @ConditionalOnMissingBean(name = "outboxLifecycleStateMachineFactory")
    public OutboxLifecycleStateMachineFactory outboxLifecycleStateMachineFactory() {
        return new OutboxLifecycleStateMachineFactory();
    }

    @Bean(name = "outboxLifecycleService")
    @ConditionalOnMissingBean(name = "outboxLifecycleService")
    public OutboxLifecycleService outboxLifecycleService(
            @Qualifier("outboxLifecycleStateMachineFactory")
            OutboxLifecycleStateMachineFactory factory,
            @Qualifier("outboxStateMachineExecutionService")
            StateMachineExecutionService executionService,
            @Qualifier("outboxStateMachineProperties")
            OutboxStateMachineProperties properties,
            TransactionalOutboxProperties outboxProperties,
            @Qualifier("egonColaValidationUtils")
            ValidationUtils validationUtils
    ) {
        validateBudgets(properties, outboxProperties, validationUtils);
        return new OutboxLifecycleService(factory, executionService, properties, validationUtils);
    }

    @Bean
    public static BeanFactoryPostProcessor outboxStateMachineAliases() {
        return beanFactory -> {
            if (!(beanFactory instanceof AliasRegistry aliases)) {
                throw new OutboxConfigurationException("Bean factory does not support state-machine aliases");
            }
            registerAliasIfAvailable(
                    beanFactory,
                    aliases,
                    OutboxStateMachineProperties.PREFIX + "-" + OutboxStateMachineProperties.class.getName(),
                    "outboxStateMachineProperties"
            );
            String[] outboxPropertiesNames = beanFactory.getBeanNamesForType(
                    TransactionalOutboxProperties.class, true, false);
            if (outboxPropertiesNames.length != 1) {
                throw new OutboxConfigurationException(
                        "Transactional outbox properties must have exactly one bean for state-machine alias"
                );
            }
            registerAliasIfAvailable(
                    beanFactory,
                    aliases,
                    outboxPropertiesNames[0],
                    "outboxStateMachineOutboxProperties"
            );
            registerAliasIfAvailable(beanFactory, aliases, "egonColaMybatisPlusClock", "outboxStateMachineClock");
            String objectMapperName = selectObjectMapper(beanFactory);
            if (objectMapperName != null) {
                registerAliasIfAvailable(
                        beanFactory,
                        aliases,
                        objectMapperName,
                        "outboxStateMachineObjectMapper"
                );
            }
        };
    }

    private static void validateBudgets(
            OutboxStateMachineProperties properties,
            TransactionalOutboxProperties outboxProperties,
            ValidationUtils validationUtils
    ) {
        validationUtils.validate(properties);
        Duration claimTimeout = properties.getClaimEvaluationTimeout();
        Duration leaseDuration = outboxProperties.getDelivery().getLeaseDuration();
        Duration businessTimeout = properties.getBusiness().getTimeout();
        Duration deliveryTimeout = outboxProperties.getDelivery().getTimeout();
        if (leaseDuration == null || claimTimeout.compareTo(leaseDuration) > 0) {
            throw new OutboxConfigurationException(
                    "State-machine claim-evaluation-timeout must not exceed delivery.lease-duration"
            );
        }
        if (deliveryTimeout == null || businessTimeout.compareTo(deliveryTimeout) >= 0) {
            throw new OutboxConfigurationException(
                    "State-machine business.timeout must be less than delivery.timeout"
            );
        }
    }

    private static String selectObjectMapper(ConfigurableListableBeanFactory beanFactory) {
        String[] candidates = beanFactory.getBeanNamesForType(ObjectMapper.class, true, false);
        if (candidates.length == 0) {
            return null;
        }
        if (candidates.length == 1) {
            return candidates[0];
        }
        List<String> primaryCandidates = Arrays.stream(candidates)
                .filter(beanName -> beanFactory instanceof BeanDefinitionRegistry registry
                        && registry.containsBeanDefinition(beanName)
                        && beanFactory.getBeanDefinition(beanName).isPrimary())
                .toList();
        if (primaryCandidates.size() != 1) {
            throw new OutboxConfigurationException(
                    "ObjectMapper selection is ambiguous for outbox state-machine alias"
            );
        }
        return primaryCandidates.getFirst();
    }

    private static void registerAliasIfAvailable(
            ConfigurableListableBeanFactory beanFactory,
            AliasRegistry aliases,
            String sourceName,
            String aliasName
    ) {
        if (aliases.isAlias(aliasName)) {
            boolean pointsToSource = Arrays.asList(aliases.getAliases(sourceName)).contains(aliasName);
            if (!pointsToSource) {
                throw new OutboxConfigurationException("Conflicting outbox state-machine alias: " + aliasName);
            }
            return;
        }
        if (beanFactory.containsBeanDefinition(aliasName) || beanFactory.containsSingleton(aliasName)) {
            throw new OutboxConfigurationException("Conflicting outbox state-machine bean name: " + aliasName);
        }
        if (beanFactory.containsBean(sourceName)) {
            aliases.registerAlias(sourceName, aliasName);
        }
    }
}
