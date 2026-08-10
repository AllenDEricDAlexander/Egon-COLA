package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.ddc.admin.controller.config.DdcConfigController;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigCreateRequest;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigQueryRequest;
import top.egon.cola.component.ddc.admin.model.dto.DdcPublishRequest;
import top.egon.cola.component.ddc.admin.model.vo.DdcConfigVO;
import top.egon.cola.component.ddc.admin.model.vo.DdcConfigVersionVO;
import top.egon.cola.component.ddc.admin.model.vo.DdcPublishResultVO;
import top.egon.cola.component.ddc.admin.service.config.DdcConfigService;
import top.egon.cola.component.ddc.admin.service.publish.DdcPublishService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        vo.setResourceName("application.yml");
        vo.setFormat("YAML");
        when(configService.create(
                any(DdcConfigCreateRequest.class),
                eq("user:controller-test [requested=tester]")
        )).thenReturn(vo);

        mockMvc.perform(post("/api/v1/ddc/configs?operator=tester")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bizCode":"default","appCode":"demo","env":"dev","resourceName":"application.yml","content":"feature:\\n  enabled: true\\n","format":"YAML"}
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
        config.setResourceName("application.yml");
        config.setFormat("YAML");
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
                                  "content":"feature:\\n  enabled: true\\n",
                                  "expectedVersion":1,
                                  "timeoutMs":30000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.changeId")
                        .value("01919f66-7e0e-7a1a-8000-000000000001"));
    }

    @Test
    void pagesConfigsAndVersionHistory() throws Exception {
        DdcConfigVO config = new DdcConfigVO();
        config.setId("config-1");
        config.setBizCode("infra");
        config.setAppCode("gateway");
        config.setEnv("prod");
        DdcConfigVersionVO version = new DdcConfigVersionVO();
        version.setId("version-2");
        version.setConfigId("config-1");
        version.setVersion(2L);
        when(configService.page(
                any(DdcConfigQueryRequest.class), any(PageQuery.class)))
                .thenReturn(new PageImpl<>(
                        List.of(config), PageRequest.of(0, 10), 12));
        when(configService.pageVersions(
                eq("config-1"), any(PageQuery.class)))
                .thenReturn(new PageImpl<>(
                        List.of(version), PageRequest.of(1, 20), 21));

        mockMvc.perform(get("/api/v1/ddc/configs/page")
                        .param("bizCode", "infra")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value("config-1"))
                .andExpect(jsonPath("$.page.total").value(12));

        mockMvc.perform(get("/api/v1/ddc/configs/config-1/versions/page")
                        .param("pageNo", "2")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value("version-2"))
                .andExpect(jsonPath("$.page.total").value(21))
                .andExpect(jsonPath("$.page.pageNo").value(2));
    }

    private TestingAuthenticationToken authentication() {
        return new TestingAuthenticationToken(
                "controller-test",
                null,
                "ROLE_USER"
        );
    }
}
