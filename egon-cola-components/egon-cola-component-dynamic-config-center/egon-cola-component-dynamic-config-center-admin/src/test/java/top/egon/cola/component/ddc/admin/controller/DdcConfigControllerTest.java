package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigCreateRequest;
import top.egon.cola.component.ddc.admin.model.vo.DdcConfigVO;
import top.egon.cola.component.ddc.admin.service.DdcConfigService;
import top.egon.cola.component.ddc.admin.service.DdcPublishService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DdcConfigController.class)
class DdcConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DdcConfigService configService;

    @MockBean
    private DdcPublishService publishService;

    @Test
    void createReturnsConfigResult() throws Exception {
        DdcConfigVO vo = new DdcConfigVO();
        vo.setId("cfg1");
        vo.setAppCode("demo");
        vo.setEnv("dev");
        vo.setNamespace("default");
        vo.setConfigKey("switch");
        when(configService.create(any(DdcConfigCreateRequest.class), eq("tester"))).thenReturn(vo);

        mockMvc.perform(post("/api/v1/ddc/configs?operator=tester")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"appCode":"demo","env":"dev","namespace":"default","configKey":"switch","configValue":"true","defaultValue":"false","valueType":"BOOLEAN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("cfg1"));
    }
}
