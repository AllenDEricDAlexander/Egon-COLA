package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.ddc.admin.controller.register.DdcInstanceController;
import top.egon.cola.component.ddc.admin.model.entity.DdcInstanceEntity;
import top.egon.cola.component.ddc.admin.service.lease.DdcInstanceAdminService;

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
        DdcInstanceEntity instance = new DdcInstanceEntity();
        instance.setId("row-1");
        instance.setInstanceId("gateway-1");
        when(instanceAdminService.page(
                eq("infra"), eq("prod"), eq("gateway"),
                any(PageQuery.class)
        )).thenReturn(new PageImpl<>(
                List.of(instance), PageRequest.of(0, 1), 2
        ));

        mockMvc.perform(get("/api/v1/ddc/instances/page")
                        .param("bizCode", "infra")
                        .param("env", "prod")
                        .param("appCode", "gateway")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].instanceId")
                        .value("gateway-1"))
                .andExpect(jsonPath("$.page.total").value(2))
                .andExpect(jsonPath("$.page.pageNo").value(1))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
