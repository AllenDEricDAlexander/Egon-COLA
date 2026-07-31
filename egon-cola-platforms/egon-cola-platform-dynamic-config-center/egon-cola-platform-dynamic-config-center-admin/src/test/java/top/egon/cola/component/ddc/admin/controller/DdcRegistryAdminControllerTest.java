package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.component.ddc.admin.service.DdcManagementFacade;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceInstance;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceKey;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DdcRegistryAdminControllerTest {

    private final DdcManagementFacade facade = mock(DdcManagementFacade.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new DdcRegistryAdminController(facade))
            .build();

    @Test
    void returnsServiceCatalogThroughJwtAdminPath() throws Exception {
        when(facade.getServiceKeys(any())).thenReturn(
                new DdcManagementServiceCatalog(
                        7L,
                        Instant.parse("2026-07-28T04:00:00Z"),
                        List.of(key())
                )
        );

        mockMvc.perform(get("/api/v1/ddc/registry/services")
                        .param("bizCode", "pay-biz")
                        .param("appCode", "orders-app")
                        .param("env", "dev")
                        .param("namespace", "codex-local")
                        .param("serviceKind", "RPC_PROVIDER")
                        .param("protocol", "grpc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.generation").value(7))
                .andExpect(jsonPath("$.data.services[0].serviceName")
                        .value("egon.gateway.test.v1.EchoService"));
    }

    @Test
    void returnsServiceInstancesThroughJwtAdminPath() throws Exception {
        when(facade.getInstances(any())).thenReturn(
                new DdcManagementServiceSnapshot(
                        key(),
                        8L,
                        Instant.parse("2026-07-28T04:00:01Z"),
                        List.of(new DdcManagementServiceInstance(
                                "rpc-provider-local-1",
                                "lease-1",
                                "192.168.6.186",
                                19101,
                                false,
                                Map.of("buildId", "local-rpc-build"),
                                "ONLINE",
                                Instant.parse("2026-07-28T03:59:00Z"),
                                Instant.parse("2026-07-28T04:00:00Z"),
                                Instant.parse("2026-07-28T04:01:00Z")
                        ))
                )
        );

        mockMvc.perform(get("/api/v1/ddc/registry/instances")
                        .param("bizCode", "pay-biz")
                        .param("appCode", "orders-app")
                        .param("env", "dev")
                        .param("namespace", "codex-local")
                        .param("serviceKind", "RPC_PROVIDER")
                        .param("protocol", "grpc")
                        .param(
                                "serviceName",
                                "egon.gateway.test.v1.EchoService"
                        )
                        .param("group", "default")
                        .param("version", "1.0.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generation").value(8))
                .andExpect(jsonPath("$.data.instances[0].instanceId")
                        .value("rpc-provider-local-1"))
                .andExpect(jsonPath("$.data.instances[0].port")
                        .value(19101));
    }

    private DdcManagementServiceKey key() {
        return new DdcManagementServiceKey(
                "dev",
                "codex-local",
                "RPC_PROVIDER",
                "egon.gateway.test.v1.EchoService",
                "default",
                "1.0.0",
                "grpc"
        );
    }

}
