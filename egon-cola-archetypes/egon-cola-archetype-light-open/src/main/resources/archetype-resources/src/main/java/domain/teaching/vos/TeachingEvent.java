package ${package}.domain.teaching.vos;

import java.time.Instant;

public record TeachingEvent(String type, String aggregateId, Instant occurredAt) {
    public static TeachingEvent classCreated(String schoolClassId) {
        return new TeachingEvent("class.created", schoolClassId, Instant.now());
    }

    public static TeachingEvent courseCreated(String courseId) {
        return new TeachingEvent("course.created", courseId, Instant.now());
    }

    public static TeachingEvent courseScheduled(String schoolClassId) {
        return new TeachingEvent("schedule.created", schoolClassId, Instant.now());
    }
}
