package top.egon.cola.component.rpc.ddc.client;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.config.*;
import top.egon.cola.component.ddc.model.lease.*;
import top.egon.cola.component.rpc.ddc.client.config.RpcDdcConfigClient;
import top.egon.cola.component.rpc.ddc.contract.DdcConfigRuntimeRpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.AcknowledgePublishResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.HeartbeatConfigClientResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.OfflineConfigClientResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RegisterConfigClientResponse;
import top.egon.cola.component.rpc.ddc.mapping.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RpcDdcConfigClientTest {

    @Test
    void mapsAllConfigRuntimePortMethods() {
        DdcConfigRuntimeRpc rpc = mock(DdcConfigRuntimeRpc.class);
        DdcCommonProtoMapper common = new DdcCommonProtoMapper(1024 * 1024);
        DdcConfigProtoMapper mapper = new DdcConfigProtoMapper(common, 1024 * 1024);
        RpcDdcConfigClient client = new RpcDdcConfigClient(
                rpc, mapper, common, new DdcRpcStatusExceptionMapper(),
                "biz", "test", "app");
        Instant now = Instant.parse("2026-08-09T00:00:00Z");
        DdcLeaseSession session = new DdcLeaseSession(
                "instance-1", "lease-1", DdcLeaseRole.CONFIG_CLIENT,
                30, 10, now, now.plusSeconds(30));
        DdcLeaseOperationResult renewed = new DdcLeaseOperationResult(
                DdcLeaseOperationStatus.RENEWED, now.plusSeconds(30));
        when(rpc.registerConfigClient(any())).thenReturn(
                RegisterConfigClientResponse.newBuilder()
                        .setSession(common.toProto(session)).build());
        when(rpc.heartbeatConfigClient(any())).thenReturn(
                HeartbeatConfigClientResponse.newBuilder()
                        .setResult(common.toProto(renewed)).build());
        when(rpc.offlineConfigClient(any())).thenReturn(
                OfflineConfigClientResponse.newBuilder()
                        .setResult(common.toProto(new DdcLeaseOperationResult(
                                DdcLeaseOperationStatus.DELETED, null))).build());
        DdcConfigValue config = new DdcConfigValue();
        config.setResourceName("application.yml");
        config.setContent("feature:\n  enabled: true\n");
        config.setFormat("YAML");
        config.setVersion(2L);
        when(rpc.pullConfig(any())).thenReturn(mapper.toPullResponse(List.of(config)));
        when(rpc.acknowledgePublish(any())).thenReturn(
                AcknowledgePublishResponse.getDefaultInstance());

        assertThat(client.register(registration())).isEqualTo(session);
        assertThat(client.heartbeat(heartbeat()).status())
                .isEqualTo(DdcLeaseOperationStatus.RENEWED);
        assertThat(client.offline(heartbeat()).status())
                .isEqualTo(DdcLeaseOperationStatus.DELETED);
        assertThat(client.pull()).singleElement()
                .extracting(DdcConfigValue::getVersion).isEqualTo(2L);
        client.ack(ack());

        verify(rpc).registerConfigClient(argThat(request ->
                request.getScope().getBizCode().equals("biz")
                        && request.getLeaseSeconds() == 30
                        && request.getRegistrationToken().equals("config-register-ticket")));
        verify(rpc).heartbeatConfigClient(argThat(request ->
                request.getRegistrationToken().equals("config-heartbeat-ticket")));
        verify(rpc).pullConfig(argThat(request ->
                request.getScope().getEnv().equals("test")));
        verify(rpc).acknowledgePublish(argThat(request ->
                request.getLeaseId().equals("lease-1")));
    }

    private DdcInstanceRegisterRequest registration() {
        DdcInstanceRegisterRequest request = new DdcInstanceRegisterRequest();
        request.setInstanceId("instance-1"); request.setBizCode("biz");
        request.setEnv("test"); request.setAppCode("app");
        request.setHost("127.0.0.1"); request.setPort(8080);
        request.setPid("1"); request.setSdkVersion("5.3.3");
        request.setLeaseSeconds(30); request.setHeartbeatIntervalSeconds(10);
        request.setMetadata(Map.of("zone", "a"));
        request.setRegistrationToken("config-register-ticket");
        return request;
    }

    private DdcHeartbeatRequest heartbeat() {
        DdcHeartbeatRequest request = new DdcHeartbeatRequest();
        request.setInstanceId("instance-1"); request.setLeaseId("lease-1");
        request.setBizCode("biz"); request.setEnv("test"); request.setAppCode("app");
        request.setHost("127.0.0.1"); request.setPort(8080);
        request.setPid("1"); request.setSdkVersion("5.3.3");
        request.setMetadata(Map.of());
        request.setRegistrationToken("config-heartbeat-ticket");
        return request;
    }

    private DdcAckRequest ack() {
        DdcAckRequest request = new DdcAckRequest();
        request.setChangeId("change-1"); request.setBizCode("biz");
        request.setEnv("test"); request.setAppCode("app");
        request.setInstanceId("instance-1"); request.setLeaseId("lease-1");
        request.setResourceName("application.yml"); request.setTargetVersion(2L);
        request.setStatus(DdcAckStatus.SUCCESS); request.setAckTime(1L);
        return request;
    }
}
