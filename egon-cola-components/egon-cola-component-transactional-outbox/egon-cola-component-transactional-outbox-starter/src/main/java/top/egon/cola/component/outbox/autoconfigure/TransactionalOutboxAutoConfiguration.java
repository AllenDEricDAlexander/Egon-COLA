package top.egon.cola.component.outbox.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import top.egon.cola.component.outbox.aop.OutboxMessageExpressionResolver;
import top.egon.cola.component.outbox.aop.TransactionalMessageAop;
import top.egon.cola.component.outbox.aop.TransactionalMessageMethodValidator;
import top.egon.cola.component.outbox.api.OutboxIdGenerator;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.outbox.api.UuidOutboxIdGenerator;
import top.egon.cola.component.outbox.cleanup.OutboxCleanupJob;
import top.egon.cola.component.outbox.deadletter.OutboxDeadLetterListener;
import top.egon.cola.component.outbox.deadletter.OutboxDeadLetterNotifier;
import top.egon.cola.component.outbox.delivery.DefaultDeliveryFailureClassifier;
import top.egon.cola.component.outbox.delivery.DeliveryFailureClassifier;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryHandlerRegistry;
import top.egon.cola.component.outbox.dispatch.OutboxDispatcher;
import top.egon.cola.component.outbox.dispatch.OutboxPoller;
import top.egon.cola.component.outbox.dispatch.OutboxWorkerIdentity;
import top.egon.cola.component.outbox.event.OutboxCommittedEventListener;
import top.egon.cola.component.outbox.exception.OutboxConfigurationException;
import top.egon.cola.component.outbox.observability.NoopOutboxMetrics;
import top.egon.cola.component.outbox.observability.OutboxMetrics;
import top.egon.cola.component.outbox.retry.ExponentialJitterRetryPolicy;
import top.egon.cola.component.outbox.retry.OutboxRetryPolicy;
import top.egon.cola.component.outbox.serialization.JacksonOutboxMessageSerializer;
import top.egon.cola.component.outbox.serialization.OutboxMessageSerializer;
import top.egon.cola.component.outbox.store.OutboxSchemaValidator;
import top.egon.cola.component.outbox.store.OutboxStore;
import top.egon.cola.component.outbox.store.PostgresqlJdbcOutboxStore;
import top.egon.cola.component.outbox.transaction.DefaultTransactionalOutbox;
import top.egon.cola.component.outbox.transaction.OutboxAfterCommitBuffer;
import top.egon.cola.component.outbox.transaction.OutboxTransactionGuard;
import top.egon.cola.component.outbox.validation.OutboxMessageValidator;

import javax.sql.DataSource;
import java.time.Clock;
import java.util.List;

