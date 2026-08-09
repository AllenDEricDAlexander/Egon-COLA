package top.egon.cola.component.ddc.admin.service.config;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.admin.model.vo.DdcPublishResultVO;
import top.egon.cola.component.ddc.admin.service.lease.DdcInstanceAdminService;
import top.egon.cola.component.ddc.admin.service.publish.DdcPublishService;
import top.egon.cola.component.ddc.model.config.DdcAckRequest;
import top.egon.cola.component.ddc.model.config.DdcConfigValue;
import top.egon.cola.component.ddc.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcConfigFacadeTest {

    @Test
    void delegatesAllConfigRuntimeOperationsWithoutChangingResults() {
        DdcInstanceAdminService instances = mock(DdcInstanceAdminService.class);
        DdcConfigService configs = mock(DdcConfigService.class);
        DdcPublishService publishes = mock(DdcPublishService.class);
        DdcConfigFacade facade = new DdcConfigFacade(instances, configs, publishes);
        DdcInstanceRegisterRequest registration = new DdcInstanceRegisterRequest();
        DdcHeartbeatRequest heartbeat = new DdcHeartbeatRequest();
        DdcAckRequest acknowledgement = new DdcAckRequest();
        DdcLeaseSession session = mock(DdcLeaseSession.class);
        DdcLeaseOperationResult renewed = mock(DdcLeaseOperationResult.class);
        DdcLeaseOperationResult deleted = mock(DdcLeaseOperationResult.class);
        DdcConfigValue config = new DdcConfigValue();
        DdcPublishResultVO ackResult = mock(DdcPublishResultVO.class);
        when(instances.register(registration)).thenReturn(session);
        when(instances.heartbeat(heartbeat)).thenReturn(renewed);
        when(instances.offline(heartbeat)).thenReturn(deleted);
        when(configs.pull("biz", "test", "app")).thenReturn(List.of(config));
        when(publishes.ack(acknowledgement)).thenReturn(ackResult);

        assertThat(facade.register(registration)).isSameAs(session);
        assertThat(facade.heartbeat(heartbeat)).isSameAs(renewed);
        assertThat(facade.offline(heartbeat)).isSameAs(deleted);
        assertThat(facade.pull("biz", "test", "app")).containsExactly(config);
        assertThat(facade.ack(acknowledgement)).isSameAs(ackResult);

        verify(instances).register(registration);
        verify(instances).heartbeat(heartbeat);
        verify(instances).offline(heartbeat);
        verify(configs).pull("biz", "test", "app");
        verify(publishes).ack(acknowledgement);
    }
}
