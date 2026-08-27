package top.egon.cola.component.ddc.admin.controller.config;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.admin.model.vo.DdcCacheCheckRow;
import top.egon.cola.component.ddc.admin.service.cache.DdcCacheService;
import top.egon.cola.component.ddc.admin.support.DdcAdminPageSupport;
import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.gateway.openapi.annotation.EgonGatewayPolicy;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/cache")
@Tag(name = "ddc-admin-ddc-cache-controller", description = "DdcCacheController 管理接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "ddc-admin",
        entityDomainName = "Dynamic Config Center 管理实体域",
        interfaceGroupCode = "ddc-admin-ddc-cache-controller"
)
public class DdcCacheController {

    private final DdcCacheService cacheService;

    public DdcCacheController(DdcCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Operation(operationId = "ddc.ddcCacheController.rebuild")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PostMapping("/rebuild")
    public ResultRecord<Integer> rebuild(
            @RequestParam("bizCode") String bizCode,
            @RequestParam("env") String env,
            @RequestParam("appCode") String appCode) {
        return ResultRecord.success(cacheService.rebuild(
                bizCode, env, appCode));
    }

    @Operation(operationId = "ddc.ddcCacheController.check")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/check")
    public ResultRecord<List<DdcCacheCheckRow>> check(
            @RequestParam("bizCode") String bizCode,
            @RequestParam("env") String env,
            @RequestParam("appCode") String appCode) {
        return ResultRecord.success(cacheService.check(
                bizCode, env, appCode));
    }

    @Operation(operationId = "ddc.ddcCacheController.page")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/check/page")
    public PageResultRecord<DdcCacheCheckRow> page(
            @RequestParam("bizCode") String bizCode,
            @RequestParam("env") String env,
            @RequestParam("appCode") String appCode,
            PageQuery pageQuery
    ) {
        return DdcAdminPageSupport.result(
                cacheService.page(bizCode, env, appCode, pageQuery));
    }
}
