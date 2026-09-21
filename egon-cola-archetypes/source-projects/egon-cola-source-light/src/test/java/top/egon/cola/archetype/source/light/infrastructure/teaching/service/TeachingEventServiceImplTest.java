package top.egon.cola.archetype.source.light.infrastructure.teaching.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import top.egon.cola.archetype.source.light.domain.teaching.vos.TeachingEvent;
import top.egon.cola.archetype.source.light.infrastructure.mq.MqRouteEnum;
import top.egon.cola.archetype.source.light.infrastructure.teaching.service.impl.TeachingEventServiceImpl;
import top.egon.cola.archetype.source.light.support.RecordingMqMessageService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Routing comes from the declared table, so the fixture asserts the exact route per event type. */
class TeachingEventServiceImplTest {
    private final RecordingMqMessageService messages = new RecordingMqMessageService();
    private final TeachingEventServiceImpl service = new TeachingEventServiceImpl(messages);

    @Test
    void resolves_each_declared_teaching_route() {
        TeachingEvent classEvent = TeachingEvent.classCreated(1003L);
        TeachingEvent courseEvent = TeachingEvent.courseCreated(1002L);
        TeachingEvent scheduleEvent = TeachingEvent.courseScheduled(1003L);

        service.publish(classEvent);
        service.publish(courseEvent);
        service.publish(scheduleEvent);

        assertEquals(List.of(
                        new RecordingMqMessageService.Publication(MqRouteEnum.CLASS_CHANGED, classEvent),
                        new RecordingMqMessageService.Publication(MqRouteEnum.COURSE_CHANGED, courseEvent),
                        new RecordingMqMessageService.Publication(MqRouteEnum.SCHEDULE_CHANGED, scheduleEvent)),
                messages.publications());
    }

    @Test
    void rejects_an_unregistered_event_type_before_touching_the_broker() {
        TeachingEvent unknown = new TeachingEvent("unknown.created", 1L, Instant.now());

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.publish(unknown));

        assertEquals("MQ_ROUTE_UNSUPPORTED: unknown.created", error.getMessage());
        assertEquals(0, messages.publications().size());
    }
}
