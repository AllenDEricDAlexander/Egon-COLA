package top.egon.cola.archetype.source.web.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.archetype.source.web.infrastructure.mq.MqRouteEnum;

/**
 * Declares the topology straight from the closed route table, so exchange, queue and dead-letter
 * naming has one owner instead of a per-bean copy. The declared names stay exactly on the wire.
 */
@Configuration("organizationRabbitConfig")
@ConditionalOnProperty(prefix = "organization.integrations.rabbit", name = "enabled", havingValue = "true")
public class OrganizationRabbitConfig {

    @Bean public TopicExchange commandExchange() {
        return exchange(MqRouteEnum.COMMAND_EXCHANGE);
    }

    @Bean public TopicExchange eventExchange() {
        return exchange(MqRouteEnum.EVENT_EXCHANGE);
    }

    @Bean public TopicExchange deadLetterExchange() {
        return exchange(MqRouteEnum.DEAD_LETTER_EXCHANGE);
    }

    @Bean public Queue createUserQueue() {
        return commandQueue(MqRouteEnum.USER_CREATE_COMMAND);
    }

    @Bean public Queue createSchoolClassQueue() {
        return commandQueue(MqRouteEnum.SCHOOL_CLASS_CREATE_COMMAND);
    }

    @Bean public Queue createUserDeadLetterQueue() {
        return QueueBuilder.durable(MqRouteEnum.CREATE_USER_DEAD_LETTER_QUEUE).build();
    }

    @Bean public Queue createSchoolClassDeadLetterQueue() {
        return QueueBuilder.durable(MqRouteEnum.CREATE_SCHOOL_CLASS_DEAD_LETTER_QUEUE).build();
    }

    @Bean public Binding createUserBinding() {
        return BindingBuilder.bind(createUserQueue()).to(commandExchange())
                .with(MqRouteEnum.USER_CREATE_COMMAND.getRoutingKey());
    }

    @Bean public Binding createSchoolClassBinding() {
        return BindingBuilder.bind(createSchoolClassQueue()).to(commandExchange())
                .with(MqRouteEnum.SCHOOL_CLASS_CREATE_COMMAND.getRoutingKey());
    }

    @Bean public Binding createUserDeadLetterBinding() {
        return BindingBuilder.bind(createUserDeadLetterQueue()).to(deadLetterExchange())
                .with(MqRouteEnum.USER_CREATE_COMMAND.getDeadLetterRoutingKey());
    }

    @Bean public Binding createSchoolClassDeadLetterBinding() {
        return BindingBuilder.bind(createSchoolClassDeadLetterQueue()).to(deadLetterExchange())
                .with(MqRouteEnum.SCHOOL_CLASS_CREATE_COMMAND.getDeadLetterRoutingKey());
    }

    private static TopicExchange exchange(String name) {
        return new TopicExchange(name, MqRouteEnum.USER_CREATE_COMMAND.isDurable(), false);
    }

    private static Queue commandQueue(MqRouteEnum route) {
        return QueueBuilder.durable(route.getQueue())
                .deadLetterExchange(MqRouteEnum.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(route.getDeadLetterRoutingKey())
                .build();
    }
}
