package top.egon.cola.component.ddc.admin.rpc.provider;

import org.junit.jupiter.api.Test;
import io.grpc.Context;
import top.egon.cola.component.ddc.admin.security.rpc.DdcServicePrincipal;
import top.egon.cola.component.ddc.admin.service.config.DdcConfigFacade;
import top.egon.cola.component.ddc.model.config.DdcAckRequest;
import top.egon.cola.component.ddc.model.config.DdcAckStatus;
import top.egon.cola.component.ddc.model.config.DdcConfigValue;
import top.egon.cola.component.ddc.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.rpc.ddc.mapping.DdcCommonProtoMapper;
import top.egon.cola.component.rpc.ddc.mapping.DdcConfigProtoMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcConfigRpcProviderTest {

    @Test
    void mapsAndDelegatesAllFiveRuntimeMethods() {
        DdcConfigFacade facade = mock(DdcConfigFacade.class);
        DdcCommonProtoMapper common = new DdcCommonProtoMapper(4 * 1024 * 1024);
        DdcConfigProtoMapper mapper = new DdcConfigProtoMapper(common, 1024 * 1024);
        DdcConfigRpcProvider provider = new DdcConfigRpcProvider(
                facade, common, mapper);
        Instant now = Instant.parse("2026-08-09T00:00:00Z");
        DdcInstanceRegisterRequest registration = registration();
        DdcHeartbeatRequest heartbeat = heartbeat();
        DdcLeaseSession session = new DdcLeaseSession(
                "instance-1", "lease-1", DdcLeaseRole.CONFIG_CLIENT,
                30, 10, now, now.plusSeconds(30));
        DdcLeaseOperationResult renewed = new DdcLeaseOperationResult(
                DdcLeaseOperationStatus.RENEWED, now.plusSeconds(30));
        DdcLeaseOperationResult deleted = new DdcLeaseOperationResult(
                DdcLeaseOperationStatus.DELETED, null);
        DdcConfigValue config = config();
        DdcAckRequest ack = acknowledgement();
        when(facade.register(any(DdcInstanceRegisterRequest.class))).thenReturn(session);
        when(facade.heartbeat(any(DdcHeartbeatRequest.class))).thenReturn(renewed);
        when(facade.offline(any(DdcHeartbeatRequest.class))).thenReturn(deleted);
        when(facade.pull("biz", "test", "app")).thenReturn(List.of(config));

        principal("SDK").bind(Context.current()).run(() -> {
            assertThat(provider.registerConfigClient(mapper.toRegisterRequest(registration))
                    .getSession()).isEqualTo(common.toProto(session));
            assertThat(provider.heartbeatConfigClient(mapper.toHeartbeatRequest(heartbeat))
                    .getResult()).isEqualTo(common.toProto(renewed));
            assertThat(provider.offlineConfigClient(mapper.toOfflineRequest(heartbeat))
                    .getResult()).isEqualTo(common.toProto(deleted));
            assertThat(provider.pullConfig(mapper.toPullRequest("biz", "test", "app"))
                    .getConfigsList()).containsExactly(mapper.toConfig(config));
            assertThat(provider.acknowledgePublish(mapper.toAcknowledgeRequest(ack)))
                    .isNotNull();
        });

        verify(facade).register(argThat(value ->
                value.getInstanceId().equals("instance-1")
                        && value.getBizCode().equals("biz")));
        verify(facade).heartbeat(argThat(value ->
                value.getInstanceId().equals("instance-1")
                        && value.getLeaseId().equals("lease-1")));
        verify(facade).offline(argThat(value ->
                value.getInstanceId().equals("instance-1")
                        && value.getLeaseId().equals("lease-1")));
        verify(facade).pull("biz", "test", "app");
        verify(facade).ack(argThat(value ->
                value.getChangeId().equals("change-1")
                        && value.getStatus() == DdcAckStatus.SUCCESS));
    }

    private DdcServicePrincipal principal(String clientType) {
        return new DdcServicePrincipal(
                "sdk-a", clientType, Set.of("*"), Set.of("*"),
                Set.of("*"), Set.of("*"), "app", "test", "biz");
    }

    private DdcInstanceRegisterRequest registration() {
        DdcInstanceRegisterRequest value = new DdcInstanceRegisterRequest();
        value.setBizCode("biz");
        value.setEnv("test");
        value.setAppCode("app");
        value.setInstanceId("instance-1");
        value.setHost("127.0.0.1");
        value.setPort(8080);
        value.setPid("100");
        value.setSdkVersion("5.3.3");
        value.setLeaseSeconds(30);
        value.setHeartbeatIntervalSeconds(10);
        value.setMetadata(Map.of("zone", "east"));
        return value;
    }

    private DdcHeartbeatRequest heartbeat() {
        DdcHeartbeatRequest value = new DdcHeartbeatRequest();
        value.setBizCode("biz");
        value.setEnv("test");
        value.setAppCode("app");
        value.setInstanceId("instance-1");
        value.setLeaseId("lease-1");
        value.setHost("127.0.0.1");
        value.setPort(8080);
        value.setPid("100");
        value.setSdkVersion("5.3.3");
        value.setMetadata(Map.of("zone", "east"));
        return value;
    }

    private DdcConfigValue config() {
        DdcConfigValue value = new DdcConfigValue();
        value.setResourceName("application.yml");
        value.setContent("feature:\n  enabled: true\n");
        value.setFormat("YAML");
        value.setVersion(2L);
        return value;
    }

    private DdcAckRequest acknowledgement() {
        DdcAckRequest value = new DdcAckRequest();
        value.setChangeId("change-1");
        value.setBizCode("biz");
        value.setEnv("test");
        value.setAppCode("app");
        value.setInstanceId("instance-1");
        value.setLeaseId("lease-1");
        value.setResourceName("application.yml");
        value.setTargetVersion(2L);
        value.setCurrentVersion(2L);
        value.setResourceChecksum("checksum");
        value.setStatus(DdcAckStatus.SUCCESS);
        value.setAckTime(Instant.parse("2026-08-09T00:00:00Z").toEpochMilli());
        return value;
    }
}
