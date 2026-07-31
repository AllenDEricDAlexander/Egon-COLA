package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.ddc.admin.service.DdcNamespaceService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DdcNamespaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class DdcNamespaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DdcNamespaceService namespaceService;

    @Test
    void domainsReturnsDistinctSortedNamespaceValues() throws Exception {
        when(namespaceService.findDomains()).thenReturn(List.of("billing", "orders"));

        mockMvc.perform(get("/api/v1/ddc/namespaces/domains"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0]").value("billing"))
                .andExpect(jsonPath("$.data[1]").value("orders"));
    }

    @Test
    void domainsReturnsEmptyListWhenNoData() throws Exception {
        when(namespaceService.findDomains()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/ddc/namespaces/domains"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
