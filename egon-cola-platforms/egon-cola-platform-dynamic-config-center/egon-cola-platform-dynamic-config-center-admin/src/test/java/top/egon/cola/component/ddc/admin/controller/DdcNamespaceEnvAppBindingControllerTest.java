package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.ddc.admin.model.vo.DdcNamespaceEnvAppBindingVO;
import top.egon.cola.component.ddc.admin.service.DdcNamespaceEnvAppBindingService;

import java.util.List;

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
}
