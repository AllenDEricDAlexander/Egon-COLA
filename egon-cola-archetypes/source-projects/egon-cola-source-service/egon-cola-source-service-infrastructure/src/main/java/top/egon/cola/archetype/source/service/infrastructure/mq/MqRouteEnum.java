package top.egon.cola.archetype.source.service.infrastructure.mq;

import top.egon.cola.archetype.source.service.infrastructure.course.mq.message.CourseScheduledMessage;
import top.egon.cola.archetype.source.service.infrastructure.exam.mq.message.ExamPublishedMessage;
import top.egon.cola.archetype.source.service.infrastructure.exam.mq.message.ScoreRecordedMessage;
import top.egon.cola.component.common.core.enums.EgonEnum;

/**
 * Closed routing table for this family: one constant per produced or consumed message, so the
 * exchange, queue, dead-letter target and accepted payload are declared once instead of being
 * re-derived from the event type string at every call site.
 *
 * <p>The wire targets stay externalised as configuration properties, so a generated project keeps
 * its broker naming without editing this table.</p>
 */
public enum MqRouteEnum implements EgonEnum {

    COURSE_SCHEDULED(0, "course scheduled", MqRouteEnum.EXCHANGE_PROPERTY, null,
            MqRouteEnum.COURSE_SCHEDULED_ROUTING_KEY_PROPERTY, null, true, CourseScheduledMessage.class, MqRouteEnum.SCHEMA_VERSION),

    EXAM_PUBLISHED(1, "exam published", MqRouteEnum.EXCHANGE_PROPERTY, null,
            MqRouteEnum.EXAM_PUBLISHED_ROUTING_KEY_PROPERTY, null, true, ExamPublishedMessage.class, MqRouteEnum.SCHEMA_VERSION),

    SCORE_RECORDED(2, "score recorded", MqRouteEnum.EXCHANGE_PROPERTY, null,
            MqRouteEnum.SCORE_RECORDED_ROUTING_KEY_PROPERTY, null, true, ScoreRecordedMessage.class, MqRouteEnum.SCHEMA_VERSION),

    /** Consumed-only route: the accepted carrier belongs to the adapter boundary, not here. */
    SCORE_COMMAND(3, "score record command", MqRouteEnum.EXCHANGE_PROPERTY, MqRouteEnum.SCORE_COMMAND_QUEUE_PROPERTY,
            MqRouteEnum.SCORE_COMMAND_ROUTING_KEY_PROPERTY, null, true, null, MqRouteEnum.SCHEMA_VERSION);

    public static final String SCHEMA_VERSION = "v1";
    public static final String EXCHANGE_PROPERTY = "app.integrations.rabbitmq.exchange";
    public static final String SCORE_COMMAND_QUEUE_PROPERTY = "app.integrations.rabbitmq.score-command-queue";
    public static final String SCORE_COMMAND_ROUTING_KEY_PROPERTY =
            "app.integrations.rabbitmq.score-command-routing-key";
    public static final String COURSE_SCHEDULED_ROUTING_KEY_PROPERTY =
            "app.integrations.rabbitmq.course-scheduled-routing-key";
    public static final String EXAM_PUBLISHED_ROUTING_KEY_PROPERTY =
            "app.integrations.rabbitmq.exam-published-routing-key";
    public static final String SCORE_RECORDED_ROUTING_KEY_PROPERTY =
            "app.integrations.rabbitmq.score-recorded-routing-key";

    private final int code;
    private final String message;
    private final String exchangeProperty;
    private final String queueProperty;
    private final String routingKeyProperty;
    private final String deadLetterRoutingKey;
    private final boolean durable;
    private final Class<?> payloadType;
    private final String schemaVersion;

    MqRouteEnum(int code, String message, String exchangeProperty, String queueProperty,
                String routingKeyProperty, String deadLetterRoutingKey, boolean durable,
                Class<?> payloadType, String schemaVersion) {
        this.code = code;
        this.message = message;
        this.exchangeProperty = exchangeProperty;
        this.queueProperty = queueProperty;
        this.routingKeyProperty = routingKeyProperty;
        this.deadLetterRoutingKey = deadLetterRoutingKey;
        this.durable = durable;
        this.payloadType = payloadType;
        this.schemaVersion = schemaVersion;
    }

    /** Exact payload check before any broker call; a consumed-only route never publishes. */
    public boolean accepts(Object payload) {
        return payloadType != null && payloadType.equals(payload.getClass());
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public String getExchangeProperty() {
        return exchangeProperty;
    }

    public String getQueueProperty() {
        return queueProperty;
    }

    public String getRoutingKeyProperty() {
        return routingKeyProperty;
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
