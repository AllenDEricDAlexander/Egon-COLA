package top.egon.cola.archetype.source.service.infrastructure.course.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.archetype.source.service.domain.course.entities.CourseSchedule;
import top.egon.cola.archetype.source.service.domain.course.service.CourseEventService;
import top.egon.cola.archetype.source.service.infrastructure.course.mq.message.CourseScheduledMessage;
import top.egon.cola.archetype.source.service.infrastructure.mq.MqMessageService;
import top.egon.cola.archetype.source.service.infrastructure.mq.MqRouteEnum;

/** Resolves the declared course route and hands the message to the single MQ boundary. */
@Validated
@Service("courseEventService")
@RequiredArgsConstructor
@Slf4j
public class CourseEventServiceImpl implements CourseEventService {
    @Qualifier("mqMessageService")
    private final MqMessageService mqMessageService;

    @Override
    public void courseScheduled(CourseSchedule schedule) {
        mqMessageService.publish(MqRouteEnum.COURSE_SCHEDULED, new CourseScheduledMessage(
                schedule.getId(), schedule.getCourseId().value(), schedule.getClassId(),
                schedule.getStartsAt(), schedule.getEndsAt()));
    }
}
