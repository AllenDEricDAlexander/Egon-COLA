package ${package}.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RabbitMqConfigTest {
    @Test
    void declares_exchange_import_queues_dead_letters_and_json_conversion() {
        RabbitMqConfig config = new RabbitMqConfig("sample.domain", "sample");
        Queue userQueue = config.userImportedQueue();
        Queue courseQueue = config.courseImportedQueue();
        Binding userBinding = config.userImportedBinding(userQueue, config.domainExchange());

        assertEquals("sample.user.imported", userQueue.getName());
        assertEquals("sample.course.imported", courseQueue.getName());
        assertEquals("sample.user.imported.dlq", userQueue.getArguments().get("x-dead-letter-routing-key"));
        assertEquals("user.imported", userBinding.getRoutingKey());
        assertNotNull(config.jacksonMessageConverter(new com.fasterxml.jackson.databind.ObjectMapper()));
    }
}
