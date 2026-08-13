package top.egon.cola.component.gateway.admin.scope.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.ddc.error.management.DdcManagementClientException;
import top.egon.cola.component.ddc.model.management.DdcManagementScopeBinding;
import top.egon.cola.component.gateway.admin.application.domain.po.GatewayApplicationPO;
import top.egon.cola.component.gateway.admin.application.repository.GatewayApplicationRepository;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayScopeServiceTest {

    private DdcManagementClient client;

    private GatewayApplicationRepository applications;

    private GatewayScopeService service;

    @BeforeEach
    void setUp() {
        client = mock(DdcManagementClient.class);
        applications = mock(GatewayApplicationRepository.class);
        service = new GatewayScopeService(client, applications);
    }

    @Test
    void mapsTwoNamespaceBindingsToOneGatewayApplication() {
        when(client.getScopeBindings(any())).thenReturn(List.of(
                binding("binding-default", "default", true),
                binding("binding-ops", "ops", true)
        ));
        when(applications.findAllByDeletedFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(application("gateway-order")));

        assertThat(service.list())
                .extracting(
                        top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO::namespace,
                        top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO::gatewayApplicationId
                )
                .containsExactly(
                        tuple("default", "gateway-order"),
                        tuple("ops", "gateway-order")
                );
    }

    @Test
    void reportsDdcFailureInsteadOfReturningStaticScopes() {
        when(client.getScopeBindings(any()))
                .thenThrow(new DdcManagementClientException(
                        "DDC_MANAGEMENT_IO_ERROR",
                        "offline",
                        null
                ));

        assertThatThrownBy(service::list)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DDC scope catalog");
    }

    private DdcManagementScopeBinding binding(
            String bindingId,
            String namespace,
            boolean enabled) {
        return new DdcManagementScopeBinding(
                bindingId,
                "retail",
                namespace,
                "local",
                "ddc-order",
                "order",
                "Order",
                enabled
        );
    }

    private GatewayApplicationPO application(String id) {
        return new GatewayApplicationPO(
                id,
                "retail",
                "order",
                "Order",
                "local",
                "default",
                null,
                "admin",
                Instant.parse("2026-08-01T00:00:00Z")
        );
    }
}
