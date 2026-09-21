package top.egon.cola.archetype.source.webopen.infrastructure.mq;

import top.egon.cola.archetype.source.webopen.domain.events.OrganizationDomainEvent;
import top.egon.cola.archetype.source.webopen.domain.teaching.events.GradeChangedEvent;
import top.egon.cola.archetype.source.webopen.domain.teaching.events.SchoolClassChangedEvent;
import top.egon.cola.archetype.source.webopen.domain.teaching.events.SchoolClassMembershipChangedEvent;
import top.egon.cola.archetype.source.webopen.domain.user.events.PermissionGrantedEvent;
import top.egon.cola.archetype.source.webopen.domain.user.events.RoleAssignedEvent;
import top.egon.cola.archetype.source.webopen.domain.user.events.UserChangedEvent;
import top.egon.cola.component.common.core.enums.EgonEnum;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Closed routing table for this family: one constant per produced or consumed message, so the
 * exchange, queue, dead-letter target, accepted wire schema and payload projection are declared
 * once instead of being re-derived from the event type string at every call site.
 *
 * <p>The names below stay exactly on the wire, because a generated project inherits this broker
 * topology; only the ownership of the constants moved here.</p>
 */
public enum MqRouteEnum implements EgonEnum {

    USER_CHANGED(0, "user changed", MqRouteEnum.EVENT_EXCHANGE, null, MqRouteEnum.USER_CHANGED_ROUTING_KEY, null, true,
            OrganizationEventMessage.class, MqRouteEnum.SCHEMA_VERSION, "USER_CHANGED", UserChangedEvent.class,
            event -> Map.of("changeType", ((UserChangedEvent) event).changeType())),

    ROLE_ASSIGNED(1, "role assigned", MqRouteEnum.EVENT_EXCHANGE, null, MqRouteEnum.ROLE_ASSIGNED_ROUTING_KEY, null, true,
            OrganizationEventMessage.class, MqRouteEnum.SCHEMA_VERSION, "ROLE_ASSIGNED", RoleAssignedEvent.class,
            event -> Map.of("roleCode", ((RoleAssignedEvent) event).roleCode())),

    PERMISSION_GRANTED(2, "permission granted", MqRouteEnum.EVENT_EXCHANGE, null, MqRouteEnum.PERMISSION_GRANTED_ROUTING_KEY,
            null, true, OrganizationEventMessage.class, MqRouteEnum.SCHEMA_VERSION, "PERMISSION_GRANTED",
            PermissionGrantedEvent.class, event -> Map.of(
                    "roleCode", ((PermissionGrantedEvent) event).roleCode(),
                    "permissionCode", ((PermissionGrantedEvent) event).permissionCode())),

    GRADE_CHANGED(3, "grade changed", MqRouteEnum.EVENT_EXCHANGE, null, MqRouteEnum.GRADE_CHANGED_ROUTING_KEY, null, true,
            OrganizationEventMessage.class, MqRouteEnum.SCHEMA_VERSION, "GRADE_CHANGED", GradeChangedEvent.class,
            event -> Map.of("changeType", ((GradeChangedEvent) event).changeType())),

    SCHOOL_CLASS_CHANGED(4, "school class changed", MqRouteEnum.EVENT_EXCHANGE, null, MqRouteEnum.SCHOOL_CLASS_CHANGED_ROUTING_KEY,
            null, true, OrganizationEventMessage.class, MqRouteEnum.SCHEMA_VERSION, "SCHOOL_CLASS_CHANGED",
            SchoolClassChangedEvent.class, event -> {
                SchoolClassChangedEvent value = (SchoolClassChangedEvent) event;
                return Map.of("gradeId", Long.toString(value.gradeId()), "changeType", value.changeType());
            }),

    SCHOOL_CLASS_MEMBERSHIP_CHANGED(5, "school class membership changed", MqRouteEnum.EVENT_EXCHANGE, null,
            MqRouteEnum.MEMBERSHIP_CHANGED_ROUTING_KEY, null, true, OrganizationEventMessage.class, MqRouteEnum.SCHEMA_VERSION,
            "SCHOOL_CLASS_MEMBERSHIP_CHANGED", SchoolClassMembershipChangedEvent.class, event -> {
                SchoolClassMembershipChangedEvent value = (SchoolClassMembershipChangedEvent) event;
                return Map.of("userId", Long.toString(value.userId()), "changeType", value.changeType());
            }),

    /** Consumed-only routes: the accepted carrier belongs to the adapter boundary, not here. */
    USER_CREATE_COMMAND(6, "create user command", MqRouteEnum.COMMAND_EXCHANGE, MqRouteEnum.CREATE_USER_QUEUE,
            MqRouteEnum.CREATE_USER_ROUTING_KEY, MqRouteEnum.CREATE_USER_DEAD_LETTER_ROUTING_KEY, true, null, MqRouteEnum.SCHEMA_VERSION,
            null, null, null),

