package ${package}.infrastructure.mq;

import ${package}.domain.client.OrganizationEventPublisher;
import ${package}.domain.events.OrganizationDomainEvent;
import ${package}.domain.teaching.events.GradeChangedEvent;
import ${package}.domain.teaching.events.SchoolClassChangedEvent;
import ${package}.domain.teaching.events.SchoolClassMembershipChangedEvent;
import ${package}.domain.user.events.PermissionGrantedEvent;
import ${package}.domain.user.events.RoleAssignedEvent;
import ${package}.domain.user.events.UserChangedEvent;
import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("rabbitOrganizationEventPublisher")
@ConditionalOnProperty(prefix = "organization.integrations.rabbit", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RabbitOrganizationEventPublisher implements OrganizationEventPublisher {
    public static final String EVENT_EXCHANGE = "student.organization.event.v1";
    private final OrganizationEventProducer producer;

    @Override public void publish(OrganizationDomainEvent event) {
        MappedEvent mapped = map(event);
        try {
            producer.send(EVENT_EXCHANGE, mapped.routingKey(), new OrganizationEventMessage(
                event.eventId(), mapped.eventType(), event.aggregateId(), event.occurredAt(), mapped.payload()));
        } catch (RuntimeException failure) {
            Metrics.counter("organization.event.publish.failures", "eventType", mapped.eventType()).increment();
            log.error("organization event publication failed eventId={} eventType={}",
                event.eventId(), mapped.eventType(), failure);
        }
    }

    private static MappedEvent map(OrganizationDomainEvent event) {
        if (event instanceof UserChangedEvent value) return new MappedEvent(
            "USER_CHANGED", "organization.event.user.changed.v1", Map.of("changeType", value.changeType()));
        if (event instanceof RoleAssignedEvent value) return new MappedEvent(
            "ROLE_ASSIGNED", "organization.event.user.role-assigned.v1", Map.of("roleCode", value.roleCode()));
        if (event instanceof PermissionGrantedEvent value) return new MappedEvent(
            "PERMISSION_GRANTED", "organization.event.user.permission-granted.v1",
            Map.of("roleCode", value.roleCode(), "permissionCode", value.permissionCode()));
        if (event instanceof GradeChangedEvent value) return new MappedEvent(
            "GRADE_CHANGED", "organization.event.teaching.grade.changed.v1", Map.of("changeType", value.changeType()));
        if (event instanceof SchoolClassChangedEvent value) return new MappedEvent(
            "SCHOOL_CLASS_CHANGED", "organization.event.teaching.school-class.changed.v1",
            Map.of("gradeId", value.gradeId(), "changeType", value.changeType()));
        if (event instanceof SchoolClassMembershipChangedEvent value) return new MappedEvent(
            "SCHOOL_CLASS_MEMBERSHIP_CHANGED", "organization.event.teaching.membership.changed.v1",
            Map.of("userId", value.userId(), "changeType", value.changeType()));
        throw new IllegalArgumentException("unsupported organization event " + event.getClass().getName());
    }

    private record MappedEvent(String eventType, String routingKey, Map<String, String> payload) {}
}
