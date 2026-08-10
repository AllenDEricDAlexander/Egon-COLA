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
import top.egon.cola.component.ddc.admin.controller.config.DdcPublishTaskController;
import top.egon.cola.component.ddc.admin.model.dto.DdcPublishTaskQueryRequest;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.vo.DdcPublishResultVO;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.ddc.admin.service.publish.DdcPublishService;
import top.egon.cola.component.ddc.admin.service.publish.DdcPublishTaskQueryService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DdcPublishTaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class DdcPublishTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DdcPublishTaskRepository taskRepository;

    @MockBean
    private DdcPublishService publishService;

    @MockBean
    private DdcPublishTaskQueryService publishTaskQueryService;

    @Test
    void pagesPublishTasks() throws Exception {
        DdcPublishTaskEntity task = new DdcPublishTaskEntity();
        task.setId("task-1");
        task.setChangeId("change-1");
        task.setStatus("FAILED");
        when(publishTaskQueryService.page(
                any(DdcPublishTaskQueryRequest.class),
                any(PageQuery.class)
        )).thenReturn(new PageImpl<>(
                List.of(task), PageRequest.of(0, 10), 11
        ));

        mockMvc.perform(get("/api/v1/ddc/publish-tasks/page")
                        .param("bizCode", "infra")
                        .param("env", "prod")
                        .param("appCode", "gateway")
                        .param("status", "FAILED")
                        .param("changeId", "change")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].changeId").value("change-1"))
                .andExpect(jsonPath("$.page.total").value(11))
                .andExpect(jsonPath("$.page.pageNo").value(1))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void retryUsesOriginalChangeId() throws Exception {
        DdcPublishResultVO result = new DdcPublishResultVO();
        result.setChangeId("change-1");
        result.setStatus("SUCCESS");
        when(publishService.retry("change-1")).thenReturn(result);

        mockMvc.perform(post("/api/v1/ddc/publish-tasks/change-1/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.changeId").value("change-1"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }
}
