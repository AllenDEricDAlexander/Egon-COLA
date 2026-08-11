package top.egon.cola.component.rpc.ddc.mapping;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcLeaseOperationStatus.UNRECOGNIZED;

class DdcRegistryProtoMapperTest {

    private final DdcCommonProtoMapper common =
            new DdcCommonProtoMapper(4 * 1024 * 1024);
    private final DdcRegistryProtoMapper mapper =
            new DdcRegistryProtoMapper(common);

    @Test
    void roundTripsRegistrationLeaseAndCompleteSnapshots() {
        DdcServiceKey key = key(DdcServiceKind.RPC_PROVIDER);
        DdcServiceRegistration registration = new DdcServiceRegistration(
                "instance-1", key, "10.0.0.1", 19080, true,
                Map.of("zone", "east"), 30, 10, "service-register-ticket");
        assertThat(mapper.fromRegisterRequest(mapper.toRegisterRequest(registration)))
                .usingRecursiveComparison().isEqualTo(registration);

        DdcServiceLeaseRequest lease = new DdcServiceLeaseRequest();
        lease.setServiceKey(key);
        lease.setInstanceId("instance-1");
        lease.setLeaseId("lease-1");
        lease.setAdmissionTicket("service-heartbeat-ticket");
        assertThat(mapper.fromHeartbeatRequest(mapper.toHeartbeatRequest(lease)))
                .usingRecursiveComparison().isEqualTo(lease);
        assertThat(mapper.fromDeregisterRequest(mapper.toDeregisterRequest(lease)))
                .usingRecursiveComparison()
                .ignoringFields("admissionTicket")
                .isEqualTo(lease);

        Instant now = Instant.parse("2026-08-09T08:00:00.123456Z");
        DdcServiceInstance instance = new DdcServiceInstance(
                "instance-1", "lease-1", key, "10.0.0.1", 19080, true,
                Map.of("zone", "east"), 30, 10, now, now, now.plusSeconds(30),
                "ONLINE", 7L, "resource-order", 12L, "kid-2026",
                now.plusSeconds(20));
        DdcServiceSnapshot snapshot = new DdcServiceSnapshot(
                key, 9L, List.of(instance), now);
        assertThat(mapper.fromInstancesResponse(
                mapper.toInstancesResponse(snapshot)))
                .usingRecursiveComparison().isEqualTo(snapshot);
        assertThat(mapper.toRegisterRequest(registration).getAdmissionTicket())
                .isEqualTo("service-register-ticket");
        assertThat(mapper.toHeartbeatRequest(lease).getAdmissionTicket())
                .isEqualTo("service-heartbeat-ticket");
        assertThat(mapper.toDeregisterRequest(lease).getAllFields().keySet())
                .noneMatch(field -> field.getName().equals("admission_ticket"));
        assertThat(mapper.toProto(instance).getDescriptorForType()
                .findFieldByName("admission_ticket")).isNull();

        DdcServiceQuery query = new DdcServiceQuery(
                "retail", "prod", "order", DdcServiceKind.RPC_PROVIDER,
                "grpc", "OrderRpc", "default", "1.0.0");
        DdcServiceCatalogSnapshot catalog = new DdcServiceCatalogSnapshot(
                query, 10L, List.of(key), now);
        assertThat(mapper.fromServicesResponse(mapper.toServicesResponse(catalog)))
                .usingRecursiveComparison().isEqualTo(catalog);
    }

    @Test
    void mapsEverySharedLeaseAndServiceEnumBothDirections() {
        Instant now = Instant.parse("2026-08-09T08:00:00Z");
        for (DdcLeaseRole role : DdcLeaseRole.values()) {
            DdcLeaseSession value = new DdcLeaseSession(
                    "instance", "lease", role, 30, 10, now, now.plusSeconds(30));
            assertThat(common.fromProto(common.toProto(value)))
                    .isEqualTo(value);
        }
        for (DdcLeaseOperationStatus status : DdcLeaseOperationStatus.values()) {
            DdcLeaseOperationResult value = new DdcLeaseOperationResult(status, now);
            assertThat(common.fromProto(common.toProto(value)))
                    .isEqualTo(value);
        }
        for (DdcServiceKind kind : DdcServiceKind.values()) {
            assertThat(common.fromProto(common.toProto(key(kind))))
                    .isEqualTo(key(kind));
        }
    }

    @Test
    void rejectsUnspecifiedAndUnrecognizedWireEnums() {
        assertThatThrownBy(() -> common.fromProto(
                top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcLeaseOperationResult
                        .newBuilder()
                        .setStatus(top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcLeaseOperationStatus
                                .DDC_LEASE_OPERATION_STATUS_UNSPECIFIED)
                        .build()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> common.fromProtoLeaseOperationStatus(UNRECOGNIZED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private DdcServiceKey key(DdcServiceKind kind) {
        String protocol = kind == DdcServiceKind.HTTP_PROVIDER ? "https" : "grpc";
        return new DdcServiceKey(
                "retail", "prod", "order", kind,
                kind.name() + "Service", "default", "1.0.0", protocol);
    }
}
