package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.ddc.admin.model.entity.DdcAppEntity;
import top.egon.cola.component.ddc.admin.service.DdcAppService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DdcAppController.class)
@AutoConfigureMockMvc(addFilters = false)
class DdcAppControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DdcAppService appService;

    private DdcAppEntity app(String code) {
        DdcAppEntity entity = new DdcAppEntity();
        entity.setId(code);
        entity.setAppCode(code);
        entity.setAppName(code);
        entity.setEnabled(true);
        return entity;
    }

    @Test
    void listWithoutNamespaceReturnsAllApps() throws Exception {
        when(appService.list()).thenReturn(List.of(app("orders"), app("billing")));

        mockMvc.perform(get("/api/v1/ddc/apps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void listWithNamespaceReturnsOnlyDomainApps() throws Exception {
        when(appService.findByNamespace("orders-domain")).thenReturn(List.of(app("orders")));

        mockMvc.perform(get("/api/v1/ddc/apps").param("namespace", "orders-domain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].appCode").value("orders"));
    }
}
