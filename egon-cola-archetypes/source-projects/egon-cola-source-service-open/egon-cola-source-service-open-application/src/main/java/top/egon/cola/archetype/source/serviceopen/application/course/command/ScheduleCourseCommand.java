package top.egon.cola.archetype.source.serviceopen.application.course.command;

import java.time.Instant;

public record ScheduleCourseCommand(
        Long courseId, Long classId, Instant startsAt, Instant endsAt) {
}
