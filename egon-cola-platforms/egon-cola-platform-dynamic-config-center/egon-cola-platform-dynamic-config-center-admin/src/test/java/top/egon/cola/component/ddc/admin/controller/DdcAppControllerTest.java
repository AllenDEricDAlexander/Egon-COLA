package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.ddc.admin.model.entity.DdcAppEntity;
import top.egon.cola.component.ddc.admin.service.DdcAppService;
import top.egon.cola.component.ddc.common.DdcErrorStatus;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
        entity.setBizCode("pay-biz");
        entity.setAppName(code);
        entity.setEnabled(true);
        return entity;
    }

    @Test
    void listWithBizAndKeywordDelegatesToService() throws Exception {
        when(appService.list("pay-biz", "orders")).thenReturn(List.of(app("orders")));

        mockMvc.perform(get("/api/v1/ddc/apps")
                        .param("biz", "pay-biz")
                        .param("keyword", "orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].appCode").value("orders"))
                .andExpect(jsonPath("$.data[0].bizCode").value("pay-biz"));
    }

    @Test
    void listWithoutParamsReturnsAllApps() throws Exception {
        when(appService.list(null, null)).thenReturn(List.of(app("orders"), app("billing")));

        mockMvc.perform(get("/api/v1/ddc/apps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void deleteInUseReturnsFailureCode() throws Exception {
        doThrow(new CommonException(DdcErrorStatus.APP_IN_USE))
                .when(appService).delete("orders");

        mockMvc.perform(delete("/api/v1/ddc/apps/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(DdcErrorStatus.APP_IN_USE.getCode()));
    }
}
