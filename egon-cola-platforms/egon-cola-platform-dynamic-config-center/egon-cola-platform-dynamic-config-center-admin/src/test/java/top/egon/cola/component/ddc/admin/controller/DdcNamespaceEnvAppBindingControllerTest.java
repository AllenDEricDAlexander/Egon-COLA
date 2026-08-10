package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.ddc.admin.controller.metadata.DdcNamespaceEnvAppBindingController;
import top.egon.cola.component.ddc.admin.model.vo.DdcNamespaceEnvAppBindingVO;
import top.egon.cola.component.ddc.admin.service.metadata.DdcNamespaceEnvAppBindingService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DdcNamespaceEnvAppBindingController.class)
@AutoConfigureMockMvc(addFilters = false)
class DdcNamespaceEnvAppBindingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DdcNamespaceEnvAppBindingService bindingService;

    @Test
    void listsBindingsByOptionalVisibilityFilters() throws Exception {
        when(bindingService.list("infra", "ops", "prod", "ge"))
                .thenReturn(List.of(new DdcNamespaceEnvAppBindingVO(
                        "binding-1", "infra", "ns-ops", "ops", "prod",
                        "app-ge", "ge", "Gateway Engine", true
                )));

        mockMvc.perform(get("/api/v1/ddc/namespace-env-app-bindings")
                        .param("bizCode", "infra")
                        .param("namespaceCode", "ops")
                        .param("env", "prod")
                        .param("appCode", "ge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].appCode").value("ge"));
    }

    @Test
    void pagesBindingsWithJoinProjectionEnvelope() throws Exception {
        DdcNamespaceEnvAppBindingVO row = new DdcNamespaceEnvAppBindingVO(
                "binding-1", "infra", "ns-ops", "ops", "prod",
                "app-ge", "ge", "Gateway Engine", true);
        when(bindingService.page(
                eq("infra"), eq("ops"), eq("prod"), eq("ge"),
                any(PageQuery.class)))
                .thenReturn(new PageImpl<>(
                        List.of(row), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/ddc/namespace-env-app-bindings/page")
                        .param("bizCode", "infra")
                        .param("namespaceCode", "ops")
                        .param("env", "prod")
                        .param("appCode", "ge")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].appCode").value("ge"))
                .andExpect(jsonPath("$.page.total").value(1))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