@AutoConfiguration(afterName = {
        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration",
        "org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration"
})
@EnableConfigurationProperties(TransactionalOutboxProperties.class)
@ConditionalOnClass({DataSource.class, JdbcTemplate.class})
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(
        prefix = "egon.cola.component.transactional-outbox",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class TransactionalOutboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    OutboxConfigurationValidator outboxConfigurationValidator() {
        return new OutboxConfigurationValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxInfrastructureResolver outboxInfrastructureResolver() {
        return new OutboxInfrastructureResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxInfrastructure outboxInfrastructure(
            ConfigurableListableBeanFactory beanFactory,
            TransactionalOutboxProperties properties,
            OutboxConfigurationValidator validator,
            OutboxInfrastructureResolver resolver
    ) {
        validator.validate(properties);
        return resolver.resolve(beanFactory, properties);
    }

    @Bean(name = "outboxJdbcTemplate")
    @ConditionalOnMissingBean(name = "outboxJdbcTemplate")
    JdbcTemplate outboxJdbcTemplate(OutboxInfrastructure infrastructure) {
        return new JdbcTemplate(infrastructure.dataSource());
    }

    @Bean(name = "outboxNamedParameterJdbcTemplate")
    @ConditionalOnMissingBean(name = "outboxNamedParameterJdbcTemplate")
    NamedParameterJdbcTemplate outboxNamedParameterJdbcTemplate(
            OutboxInfrastructure infrastructure
    ) {
        return new NamedParameterJdbcTemplate(infrastructure.dataSource());
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxMessageValidator outboxMessageValidator(
            ObjectProvider<ObjectMapper> objectMappers,
            TransactionalOutboxProperties properties
    ) {
        return new OutboxMessageValidator(
                requireObjectMapper(objectMappers),
                Math.toIntExact(properties.getPayload().getMaxBytes().toBytes()),
                properties.getPayload().getMaxHeaderCount(),
                Math.toIntExact(properties.getPayload().getMaxHeaderBytes().toBytes())
        );
    }

    @Bean
    @ConditionalOnMissingBean(OutboxMessageSerializer.class)
    OutboxMessageSerializer outboxMessageSerializer(
            ObjectProvider<ObjectMapper> objectMappers
    ) {
        return new JacksonOutboxMessageSerializer(requireObjectMapper(objectMappers));
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxIdGenerator outboxIdGenerator() {
        return new UuidOutboxIdGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxStore outboxStore(
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("outboxNamedParameterJdbcTemplate")
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            ObjectProvider<ObjectMapper> objectMappers,
            OutboxInfrastructure infrastructure
    ) {
        return new PostgresqlJdbcOutboxStore(
                jdbcTemplate,
                namedParameterJdbcTemplate,
                requireObjectMapper(objectMappers),
                infrastructure.transactionManager()
        );
    }

    @Bean(initMethod = "validate")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.transactional-outbox.storage",
            name = "validate-schema",
            havingValue = "true",
            matchIfMissing = true
    )
    OutboxSchemaValidator outboxSchemaValidator(OutboxStore store) {
        return new OutboxSchemaValidator(store);
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxTransactionGuard outboxTransactionGuard(OutboxInfrastructure infrastructure) {
        return new OutboxTransactionGuard(infrastructure.dataSource());
    }

    @Bean
    @ConditionalOnMissingBean
    DeliveryHandlerRegistry deliveryHandlerRegistry(
            ObjectProvider<DeliveryHandler> handlers
    ) {
        return new DeliveryHandlerRegistry(handlers.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxRetryPolicy outboxRetryPolicy(TransactionalOutboxProperties properties) {
        return new ExponentialJitterRetryPolicy(
                properties.getRetry().getInitialDelay(),
                properties.getRetry().getMultiplier(),
                properties.getRetry().getMaxDelay(),
                properties.getRetry().getJitter(),
                Math::random
        );
    }

    @Bean
    @ConditionalOnMissingBean
    DeliveryFailureClassifier deliveryFailureClassifier() {
        return new DefaultDeliveryFailureClassifier();
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxMetrics outboxMetrics() {
        return new NoopOutboxMetrics();
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxDeadLetterNotifier outboxDeadLetterNotifier(
            ObjectProvider<OutboxDeadLetterListener> listeners
    ) {
        List<OutboxDeadLetterListener> configured = listeners.orderedStream().toList();
        return new OutboxDeadLetterNotifier(configured);
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxAfterCommitBuffer outboxAfterCommitBuffer(
            ApplicationEventPublisher publisher,
            OutboxMetrics metrics
    ) {
        return new OutboxAfterCommitBuffer(publisher, metrics);
    }

    @Bean
    @ConditionalOnMissingBean(TransactionalOutbox.class)
    TransactionalOutbox transactionalOutbox(
            OutboxMessageValidator validator,
            OutboxMessageSerializer serializer,
            OutboxIdGenerator idGenerator,
            ObjectProvider<ObjectMapper> objectMappers,
            OutboxTransactionGuard transactionGuard,
            OutboxStore store,
            DeliveryHandlerRegistry handlerRegistry,
            OutboxAfterCommitBuffer afterCommitBuffer,
            OutboxMetrics metrics,
            TransactionalOutboxProperties properties
    ) {
        return new DefaultTransactionalOutbox(
                validator,
                serializer,
                idGenerator,
                requireObjectMapper(objectMappers),
                transactionGuard,
                store,
                handlerRegistry,
                afterCommitBuffer,
                metrics,
                properties
        );
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxWorkerIdentity outboxWorkerIdentity(TransactionalOutboxProperties properties) {
        return new OutboxWorkerIdentity(properties.getNodeId());
    }

    @Bean(name = "outboxDeliveryExecutor")
    @ConditionalOnMissingBean(name = "outboxDeliveryExecutor")
    ThreadPoolTaskExecutor outboxDeliveryExecutor(TransactionalOutboxProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getPolling().getConcurrency());
        executor.setMaxPoolSize(properties.getPolling().getConcurrency());
        executor.setQueueCapacity(properties.getDelivery().getQueueCapacity());
        executor.setThreadNamePrefix("egon-cola-outbox-delivery-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(
                Math.toIntExact(properties.getShutdown().getGracePeriod().toSeconds())
        );
        return executor;
    }

    @Bean(name = "outboxTaskScheduler")
    @ConditionalOnMissingBean(name = "outboxTaskScheduler")
    ThreadPoolTaskScheduler outboxTaskScheduler(TransactionalOutboxProperties properties) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setThreadNamePrefix("egon-cola-outbox-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(
                Math.toIntExact(properties.getShutdown().getGracePeriod().toSeconds())
        );
        return scheduler;
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxDispatcher outboxDispatcher(
            OutboxStore store,
            DeliveryHandlerRegistry handlerRegistry,
            DeliveryFailureClassifier failureClassifier,
            OutboxRetryPolicy retryPolicy,
            OutboxDeadLetterNotifier deadLetterNotifier,
            OutboxMetrics metrics,
            OutboxWorkerIdentity workerIdentity,
            @Qualifier("outboxDeliveryExecutor") TaskExecutor taskExecutor,
            TransactionalOutboxProperties properties
    ) {
        return new OutboxDispatcher(
                store,
                handlerRegistry,
                failureClassifier,
                retryPolicy,
                deadLetterNotifier,
                metrics,
                workerIdentity,
                taskExecutor,
                properties,
                Clock.systemUTC()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxCleanupJob outboxCleanupJob(
            OutboxStore store,
            TransactionalOutboxProperties properties
    ) {
        return new OutboxCleanupJob(store, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxCommittedEventListener outboxCommittedEventListener(
            OutboxDispatcher dispatcher
    ) {
        return new OutboxCommittedEventListener(dispatcher);
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxPoller outboxPoller(
            OutboxDispatcher dispatcher,
            OutboxCleanupJob cleanupJob,
            @Qualifier("outboxTaskScheduler") TaskScheduler taskScheduler,
            TransactionalOutboxProperties properties,
            ObjectProvider<OutboxSchemaValidator> schemaValidator
    ) {
        schemaValidator.getIfAvailable();
        return new OutboxPoller(dispatcher, cleanupJob, taskScheduler, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.transactional-outbox.annotation",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    OutboxMessageExpressionResolver outboxMessageExpressionResolver() {
        return new OutboxMessageExpressionResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.transactional-outbox.annotation",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    TransactionalMessageMethodValidator transactionalMessageMethodValidator(
            OutboxInfrastructure infrastructure
    ) {
        return new TransactionalMessageMethodValidator(
                infrastructure.transactionManagerBeanName()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.transactional-outbox.annotation",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    TransactionalMessageAop transactionalMessageAop(
            TransactionalOutbox transactionalOutbox,
            OutboxInfrastructure infrastructure,
            OutboxMessageExpressionResolver expressionResolver,
            TransactionalMessageMethodValidator methodValidator,
            TransactionalOutboxProperties properties
    ) {
        return new TransactionalMessageAop(
                transactionalOutbox,
                infrastructure.transactionManager(),
                infrastructure.transactionManagerBeanName(),
                expressionResolver,
                methodValidator,
                properties
        );
    }

    private ObjectMapper requireObjectMapper(ObjectProvider<ObjectMapper> objectMappers) {
        ObjectMapper objectMapper = objectMappers.getIfUnique();
        if (objectMapper == null) {
            throw new OutboxConfigurationException(
                    "Transactional outbox requires exactly one ObjectMapper bean"
            );
        }
        return objectMapper;
    }
}
