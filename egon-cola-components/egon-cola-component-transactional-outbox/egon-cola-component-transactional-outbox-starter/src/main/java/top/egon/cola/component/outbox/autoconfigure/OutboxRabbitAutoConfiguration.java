package top.egon.cola.component.outbox.autoconfigure;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.outbox.delivery.rabbitmq.PropertiesRabbitDestinationResolver;
import top.egon.cola.component.outbox.delivery.rabbitmq.RabbitDeliveryHandler;
import top.egon.cola.component.outbox.delivery.rabbitmq.RabbitDestinationResolver;
import top.egon.cola.component.outbox.delivery.rabbitmq.RabbitMessagePublisher;
import top.egon.cola.component.outbox.delivery.rabbitmq.RabbitTemplateMessagePublisher;
import top.egon.cola.component.outbox.exception.OutboxConfigurationException;

import java.time.Clock;

@AutoConfiguration(after = TransactionalOutboxAutoConfiguration.class)
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnBean({TransactionalOutbox.class, RabbitTemplate.class})
@ConditionalOnProperty(
        prefix = "egon.cola.component.transactional-outbox.rabbitmq",
        name = "enabled",
        havingValue = "true"
)
public class OutboxRabbitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RabbitDestinationResolver rabbitDestinationResolver(
            TransactionalOutboxProperties properties
    ) {
        return new PropertiesRabbitDestinationResolver(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    RabbitMessagePublisher rabbitMessagePublisher(RabbitTemplate rabbitTemplate) {
        validateRabbitTemplate(rabbitTemplate);
        return new RabbitTemplateMessagePublisher(rabbitTemplate, Clock.systemUTC());
    }

    @Bean
    @ConditionalOnMissingBean
    RabbitDeliveryHandler rabbitDeliveryHandler(
            RabbitDestinationResolver destinationResolver,
            RabbitMessagePublisher publisher
    ) {
        return new RabbitDeliveryHandler(destinationResolver, publisher);
    }

    private void validateRabbitTemplate(RabbitTemplate rabbitTemplate) {
        ConnectionFactory connectionFactory = rabbitTemplate.getConnectionFactory();
        Message probe = new Message(new byte[0], new MessageProperties());
        if (connectionFactory == null
                || !connectionFactory.isPublisherConfirms()
                || !connectionFactory.isPublisherReturns()
                || !rabbitTemplate.isMandatoryFor(probe)) {
            throw new OutboxConfigurationException(
                    "RabbitMQ outbox delivery requires correlated confirms, returns, and mandatory publishing"
            );
        }
    }
}
