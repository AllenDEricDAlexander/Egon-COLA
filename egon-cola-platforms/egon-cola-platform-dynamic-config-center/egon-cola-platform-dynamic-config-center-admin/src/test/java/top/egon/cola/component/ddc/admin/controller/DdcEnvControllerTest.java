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
import top.egon.cola.component.ddc.admin.controller.metadata.DdcEnvController;
import top.egon.cola.component.ddc.admin.model.entity.DdcEnvEntity;
import top.egon.cola.component.ddc.admin.service.metadata.DdcEnvService;
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

@WebMvcTest(DdcEnvController.class)
@AutoConfigureMockMvc(addFilters = false)
class DdcEnvControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DdcEnvService envService;

    @Test
    void listDelegatesToService() throws Exception {
        when(envService.list(null, null, null)).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/ddc/envs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void pagesEnvsWithinMetadataScope() throws Exception {
        DdcEnvEntity env = new DdcEnvEntity();
        env.setId("env-prod");
        env.setEnvCode("prod");
        env.setSortOrder(20);
        when(envService.page(
                eq("pay-biz"), eq("ops"), eq("pro"), any(PageQuery.class)))
                .thenReturn(new PageImpl<>(
                        List.of(env), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/ddc/envs/page")
                        .param("bizCode", "pay-biz")
                        .param("namespaceCode", "ops")
                        .param("keyword", "pro")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].envCode").value("prod"))
                .andExpect(jsonPath("$.page.total").value(1))
                .andExpect(jsonPath("$.page.pageNo").value(1))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void deleteInUseReturnsFailureCode() throws Exception {
        doThrow(new CommonException(DdcErrorStatus.ENV_IN_USE))
                .when(envService).delete("prod");

        mockMvc.perform(delete("/api/v1/ddc/envs/prod"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(DdcErrorStatus.ENV_IN_USE.getCode()));
    }
}
