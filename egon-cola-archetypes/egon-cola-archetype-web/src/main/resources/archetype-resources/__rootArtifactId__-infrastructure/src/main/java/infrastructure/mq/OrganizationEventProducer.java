package ${package}.infrastructure.mq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("organizationEventProducer")
@ConditionalOnProperty(prefix = "organization.integrations.rabbit", name = "enabled", havingValue = "true")
public class OrganizationEventProducer {
    private final RabbitTemplate rabbitTemplate;
    public OrganizationEventProducer(RabbitTemplate rabbitTemplate) { this.rabbitTemplate = rabbitTemplate; }

    public void send(String exchange, String routingKey, OrganizationEventMessage message) {
        long delayMillis = 1_000;
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, message);
                return;
            } catch (RuntimeException failure) {
                lastFailure = failure;
                if (attempt < 3) sleep(delayMillis);
                delayMillis = Math.min(delayMillis * 2, 5_000);
            }
        }
        throw lastFailure;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("event retry interrupted", interrupted);
        }
    }
}
