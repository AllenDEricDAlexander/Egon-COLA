package top.egon.cola.component.tianshu.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.controller.metadata.DdcEnvController;
import top.egon.cola.component.tianshu.admin.model.bo.DdcEnvBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcEnvResultConverter;
import top.egon.cola.component.tianshu.admin.model.vo.DdcEnvResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcEnvService;
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

@WebMvcTest(DdcEnvController.class)
@AutoConfigureMockMvc(addFilters = false)
class DdcEnvControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean(name = "ddcEnvService") private DdcEnvService envService;
    @MockBean(name = "ddcEnvResultConverterImpl") private DdcEnvResultConverter resultConverter;

    @Test
    void listDelegatesToService() throws Exception {
        List<DdcEnvBO> rows = List.of();
        when(envService.list(null, null, null)).thenReturn(rows);
        when(resultConverter.toTargetList(rows)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/tianshu/envs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void pagesEnvsWithinMetadataScope() throws Exception {
        DdcEnvBO env = DdcEnvBO.builder().publicId("env-prod").envCode("prod").sortOrder(20).build();
        List<DdcEnvBO> rows = List.of(env);
        when(envService.page(eq("pay-biz"), eq("ops"), eq("pro"), any(PageQuery.class)))
                .thenReturn(DdcPageResult.<DdcEnvBO>builder().records(rows).total(1).pageNo(1).pageSize(10).build());
        when(resultConverter.toTargetList(rows)).thenReturn(List.of(
                new DdcEnvResult("env-prod", "prod", null, 20, null, null, null)));
        mockMvc.perform(get("/api/v1/tianshu/envs/page").param("bizCode", "pay-biz").param("namespaceCode", "ops")
                        .param("keyword", "pro").param("pageNo", "1").param("pageSize", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.records[0].id").value("env-prod"))
                .andExpect(jsonPath("$.records[0].envCode").value("prod"))
                .andExpect(jsonPath("$.page.total").value(1)).andExpect(jsonPath("$.page.pageNo").value(1))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void deleteInUseReturnsFailureCode() throws Exception {
        doThrow(new CommonException(DdcErrorStatus.ENV_IN_USE)).when(envService).delete("prod");
        mockMvc.perform(delete("/api/v1/tianshu/envs/prod")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(DdcErrorStatus.ENV_IN_USE.getCode()));
    }
}
