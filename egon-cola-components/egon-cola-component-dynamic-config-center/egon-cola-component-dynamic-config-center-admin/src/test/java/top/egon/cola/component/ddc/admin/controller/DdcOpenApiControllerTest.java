package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.ddc.admin.service.DdcConfigService;
import top.egon.cola.component.ddc.admin.service.DdcInstanceAdminService;
import top.egon.cola.component.ddc.admin.service.DdcPublishService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DdcOpenApiController.class)
class DdcOpenApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DdcInstanceAdminService instanceAdminService;

    @MockBean
    private DdcConfigService configService;

    @MockBean
    private DdcPublishService publishService;

    @Test
    void ackReturnsSuccessResult() throws Exception {
        mockMvc.perform(post("/api/v1/ddc/openapi/publish/ack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"changeId":"c1","instanceId":"i1","appCode":"demo","env":"dev","namespace":"default","configKey":"switch","targetVersion":2,"currentVersion":2,"status":"SUCCESS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
