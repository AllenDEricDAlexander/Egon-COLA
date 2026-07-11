package ${package}.infrastructure;

import ${package}.domain.events.user.RoleAssignedEvent;
import ${package}.infrastructure.config.OrganizationRabbitConfig;
import ${package}.infrastructure.mq.OrganizationEventMessage;
import ${package}.infrastructure.mq.OrganizationEventProducer;
import ${package}.infrastructure.mq.RabbitOrganizationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrganizationRabbitMqContractTest {
    @Mock OrganizationEventProducer producer;

    @Test
    void declaresDistinctTopologyAndMapsEvents() {
        OrganizationRabbitConfig config = new OrganizationRabbitConfig();
        assertEquals("student.organization.command.v1", config.commandExchange().getName());
        assertEquals("student.organization.event.v1", config.eventExchange().getName());
        assertEquals("student.organization.dlx.v1", config.deadLetterExchange().getName());
        assertEquals("student.organization.user.create.v1", config.createUserQueue().getName());
        assertEquals("student.organization.school-class.create.v1", config.createSchoolClassQueue().getName());

        Instant occurredAt = Instant.parse("2026-07-11T00:00:00Z");
        new RabbitOrganizationEventPublisher(producer).publish(
            new RoleAssignedEvent("e-1", "u-1", occurredAt, "STUDENT"));
        verify(producer).send("student.organization.event.v1",
            "organization.event.user.role-assigned.v1",
            new OrganizationEventMessage("e-1", "ROLE_ASSIGNED", "u-1", occurredAt,
                Map.of("roleCode", "STUDENT")));
    }
}
