package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigCreateRequest;
import top.egon.cola.component.ddc.admin.model.dto.DdcPublishRequest;
import top.egon.cola.component.ddc.admin.model.vo.DdcConfigVO;
import top.egon.cola.component.ddc.admin.model.vo.DdcPublishResultVO;
import top.egon.cola.component.ddc.admin.service.DdcConfigService;
import top.egon.cola.component.ddc.admin.service.DdcPublishService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DdcConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
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
        vo.setBizCode("default");
        vo.setAppCode("demo");
        vo.setEnv("dev");
        vo.setConfigKey("switch");
        when(configService.create(
                any(DdcConfigCreateRequest.class),
                eq("user:controller-test [requested=tester]")
        )).thenReturn(vo);

        mockMvc.perform(post("/api/v1/ddc/configs?operator=tester")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bizCode":"default","appCode":"demo","env":"dev","configKey":"switch","configValue":"true","defaultValue":"false","valueType":"BOOLEAN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("cfg1"));
    }

    @Test
    void publishAcceptsCallerChangeIdExpectedVersionAndTimeout() throws Exception {
        DdcConfigVO config = new DdcConfigVO();
        config.setId("cfg1");
        config.setBizCode("default");
        config.setAppCode("demo");
        config.setEnv("dev");
        config.setConfigKey("switch");
        when(configService.get("cfg1")).thenReturn(config);
        DdcPublishResultVO result = new DdcPublishResultVO();
        result.setChangeId("01919f66-7e0e-7a1a-8000-000000000001");
        result.setStatus("SUCCESS");
        when(publishService.publish(
                any(DdcPublishRequest.class),
                eq("user:controller-test [requested=tester]")
        ))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/ddc/configs/cfg1/publish?operator=tester")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "changeId":"01919f66-7e0e-7a1a-8000-000000000001",
                                  "configValue":"true",
                                  "expectedVersion":1,
                                  "timeoutMs":30000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.changeId")
                        .value("01919f66-7e0e-7a1a-8000-000000000001"));
    }

    private TestingAuthenticationToken authentication() {
        return new TestingAuthenticationToken(
                "controller-test",
                null,
                "ROLE_USER"
        );
    }
}
