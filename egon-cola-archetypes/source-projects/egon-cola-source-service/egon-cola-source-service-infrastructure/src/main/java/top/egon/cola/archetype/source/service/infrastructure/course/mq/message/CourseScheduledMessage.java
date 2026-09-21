package top.egon.cola.archetype.source.service.infrastructure.course.mq.message;

import java.time.Instant;
import top.egon.cola.component.common.core.pojo.BasePojo;

/** Course-schedule event published on the {@code COURSE_SCHEDULED} route. */
public record CourseScheduledMessage(
        Long scheduleId, Long courseId, Long classId, Instant startsAt, Instant endsAt)
        implements BasePojo { }
