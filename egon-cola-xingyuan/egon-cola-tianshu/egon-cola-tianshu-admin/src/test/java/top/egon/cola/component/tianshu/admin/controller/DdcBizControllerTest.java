package top.egon.cola.component.tianshu.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.controller.metadata.DdcBizController;
import top.egon.cola.component.tianshu.admin.model.bo.DdcBizBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcBizResultConverter;
import top.egon.cola.component.tianshu.admin.model.vo.DdcBizResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcBizService;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;

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
    @Autowired private MockMvc mockMvc;
    @MockBean(name = "ddcBizService") private DdcBizService bizService;
    @MockBean(name = "ddcBizResultConverterImpl") private DdcBizResultConverter resultConverter;

    @Test
    void listWithKeywordDelegatesToService() throws Exception {
        List<DdcBizBO> rows = List.of();
        when(bizService.list("pay")).thenReturn(rows);
        when(resultConverter.toTargetList(rows)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/tianshu/bizs").param("keyword", "pay"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void pagesBizsWithoutChangingLegacyWireFields() throws Exception {
        DdcBizBO biz = DdcBizBO.builder().publicId("biz-1").bizCode("pay").bizName("支付").enabled(true).build();
        DdcBizResult result = new DdcBizResult("biz-1", "pay", "支付", null, true, null, null);
        List<DdcBizBO> rows = List.of(biz);
        when(bizService.page(eq("pay"), any(PageQuery.class))).thenReturn(DdcPageResult.<DdcBizBO>builder()
                .records(rows).total(21).pageNo(2).pageSize(20).build());
        when(resultConverter.toTargetList(rows)).thenReturn(List.of(result));
        mockMvc.perform(get("/api/v1/tianshu/bizs/page").param("keyword", "pay")
                        .param("pageNo", "2").param("pageSize", "20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.records[0].id").value("biz-1"))
                .andExpect(jsonPath("$.records[0].bizCode").value("pay"))
                .andExpect(jsonPath("$.page.total").value(21)).andExpect(jsonPath("$.page.pageNo").value(2))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void deleteInUseReturnsFailureCode() throws Exception {
        doThrow(new CommonException(DdcErrorStatus.BIZ_IN_USE)).when(bizService).delete("pay");
        mockMvc.perform(delete("/api/v1/tianshu/bizs/pay")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(DdcErrorStatus.BIZ_IN_USE.getCode()));
    }
}
