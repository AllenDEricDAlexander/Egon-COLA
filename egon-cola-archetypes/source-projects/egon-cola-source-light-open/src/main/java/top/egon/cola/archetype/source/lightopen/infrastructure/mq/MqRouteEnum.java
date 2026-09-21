package top.egon.cola.archetype.source.lightopen.infrastructure.mq;

import top.egon.cola.archetype.source.lightopen.facade.teaching.dto.CreateCourseDTO;
import top.egon.cola.archetype.source.lightopen.facade.user.dto.CreateUserDTO;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.TeachingEvent;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserEvent;
import top.egon.cola.component.common.core.enums.EgonEnum;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Closed routing table for this family: one constant per produced or consumed message, so the
 * exchange, queue, dead-letter target and accepted payload are declared once instead of being
 * re-derived from the event type string at every call site.
 */
public enum MqRouteEnum implements EgonEnum {

    CLASS_CHANGED(0, "class changed", "class.created", "app.integrations.rabbitmq.exchange", null,
            "class.changed", null, true, TeachingEvent.class, "v1"),

    COURSE_CHANGED(1, "course changed", "course.created", "app.integrations.rabbitmq.exchange", null,
            "course.changed", null, true, TeachingEvent.class, "v1"),

    SCHEDULE_CHANGED(2, "schedule changed", "schedule.created", "app.integrations.rabbitmq.exchange", null,
            "schedule.changed", null, true, TeachingEvent.class, "v1"),

    USER_CHANGED(3, "user changed", "user.created", "app.integrations.rabbitmq.exchange", null,
            "user.changed", null, true, UserEvent.class, "v1"),

    USER_ROLE_CHANGED(4, "user role changed", "user.role-assigned", "app.integrations.rabbitmq.exchange", null,
            "user.changed", null, true, UserEvent.class, "v1"),

    AUTHORIZATION_CHANGED(5, "authorization changed", "authorization.permission-granted",
            "app.integrations.rabbitmq.exchange", null, "authorization.changed", null, true, UserEvent.class, "v1"),

    USER_IMPORTED(6, "user import command", null, "app.integrations.rabbitmq.exchange", "user.imported",
            "user.imported", "user.imported.dlq", true, CreateUserDTO.class, "v1"),

    COURSE_IMPORTED(7, "course import command", null, "app.integrations.rabbitmq.exchange", "course.imported",
            "course.imported", "course.imported.dlq", true, CreateCourseDTO.class, "v1");

    public static final String EXCHANGE_PROPERTY = "app.integrations.rabbitmq.exchange";
    public static final String SCHEMA_VERSION = "v1";

    private static final Map<String, MqRouteEnum> PRODUCER_BY_EVENT_TYPE = Stream.of(values())
            .filter(route -> route.eventType != null)
            .collect(Collectors.toUnmodifiableMap(MqRouteEnum::getEventType, route -> route));

    private final int code;
    private final String message;
    private final String eventType;
    private final String exchangeProperty;
    private final String queueSuffix;
    private final String routingKey;
    private final String deadLetterRoutingKey;
    private final boolean durable;
    private final Class<?> payloadType;
    private final String schemaVersion;

    MqRouteEnum(int code, String message, String eventType, String exchangeProperty, String queueSuffix,
                String routingKey, String deadLetterRoutingKey, boolean durable,
                Class<?> payloadType, String schemaVersion) {
        this.code = code;
        this.message = message;
        this.eventType = eventType;
        this.exchangeProperty = exchangeProperty;
        this.queueSuffix = queueSuffix;
        this.routingKey = routingKey;
        this.deadLetterRoutingKey = deadLetterRoutingKey;
        this.durable = durable;
        this.payloadType = payloadType;
        this.schemaVersion = schemaVersion;
    }

    /** Exact declared-event lookup; an unregistered type is rejected before any broker call. */
    public static MqRouteEnum resolveProducer(String eventType) {
        MqRouteEnum route = eventType == null ? null : PRODUCER_BY_EVENT_TYPE.get(eventType);
        if (route == null) {
            throw new IllegalStateException("MQ_ROUTE_UNSUPPORTED: " + eventType);
        }
        return route;
    }

    public boolean accepts(Object payload) {
        return payloadType.isInstance(payload);
    }

    public String consumerQueue(String applicationName) {
        return applicationName + "." + queueSuffix;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public String getEventType() {
        return eventType;
    }

    public String getExchangeProperty() {
        return exchangeProperty;
    }

    public String getQueueSuffix() {
        return queueSuffix;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getDeadLetterRoutingKey() {
        return deadLetterRoutingKey;
    }

    public boolean isDurable() {
        return durable;
    }

    public Class<?> getPayloadType() {
        return payloadType;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }
}
