package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.ddc.admin.service.DdcConfigService;
import top.egon.cola.component.ddc.admin.service.DdcInstanceAdminService;
import top.egon.cola.component.ddc.admin.service.DdcPublishService;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DdcOpenApiController.class)
@AutoConfigureMockMvc(addFilters = false)
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
    void registrationReturnsTheAdminIssuedLease() throws Exception {
        when(instanceAdminService.register(any())).thenReturn(new DdcLeaseSession(
                "instance-1",
                "lease-1",
                DdcLeaseRole.CONFIG_CLIENT,
                30,
                10,
                Instant.parse("2026-07-24T12:00:00Z"),
                Instant.parse("2026-07-24T12:00:30Z")
        ));

        mockMvc.perform(post("/api/v1/ddc/openapi/instances/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instanceId":"instance-1","appCode":"demo","env":"dev","namespace":"default","host":"127.0.0.1","port":8080,"pid":"100","sdkVersion":"5.2.3","leaseSeconds":30,"heartbeatIntervalSeconds":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.instanceId").value("instance-1"))
                .andExpect(jsonPath("$.data.leaseId").value("lease-1"))
                .andExpect(jsonPath("$.data.role").value("CONFIG_CLIENT"));
    }

    @Test
    void heartbeatAndOfflineReturnLeaseOperationResults() throws Exception {
        when(instanceAdminService.heartbeat(any())).thenReturn(new DdcLeaseOperationResult(
                DdcLeaseOperationStatus.RENEWED,
                Instant.parse("2026-07-24T12:01:00Z")
        ));
        when(instanceAdminService.offline(any())).thenReturn(new DdcLeaseOperationResult(
                DdcLeaseOperationStatus.DELETED,
                null
        ));
        String request = """
                {"instanceId":"instance-1","leaseId":"lease-1","appCode":"demo","env":"dev","namespace":"default"}
                """;

        mockMvc.perform(post("/api/v1/ddc/openapi/instances/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RENEWED"))
                .andExpect(jsonPath("$.data.leaseExpireAt").value("2026-07-24T12:01:00Z"));

        mockMvc.perform(post("/api/v1/ddc/openapi/instances/offline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"));
    }

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