    SCHOOL_CLASS_CREATE_COMMAND(7, "create school class command", MqRouteEnum.COMMAND_EXCHANGE,
            MqRouteEnum.CREATE_SCHOOL_CLASS_QUEUE, MqRouteEnum.CREATE_SCHOOL_CLASS_ROUTING_KEY,
            MqRouteEnum.CREATE_SCHOOL_CLASS_DEAD_LETTER_ROUTING_KEY, true, null, MqRouteEnum.SCHEMA_VERSION, null, null, null);

    public static final String SCHEMA_VERSION = "v1";
    public static final String COMMAND_EXCHANGE = "student.organization.command.v1";
    public static final String EVENT_EXCHANGE = "student.organization.event.v1";
    public static final String DEAD_LETTER_EXCHANGE = "student.organization.dlx.v1";
    public static final String CREATE_USER_QUEUE = "student.organization.user.create.v1";
    public static final String CREATE_SCHOOL_CLASS_QUEUE = "student.organization.school-class.create.v1";
    public static final String CREATE_USER_DEAD_LETTER_QUEUE = CREATE_USER_QUEUE + ".dlq";
    public static final String CREATE_SCHOOL_CLASS_DEAD_LETTER_QUEUE = CREATE_SCHOOL_CLASS_QUEUE + ".dlq";
    public static final String CREATE_USER_ROUTING_KEY = "organization.command.user.create.v1";
    public static final String CREATE_SCHOOL_CLASS_ROUTING_KEY =
            "organization.command.teaching.school-class.create.v1";
    public static final String CREATE_USER_DEAD_LETTER_ROUTING_KEY = "organization.dead.user.create.v1";
    public static final String CREATE_SCHOOL_CLASS_DEAD_LETTER_ROUTING_KEY =
            "organization.dead.teaching.school-class.create.v1";
    public static final String USER_CHANGED_ROUTING_KEY = "organization.event.user.changed.v1";
    public static final String ROLE_ASSIGNED_ROUTING_KEY = "organization.event.user.role-assigned.v1";
    public static final String PERMISSION_GRANTED_ROUTING_KEY =
            "organization.event.user.permission-granted.v1";
    public static final String GRADE_CHANGED_ROUTING_KEY = "organization.event.teaching.grade.changed.v1";
    public static final String SCHOOL_CLASS_CHANGED_ROUTING_KEY =
            "organization.event.teaching.school-class.changed.v1";
    public static final String MEMBERSHIP_CHANGED_ROUTING_KEY =
            "organization.event.teaching.membership.changed.v1";

    /** Event type to declared route; an unknown event never reaches a broker call. */
    private static final Map<Class<? extends OrganizationDomainEvent>, MqRouteEnum> EVENT_ROUTES =
            Arrays.stream(values())
                    .filter(route -> route.eventType != null)
                    .collect(Collectors.toUnmodifiableMap(MqRouteEnum::getEventClass,
                            Function.identity()));

    private final int code;
    private final String message;
    private final String exchange;
    private final String queue;
    private final String routingKey;
    private final String deadLetterRoutingKey;
    private final boolean durable;
    private final Class<?> payloadType;
    private final String schemaVersion;
    private final String eventType;
    private final Class<? extends OrganizationDomainEvent> eventClass;
    private final Function<OrganizationDomainEvent, Map<String, String>> payloadExtractor;

    MqRouteEnum(int code, String message, String exchange, String queue, String routingKey,
                String deadLetterRoutingKey, boolean durable, Class<?> payloadType, String schemaVersion,
                String eventType, Class<? extends OrganizationDomainEvent> eventClass,
                Function<OrganizationDomainEvent, Map<String, String>> payloadExtractor) {
        this.code = code;
        this.message = message;
        this.exchange = exchange;
        this.queue = queue;
        this.routingKey = routingKey;
        this.deadLetterRoutingKey = deadLetterRoutingKey;
        this.durable = durable;
        this.payloadType = payloadType;
        this.schemaVersion = schemaVersion;
        this.eventType = eventType;
        this.eventClass = eventClass;
        this.payloadExtractor = payloadExtractor;
    }

    /** Exact table lookup: the event class is matched, never guessed from its name. */
    public static MqRouteEnum requireEventRoute(OrganizationDomainEvent event) {
        MqRouteEnum route = EVENT_ROUTES.get(Objects.requireNonNull(event, "event").getClass());
        if (route == null) {
            throw new IllegalArgumentException("unsupported organization event " + event.getClass().getName());
        }
        return route;
    }

    /** Exact payload check before any broker call; a consumed-only route never publishes. */
    public boolean accepts(Object payload) {
        return payloadType != null && payloadType.equals(payload.getClass());
    }

    public Map<String, String> payloadOf(OrganizationDomainEvent event) {
        if (payloadExtractor == null) {
            throw new IllegalStateException("MQ_ROUTE_NOT_PRODUCIBLE: " + name());
        }
        return payloadExtractor.apply(event);
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public String getExchange() {
        return exchange;
    }

    public String getQueue() {
        return queue;
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

    public String getEventType() {
        return eventType;
    }

    public Class<? extends OrganizationDomainEvent> getEventClass() {
        return eventClass;
    }
}
