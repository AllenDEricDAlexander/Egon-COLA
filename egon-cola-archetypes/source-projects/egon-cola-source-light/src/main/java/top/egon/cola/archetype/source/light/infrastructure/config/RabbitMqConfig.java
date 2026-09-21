package top.egon.cola.archetype.source.light.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.archetype.source.light.infrastructure.mq.MqRouteEnum;

@EnableRabbit
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.integrations.rabbitmq.enabled", havingValue = "true")
@RequiredArgsConstructor
public class RabbitMqConfig {
    @Value("${app.integrations.rabbitmq.exchange}")
    private final String exchangeName;
    @Value("${spring.application.name}")
    private final String applicationName;

    @Bean
    TopicExchange domainExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    Queue userImportedQueue() {
        return consumerQueue(MqRouteEnum.USER_IMPORTED);
    }

    @Bean
    Queue courseImportedQueue() {
        return consumerQueue(MqRouteEnum.COURSE_IMPORTED);
    }

    @Bean
    Queue userImportedDeadLetterQueue() {
        return deadLetterQueue(MqRouteEnum.USER_IMPORTED);
    }

    @Bean
    Queue courseImportedDeadLetterQueue() {
        return deadLetterQueue(MqRouteEnum.COURSE_IMPORTED);
    }

    @Bean
    Binding userImportedBinding(Queue userImportedQueue, TopicExchange domainExchange) {
        return consumerBinding(MqRouteEnum.USER_IMPORTED, userImportedQueue, domainExchange);
    }

    @Bean
    Binding courseImportedBinding(Queue courseImportedQueue, TopicExchange domainExchange) {
        return consumerBinding(MqRouteEnum.COURSE_IMPORTED, courseImportedQueue, domainExchange);
    }

    @Bean
    MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    private Queue consumerQueue(MqRouteEnum route) {
        return QueueBuilder.durable(route.consumerQueue(applicationName))
                .deadLetterExchange("")
                .deadLetterRoutingKey(applicationName + "." + route.getDeadLetterRoutingKey())
                .build();
    }

    private Queue deadLetterQueue(MqRouteEnum route) {
        return QueueBuilder.durable(applicationName + "." + route.getDeadLetterRoutingKey()).build();
    }

    private Binding consumerBinding(MqRouteEnum route, Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(route.getRoutingKey());
    }
}
