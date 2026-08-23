#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.config;

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

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.integrations.rabbitmq", name = "enabled", havingValue = "true")
public class RabbitMqConfiguration {
    @Bean TopicExchange evaluationExchange(
            @Value("${symbol_dollar}{app.integrations.rabbitmq.exchange}") String name) {
        return new TopicExchange(name, true, false);
    }
    @Bean Queue recordScoreCommandQueue(
            @Value("${symbol_dollar}{app.integrations.rabbitmq.score-command-queue}") String name) {
        return QueueBuilder.durable(name).build();
    }
    @Bean Binding recordScoreCommandBinding(
            Queue recordScoreCommandQueue,
            TopicExchange evaluationExchange,
            @Value("${symbol_dollar}{app.integrations.rabbitmq.score-command-routing-key}") String routingKey) {
        return BindingBuilder.bind(recordScoreCommandQueue).to(evaluationExchange).with(routingKey);
    }
    @Bean MessageConverter rabbitMessageConverter() { return new Jackson2JsonMessageConverter(); }
}
