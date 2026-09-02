package top.egon.cola.archetype.source.serviceopen.infrastructure.course.mq.message;
import java.time.Instant;
public record CourseScheduledMessage(
        Long scheduleId, Long courseId, Long classId, Instant startsAt, Instant endsAt) { }
