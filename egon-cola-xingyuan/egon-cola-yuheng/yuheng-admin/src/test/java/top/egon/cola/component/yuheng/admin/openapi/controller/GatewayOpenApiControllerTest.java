package top.egon.cola.component.yuheng.admin.openapi.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.component.yuheng.admin.openapi.domain.vo.GatewayOpenApiDocumentVO;
import top.egon.cola.component.yuheng.admin.openapi.domain.vo.GatewayOpenApiSyncStateVO;
import top.egon.cola.component.yuheng.admin.openapi.domain.vo.GatewayOperationOpenApiVO;
import top.egon.cola.component.yuheng.admin.openapi.service.GatewayOpenApiQueryService;
import top.egon.cola.component.yuheng.admin.shared.controller.GatewayAdminExceptionHandler;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GatewayOpenApiControllerTest {

    @Test
    void exposesReadOnlySyncOperationAndDocumentContracts() throws Exception {
        GatewayOpenApiQueryService service = mock(GatewayOpenApiQueryService.class);
        when(service.listSyncStates("platform", "gateway", "test", "orders"))
                .thenReturn(List.of(new GatewayOpenApiSyncStateVO(
                        "sync-1", "application-1", "build-1", "1.0.0", "orders",
                        "VALID", "snapshot-1", "set-1", 2, 1,
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        null, null, null, Instant.parse("2026-08-26T03:00:00Z"),
                        Instant.parse("2026-08-26T03:00:01Z"), null)));
        when(service.getSnapshotDocument("snapshot-1"))
                .thenReturn(new GatewayOpenApiDocumentVO(
                        "snapshot-1", "application-1", "build-1", "3.1.0",
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                        Map.of("openapi", "3.1.0"),
                        Instant.parse("2026-08-26T03:00:00Z"),
                        Instant.parse("2026-08-26T03:00:01Z")));
        when(service.getOperationOpenApi("operation-1"))
                .thenReturn(new GatewayOperationOpenApiVO(
                        "operation-1",
                        "orders:http:GET:/orders/{id}",
                        "snapshot-1",
                        "3.1.0",
                        "orders",
                        "/orders/{id}",
                        "GET",
                        "getOrder",
                        List.of("application/json"),
                        List.of("application/json"),
                        Map.of("operationId", "getOrder"),
                        Instant.parse("2026-08-26T03:00:01Z")));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                        new GatewayOpenApiController(service))
                .setControllerAdvice(new GatewayAdminExceptionHandler())
                .build();

        mvc.perform(get("/api/v1/gateway/admin/openapi/sync-states")
                        .queryParam("bizCode", "platform")
                        .queryParam("namespace", "gateway")
                        .queryParam("env", "test")
                        .queryParam("appCode", "orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].openapiGroup").value("orders"))
                .andExpect(jsonPath("$[0].status").value("VALID"));

        mvc.perform(get("/api/v1/gateway/admin/openapi/snapshots/snapshot-1/document"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\""))
                .andExpect(jsonPath("$.document.openapi").value("3.1.0"));

        mvc.perform(get("/api/v1/gateway/admin/operations/operation-1/openapi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapiGroup").value("orders"))
                .andExpect(jsonPath("$.operation.operationId").value("getOrder"));

        verify(service).listSyncStates("platform", "gateway", "test", "orders");
        verify(service).getSnapshotDocument("snapshot-1");
        verify(service).getOperationOpenApi("operation-1");
    }

    @Test
    void mapsMissingReadResourceThroughExistingAdminAdvice() throws Exception {
        GatewayOpenApiQueryService service = mock(GatewayOpenApiQueryService.class);
        when(service.getSnapshotDocument("missing"))
                .thenThrow(new top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminNotFoundException(
                        "OpenAPI snapshot was not found"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                        new GatewayOpenApiController(service))
                .setControllerAdvice(new GatewayAdminExceptionHandler())
                .build();

        mvc.perform(get("/api/v1/gateway/admin/openapi/snapshots/missing/document"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GATEWAY_ADMIN_NOT_FOUND"));
    }

    @Test
    void mapsUnavailableOpenApiSourceToConflict() throws Exception {
        GatewayOpenApiQueryService service = mock(GatewayOpenApiQueryService.class);
        when(service.getOperationOpenApi("rpc-operation"))
                .thenThrow(new top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayOpenApiSourceNotAvailableException());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                        new GatewayOpenApiController(service))
                .setControllerAdvice(new GatewayAdminExceptionHandler())
                .build();

        mvc.perform(get("/api/v1/gateway/admin/operations/rpc-operation/openapi"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("GATEWAY_OPENAPI_SOURCE_NOT_AVAILABLE"));
    }
}
