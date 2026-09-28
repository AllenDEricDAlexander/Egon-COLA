package top.egon.cola.component.tianshu.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.controller.register.DdcInstanceController;
import top.egon.cola.component.tianshu.admin.model.vo.DdcInstanceResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.service.lease.DdcInstanceAdminService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DdcInstanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class DdcInstanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DdcInstanceAdminService instanceAdminService;

    @Test
    void pagesPersistentInstances() throws Exception {
        DdcInstanceResult instance = DdcInstanceResult.builder()
                .publicId("row-1").instanceId("yuheng-1").build();
        when(instanceAdminService.page(
                eq("infra"), eq("prod"), eq("yuheng"),
                any(PageQuery.class))).thenReturn(DdcPageResult.<DdcInstanceResult>builder()
                .records(List.of(instance)).total(2).pageNo(1).pageSize(10).build());

        mockMvc.perform(get("/api/v1/tianshu/instances/page")
                        .param("bizCode", "infra")
                        .param("env", "prod")
                        .param("appCode", "yuheng")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value("row-1"))
                .andExpect(jsonPath("$.records[0].publicId").doesNotExist())
                .andExpect(jsonPath("$.records[0].instanceId")
                        .value("yuheng-1"))
                .andExpect(jsonPath("$.page.total").value(2))
                .andExpect(jsonPath("$.page.pageNo").value(1))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
