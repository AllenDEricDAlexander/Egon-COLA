package ${package}.domain.teaching.vos;

import java.time.Instant;

public record TeachingEvent(String type, String aggregateId, Instant occurredAt) {
    public static TeachingEvent classCreated(long schoolClassId) {
        return new TeachingEvent("class.created", Long.toString(schoolClassId), Instant.now());
    }

    public static TeachingEvent courseCreated(long courseId) {
        return new TeachingEvent("course.created", Long.toString(courseId), Instant.now());
    }

    public static TeachingEvent courseScheduled(long schoolClassId) {
        return new TeachingEvent("schedule.created", Long.toString(schoolClassId), Instant.now());
    }
}
