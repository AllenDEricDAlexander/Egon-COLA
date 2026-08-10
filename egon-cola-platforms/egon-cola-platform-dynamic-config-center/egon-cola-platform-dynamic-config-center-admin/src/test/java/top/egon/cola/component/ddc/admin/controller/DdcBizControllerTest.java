package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.ddc.admin.controller.metadata.DdcBizController;
import top.egon.cola.component.ddc.admin.model.entity.DdcBizEntity;
import top.egon.cola.component.ddc.admin.service.metadata.DdcBizService;
import top.egon.cola.component.ddc.error.DdcErrorStatus;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void pagesBizsWithoutChangingLegacyList() throws Exception {
        DdcBizEntity biz = new DdcBizEntity();
        biz.setId("biz-1");
        biz.setBizCode("pay");
        biz.setBizName("支付");
        when(bizService.page(eq("pay"), any(PageQuery.class)))
                .thenReturn(new PageImpl<>(
                        List.of(biz), PageRequest.of(1, 20), 21
                ));

        mockMvc.perform(get("/api/v1/ddc/bizs/page")
                        .param("keyword", "pay")
                        .param("pageNo", "2")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].bizCode").value("pay"))
                .andExpect(jsonPath("$.page.total").value(21))
                .andExpect(jsonPath("$.page.pageNo").value(2))
                .andExpect(jsonPath("$.data").doesNotExist());
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
