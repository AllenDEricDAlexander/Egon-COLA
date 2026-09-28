package top.egon.cola.component.tianshu.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.controller.config.DdcPublishTaskController;
import top.egon.cola.component.tianshu.admin.model.dto.DdcPublishTaskQueryRequest;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishTaskBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcPublishTaskResultConverter;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPublishResultVO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPublishTaskResult;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.tianshu.admin.service.publish.DdcPublishService;
import top.egon.cola.component.tianshu.admin.service.publish.DdcPublishTaskQueryService;

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

    @MockBean
    private DdcPublishTaskResultConverter resultConverter;

    @Test
    void pagesPublishTasks() throws Exception {
        DdcPublishTaskBO task = DdcPublishTaskBO.builder().publicId("task-1")
                .changeId("change-1").status("FAILED").build();
        DdcPublishTaskResult result = DdcPublishTaskResult.builder()
                .publicId("task-1").changeId("change-1").status("FAILED").build();
        when(resultConverter.toTargetList(List.of(task))).thenReturn(List.of(result));
        when(publishTaskQueryService.page(
                any(DdcPublishTaskQueryRequest.class),
                any(PageQuery.class)
        )).thenReturn(DdcPageResult.<DdcPublishTaskBO>builder()
                .records(List.of(task)).total(11).pageNo(1).pageSize(10).build());

        mockMvc.perform(get("/api/v1/tianshu/publish-tasks/page")
                        .param("bizCode", "infra")
                        .param("env", "prod")
                        .param("appCode", "yuheng")
                        .param("status", "FAILED")
                        .param("changeId", "change")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value("task-1"))
                .andExpect(jsonPath("$.records[0].publicId").doesNotExist())
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

        mockMvc.perform(post("/api/v1/tianshu/publish-tasks/change-1/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.changeId").value("change-1"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }
}
