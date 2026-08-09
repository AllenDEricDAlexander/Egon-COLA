package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.ddc.admin.controller.config.DdcPublishTaskController;
import top.egon.cola.component.ddc.admin.model.vo.DdcPublishResultVO;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.ddc.admin.service.publish.DdcPublishService;

import static org.mockito.Mockito.when;
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
