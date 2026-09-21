package top.egon.cola.archetype.source.lightopen.infrastructure.user.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserEvent;
import top.egon.cola.archetype.source.lightopen.infrastructure.mq.MqRouteEnum;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.service.impl.UserEventServiceImpl;
import top.egon.cola.archetype.source.lightopen.support.RecordingMqMessageService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Routing comes from the declared table, so the fixture asserts the exact route per event type. */
class UserEventServiceImplTest {
    private final RecordingMqMessageService messages = new RecordingMqMessageService();
    private final UserEventServiceImpl service = new UserEventServiceImpl(messages);

    @Test
    void resolves_each_declared_user_route() {
        UserEvent created = UserEvent.created(1001L);
        UserEvent roleAssigned = UserEvent.roleAssigned(1001L);
        UserEvent permissionGranted = UserEvent.permissionGranted(2001L);

        service.publish(created);
        service.publish(roleAssigned);
        service.publish(permissionGranted);

        assertEquals(List.of(
                        new RecordingMqMessageService.Publication(MqRouteEnum.USER_CHANGED, created),
                        new RecordingMqMessageService.Publication(MqRouteEnum.USER_ROLE_CHANGED, roleAssigned),
                        new RecordingMqMessageService.Publication(
                                MqRouteEnum.AUTHORIZATION_CHANGED, permissionGranted)),
                messages.publications());
    }

    @Test
    void rejects_an_unregistered_event_type_before_touching_the_broker() {
        UserEvent unknown = new UserEvent("user.unknown", 1L, java.time.Instant.now());

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.publish(unknown));

        assertEquals("MQ_ROUTE_UNSUPPORTED: user.unknown", error.getMessage());
        assertEquals(0, messages.publications().size());
    }
}
