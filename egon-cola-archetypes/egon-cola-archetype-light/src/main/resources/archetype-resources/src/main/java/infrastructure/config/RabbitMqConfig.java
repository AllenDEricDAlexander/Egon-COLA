package ${package}.infrastructure.config;

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

@EnableRabbit
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.integrations.rabbitmq.enabled", havingValue = "true")
@RequiredArgsConstructor
public class RabbitMqConfig {
    @Value("${symbol_dollar}{app.integrations.rabbitmq.exchange}")
    private final String exchangeName;
    @Value("${symbol_dollar}{spring.application.name}")
    private final String applicationName;

    @Bean
    TopicExchange domainExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    Queue userImportedQueue() {
        String name = applicationName + ".user.imported";
        return QueueBuilder.durable(name)
                .deadLetterExchange("")
                .deadLetterRoutingKey(name + ".dlq")
                .build();
    }

    @Bean
    Queue courseImportedQueue() {
        String name = applicationName + ".course.imported";
        return QueueBuilder.durable(name)
                .deadLetterExchange("")
                .deadLetterRoutingKey(name + ".dlq")
                .build();
    }

    @Bean
    Queue userImportedDeadLetterQueue() {
        return QueueBuilder.durable(applicationName + ".user.imported.dlq").build();
    }

    @Bean
    Queue courseImportedDeadLetterQueue() {
        return QueueBuilder.durable(applicationName + ".course.imported.dlq").build();
    }

    @Bean
    Binding userImportedBinding(Queue userImportedQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(userImportedQueue).to(domainExchange).with("user.imported");
    }

    @Bean
    Binding courseImportedBinding(Queue courseImportedQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(courseImportedQueue).to(domainExchange).with("course.imported");
    }

    @Bean
    MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
