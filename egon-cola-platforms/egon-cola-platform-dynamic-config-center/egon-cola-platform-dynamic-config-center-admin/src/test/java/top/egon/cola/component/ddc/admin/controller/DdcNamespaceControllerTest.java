package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.ddc.admin.controller.metadata.DdcNamespaceController;
import top.egon.cola.component.ddc.admin.service.metadata.DdcNamespaceService;
import top.egon.cola.component.ddc.error.DdcErrorStatus;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    void listWithBizAndKeywordDelegatesToService() throws Exception {
        when(namespaceService.list("pay-biz", "default"))
                .thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/ddc/namespaces")
                        .param("bizCode", "pay-biz")
                        .param("keyword", "default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteInUseReturnsFailureCode() throws Exception {
        doThrow(new CommonException(DdcErrorStatus.NAMESPACE_IN_USE))
                .when(namespaceService).delete("ns-1");

        mockMvc.perform(delete("/api/v1/ddc/namespaces/ns-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(DdcErrorStatus.NAMESPACE_IN_USE.getCode()));
    }
}
