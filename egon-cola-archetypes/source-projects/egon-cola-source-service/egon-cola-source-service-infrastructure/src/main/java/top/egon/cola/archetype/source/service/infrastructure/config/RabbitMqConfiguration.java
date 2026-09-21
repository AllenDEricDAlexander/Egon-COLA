package top.egon.cola.archetype.source.service.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.archetype.source.service.infrastructure.mq.MqRouteEnum;

/** Declares the topology straight from the closed route table, so routing has one owner. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.integrations.rabbitmq", name = "enabled", havingValue = "true")
public class RabbitMqConfiguration {

    @Bean TopicExchange evaluationExchange(
            @Value("${" + MqRouteEnum.EXCHANGE_PROPERTY + "}") String name) {
        return new TopicExchange(name, MqRouteEnum.SCORE_COMMAND.isDurable(), false);
    }

    @Bean Queue recordScoreCommandQueue(
            @Value("${" + MqRouteEnum.SCORE_COMMAND_QUEUE_PROPERTY + "}") String name) {
        return QueueBuilder.durable(name).build();
    }

    @Bean Binding recordScoreCommandBinding(
            Queue recordScoreCommandQueue,
            TopicExchange evaluationExchange,
            @Value("${" + MqRouteEnum.SCORE_COMMAND_ROUTING_KEY_PROPERTY + "}") String routingKey) {
        return BindingBuilder.bind(recordScoreCommandQueue).to(evaluationExchange).with(routingKey);
    }

    @Bean MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
