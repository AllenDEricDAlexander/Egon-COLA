package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.ddc.admin.model.vo.DdcNamespaceEnvAppBindingVO;
import top.egon.cola.component.ddc.admin.service.management.DdcManagementFacade;
import top.egon.cola.component.ddc.admin.service.metadata.DdcNamespaceEnvAppBindingService;
import top.egon.cola.component.ddc.management.model.DdcManagementConfig;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishStatus;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTarget;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTask;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DdcManagementOpenApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class DdcManagementOpenApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DdcManagementFacade facade;

    @MockBean
    private DdcNamespaceEnvAppBindingService bindingService;

    @Test
    void pathScopeOverridesDuplicatedBodyIdentity() throws Exception {
        when(facade.upsert(any())).thenReturn(new DdcManagementConfig(
                "infra",
                "dev",
                "gateway",
                "application.yml",
                "gateway:\n  enabled: true\n",
                "YAML",
                1L,
                true,
                false,
                Instant.parse("2026-07-25T02:00:00Z")
        ));

        mockMvc.perform(put(
                        "/api/v1/ddc/openapi/management/configs"
                                + "/infra/dev/gateway"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bizCode":"forged-biz",
                                  "appCode":"forged",
                                  "env":"prod",
                                  "resourceName":"application.yml",
                                  "content":"gateway:\\n  enabled: true\\n",
                                  "format":"YAML",
                                  "description":"routes",
                                  "operator":"gateway-admin"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.appCode").value("gateway"))
                .andExpect(jsonPath("$.data.resourceName").value("application.yml"));
    }

    @Test
    void exactConfigEndpointReturnsManagementProjection() throws Exception {
        when(facade.findConfig(any())).thenReturn(new DdcManagementConfig(
                "gateway",
                "dev",
                "runtime",
                "application.yml",
                "gateway:\n  enabled: true\n",
                "YAML",
                2L,
                false,
                true,
                Instant.parse("2026-07-26T00:00:00Z")
        ));

        mockMvc.perform(get(
                        "/api/v1/ddc/openapi/management/configs"
                                + "/gateway/dev/runtime"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resourceName").value("application.yml"))
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.deleted").value(true));
    }

    @Test
    void taskEndpointReturnsStableTargetProjection() throws Exception {
        when(facade.getPublishTask("change-1")).thenReturn(new DdcManagementPublishTask(
                "change-1",
                DdcManagementPublishStatus.SUCCESS,
                2L,
                "checksum",
                1,
                1,
                0,
                0,
                0,
                1,
                List.of(new DdcManagementPublishTarget(
                        "engine-1",
                        "lease-1",
                        2L,
                        "SUCCESS",
                        null,
                        Instant.parse("2026-07-25T02:00:02Z")
                )),
                null,
                Instant.parse("2026-07-25T02:00:00Z"),
                Instant.parse("2026-07-25T02:00:01Z"),
                Instant.parse("2026-07-25T02:00:02Z")
        ));

        mockMvc.perform(get(
                        "/api/v1/ddc/openapi/management/publish-tasks/change-1"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.changeId").value("change-1"))
                .andExpect(jsonPath("$.data.targets[0].instanceId").value("engine-1"))
                .andExpect(jsonPath("$.data.targets[0].leaseId").value("lease-1"));
    }

    @Test
    void scopeBindingsAcceptAnySubsetOfFilters() throws Exception {
        when(bindingService.list("retail", null, null, "order"))
                .thenReturn(List.of(new DdcNamespaceEnvAppBindingVO(
                        "binding-1",
                        "retail",
                        "ns-ops",
                        "ops",
                        "local",
                        "app-order",
                        "order",
                        "Order",
                        true
                )));

        mockMvc.perform(get(
                        "/api/v1/ddc/openapi/management/scope-bindings"
                )
                        .param("bizCode", "retail")
                        .param("appCode", "order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].namespaceCode").value("ops"))
                .andExpect(jsonPath("$.data[0].appCode").value("order"));
    }
}
