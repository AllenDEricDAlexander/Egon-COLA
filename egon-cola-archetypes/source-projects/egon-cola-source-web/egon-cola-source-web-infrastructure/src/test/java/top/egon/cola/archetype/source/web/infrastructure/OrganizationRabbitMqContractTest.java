package top.egon.cola.archetype.source.web.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.egon.cola.archetype.source.web.domain.user.events.RoleAssignedEvent;
import top.egon.cola.archetype.source.web.infrastructure.config.OrganizationRabbitConfig;
import top.egon.cola.archetype.source.web.infrastructure.mq.MqRouteEnum;
import top.egon.cola.archetype.source.web.infrastructure.mq.OrganizationEventMessage;
import top.egon.cola.archetype.source.web.infrastructure.mq.OrganizationEventProducer;
import top.egon.cola.archetype.source.web.infrastructure.mq.impl.RabbitMqMessageServiceImpl;
import top.egon.cola.archetype.source.web.infrastructure.service.impl.OrganizationEventServiceImpl;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

/**
 * The declared topology and the produced wire payload are the two contracts a generated project
 * must not drift on, so both are pinned here through the single MQ boundary.
 */
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
        new OrganizationEventServiceImpl(new RabbitMqMessageServiceImpl(producer)).publish(
            new RoleAssignedEvent("e-1", 2001L, occurredAt, "STUDENT"));
        verify(producer).send(MqRouteEnum.EVENT_EXCHANGE,
            MqRouteEnum.ROLE_ASSIGNED_ROUTING_KEY,
            new OrganizationEventMessage("e-1", "ROLE_ASSIGNED", 2001L, occurredAt,
                Map.of("roleCode", "STUDENT")));
    }
}
