package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.ddc.admin.service.registry.DdcServiceRegistryService;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DdcRegistryOpenApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class DdcRegistryOpenApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DdcServiceRegistryService registryService;

    @Test
    void registrationReturnsIssuedLease() throws Exception {
        when(registryService.register(any())).thenReturn(new DdcLeaseSession(
                "provider-1",
                "lease-1",
                DdcLeaseRole.RPC_PROVIDER,
                30,
                10,
                Instant.parse("2026-07-24T12:00:00Z"),
                Instant.parse("2026-07-24T12:00:30Z")
        ));

        mockMvc.perform(post("/api/v1/ddc/openapi/registry/instances/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "instanceId":"provider-1",
                                  "serviceKey":{
                                    "bizCode":"pay-biz",
                                    "appCode":"orders-app",
                                    "env":"dev",
                                    "serviceKind":"RPC_PROVIDER",
                                    "serviceName":"order.v1.OrderQueryService",
                                    "group":"default",
                                    "version":"1.0.0",
                                    "protocol":"grpc"
                                  },
                                  "host":"127.0.0.1",
                                  "port":19090,
                                  "secure":false,
                                  "metadata":{"zone":"east"},
                                  "leaseSeconds":30,
                                  "heartbeatIntervalSeconds":10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instanceId").value("provider-1"))
                .andExpect(jsonPath("$.data.leaseId").value("lease-1"));
    }

    @Test
    void serviceCatalogEndpointAcceptsPartialFilters() throws Exception {
        when(registryService.getServiceKeys(any())).thenAnswer(invocation -> {
            DdcServiceQuery query = invocation.getArgument(0);
            org.assertj.core.api.Assertions.assertThat(query.bizCode()).isEqualTo("pay-biz");
            org.assertj.core.api.Assertions.assertThat(query.appCode()).isNull();
            org.assertj.core.api.Assertions.assertThat(query.env()).isNull();
            org.assertj.core.api.Assertions.assertThat(query.serviceKind()).isNull();
            return new DdcServiceCatalogSnapshot(query, 3L, List.of(), Instant.now());
        });

        mockMvc.perform(get("/api/v1/ddc/openapi/registry/services")
                        .param("bizCode", "pay-biz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revision").value(3));
    }
}
