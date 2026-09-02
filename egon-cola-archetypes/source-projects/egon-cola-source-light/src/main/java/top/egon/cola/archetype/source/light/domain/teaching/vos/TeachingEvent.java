package top.egon.cola.archetype.source.light.domain.teaching.vos;

import java.time.Instant;

public record TeachingEvent(String type, Long aggregateId, Instant occurredAt) {
    public static TeachingEvent classCreated(Long schoolClassId) {
        return new TeachingEvent("class.created", schoolClassId, Instant.now());
    }

    public static TeachingEvent courseCreated(Long courseId) {
        return new TeachingEvent("course.created", courseId, Instant.now());
    }

    public static TeachingEvent courseScheduled(Long schoolClassId) {
        return new TeachingEvent("schedule.created", schoolClassId, Instant.now());
    }

}
