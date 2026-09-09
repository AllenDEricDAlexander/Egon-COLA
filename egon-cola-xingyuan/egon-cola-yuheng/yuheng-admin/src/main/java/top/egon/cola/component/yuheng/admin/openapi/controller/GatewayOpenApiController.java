package top.egon.cola.component.yuheng.admin.openapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.yuheng.admin.openapi.domain.vo.GatewayOpenApiDocumentVO;
import top.egon.cola.component.yuheng.admin.openapi.domain.vo.GatewayOpenApiSyncStateVO;
import top.egon.cola.component.yuheng.admin.openapi.domain.vo.GatewayOperationOpenApiVO;
import top.egon.cola.component.yuheng.admin.openapi.service.GatewayOpenApiQueryService;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

import java.util.List;

/**
 * Read-only Gateway OpenAPI monitoring and provenance endpoints.
 *
 * <p>中文：所有接口只读取 Admin 本地投影；Provider 地址、凭证和网络抓取均不
 * 暴露给调用方。</p>
 */
@Validated
@RestController
@RequestMapping("/api/v1/yuheng/admin")
@PreAuthorize("hasAnyAuthority('CAP_yuheng:read','CAP_*')")
@Tag(name = "yuheng-admin")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "yuheng-admin",
        entityDomainName = "Gateway Admin 管理实体域",
        interfaceGroupCode = "yuheng-admin")
public class GatewayOpenApiController {

    private final GatewayOpenApiQueryService queryService;

    /** Creates the controller with the read-only query facade. */
    public GatewayOpenApiController(GatewayOpenApiQueryService queryService) {
        this.queryService = queryService;
    }

    /** Returns filtered OpenAPI synchronization state rows. */
    @Operation(operationId = "admin.gatewayOpenApiController.listSyncStates")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/openapi/sync-states")
    public List<GatewayOpenApiSyncStateVO> listSyncStates(
            @RequestParam(required = false)
            @Size(max = 128) String bizCode,
            @RequestParam(required = false)
            @Size(max = 128) String namespace,
            @RequestParam(required = false)
            @Size(max = 64) String env,
            @RequestParam(required = false)
            @Size(max = 128) String appCode) {
        return queryService.listSyncStates(
                bizCode,
                namespace,
                env,
                appCode);
    }

    /** Returns the stored OpenAPI fragment for the current operation. */
    @Operation(operationId = "admin.gatewayOpenApiController.operation")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/operations/{operationId}/openapi")
    public GatewayOperationOpenApiVO operation(
            @PathVariable
            @NotBlank @Size(max = 64) String operationId) {
        return queryService.getOperationOpenApi(operationId);
    }

    /** Returns an immutable raw snapshot with an ETag based on canonical hash. */
    @Operation(operationId = "admin.gatewayOpenApiController.document")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/openapi/snapshots/{snapshotId}/document")
    public ResponseEntity<GatewayOpenApiDocumentVO> document(
            @PathVariable
            @NotBlank @Size(max = 64) String snapshotId) {
        GatewayOpenApiDocumentVO document = queryService.getSnapshotDocument(
                snapshotId);
        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG,
                        quote(document.canonicalSha256()))
                .header(HttpHeaders.CACHE_CONTROL, "private, immutable")
                .body(document);
    }

    private String quote(String value) {
        return "\"" + value + "\"";
    }
}
