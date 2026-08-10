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
import top.egon.cola.component.ddc.admin.controller.metadata.DdcNamespaceController;
import top.egon.cola.component.ddc.admin.model.entity.DdcNamespaceEntity;
import top.egon.cola.component.ddc.admin.service.metadata.DdcNamespaceService;
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
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void pagesNamespacesWithoutChangingLegacyList() throws Exception {
        DdcNamespaceEntity namespace = new DdcNamespaceEntity();
        namespace.setId("namespace-1");
        namespace.setBizCode("infra");
        namespace.setNamespaceCode("ops");
        namespace.setNamespace("运维");
        when(namespaceService.page(
                eq("infra"), eq("ops"), any(PageQuery.class)))
                .thenReturn(new PageImpl<>(
                        List.of(namespace), PageRequest.of(0, 10), 1
                ));

        mockMvc.perform(get("/api/v1/ddc/namespaces/page")
                        .param("bizCode", "infra")
                        .param("keyword", "ops")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].namespaceCode").value("ops"))
                .andExpect(jsonPath("$.page.total").value(1))
                .andExpect(jsonPath("$.page.pageNo").value(1))
                .andExpect(jsonPath("$.data").doesNotExist());
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
