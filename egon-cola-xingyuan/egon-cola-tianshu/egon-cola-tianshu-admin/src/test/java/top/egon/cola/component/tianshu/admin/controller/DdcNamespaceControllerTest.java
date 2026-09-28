package top.egon.cola.component.tianshu.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.controller.metadata.DdcNamespaceController;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcNamespaceResultConverter;
import top.egon.cola.component.tianshu.admin.model.vo.DdcNamespaceResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcNamespaceService;
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

@WebMvcTest(DdcNamespaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class DdcNamespaceControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean(name = "ddcNamespaceService") private DdcNamespaceService namespaceService;
    @MockBean(name = "ddcNamespaceResultConverterImpl") private DdcNamespaceResultConverter resultConverter;

    @Test
    void listWithBizAndKeywordDelegatesToService() throws Exception {
        List<DdcNamespaceBO> rows = List.of();
        when(namespaceService.list("pay-biz", "default")).thenReturn(rows);
        when(resultConverter.toTargetList(rows)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/tianshu/namespaces").param("bizCode", "pay-biz").param("keyword", "default"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void pagesNamespacesWithoutChangingLegacyList() throws Exception {
        DdcNamespaceBO namespace = DdcNamespaceBO.builder().publicId("namespace-1").bizCode("infra")
                .namespaceCode("ops").namespace("运维").enabled(true).build();
        List<DdcNamespaceBO> rows = List.of(namespace);
        when(namespaceService.page(eq("infra"), eq("ops"), any(PageQuery.class)))
                .thenReturn(DdcPageResult.<DdcNamespaceBO>builder().records(rows).total(1).pageNo(1).pageSize(10).build());
        when(resultConverter.toTargetList(rows)).thenReturn(List.of(
                new DdcNamespaceResult("namespace-1", "infra", "运维", null, true, null, null, "ops")));
        mockMvc.perform(get("/api/v1/tianshu/namespaces/page").param("bizCode", "infra").param("keyword", "ops")
                        .param("pageNo", "1").param("pageSize", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.records[0].id").value("namespace-1"))
                .andExpect(jsonPath("$.records[0].namespaceCode").value("ops"))
                .andExpect(jsonPath("$.page.total").value(1)).andExpect(jsonPath("$.page.pageNo").value(1))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void deleteInUseReturnsFailureCode() throws Exception {
        doThrow(new CommonException(DdcErrorStatus.NAMESPACE_IN_USE)).when(namespaceService).delete("ns-1");
        mockMvc.perform(delete("/api/v1/tianshu/namespaces/ns-1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(DdcErrorStatus.NAMESPACE_IN_USE.getCode()));
    }
}
