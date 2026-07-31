package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.ddc.admin.service.DdcBizService;
import top.egon.cola.component.ddc.common.DdcErrorStatus;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DdcBizController.class)
@AutoConfigureMockMvc(addFilters = false)
class DdcBizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DdcBizService bizService;

    @Test
    void listWithKeywordDelegatesToService() throws Exception {
        when(bizService.list("pay")).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/ddc/bizs").param("keyword", "pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteInUseReturnsFailureCode() throws Exception {
        doThrow(new CommonException(DdcErrorStatus.BIZ_IN_USE))
                .when(bizService).delete("pay");

        mockMvc.perform(delete("/api/v1/ddc/bizs/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(DdcErrorStatus.BIZ_IN_USE.getCode()));
    }
}
