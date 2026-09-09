package top.egon.cola.component.tianshu.admin.controller.config;

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
import top.egon.cola.component.tianshu.admin.model.vo.DdcCacheCheckRow;
import top.egon.cola.component.tianshu.admin.service.cache.DdcCacheService;
import top.egon.cola.component.tianshu.admin.support.DdcAdminPageSupport;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tianshu/cache")
@Tag(name = "tianshu-admin-tianshu-cache-controller", description = "DdcCacheController 管理接口组")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "tianshu-admin",
        entityDomainName = "Dynamic Config Center 管理实体域",
        interfaceGroupCode = "tianshu"
)
public class DdcCacheController {

    private final DdcCacheService cacheService;

    public DdcCacheController(DdcCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Operation(operationId = "tianshu.ddcCacheController.rebuild")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PostMapping("/rebuild")
    public ResultRecord<Integer> rebuild(
            @RequestParam("bizCode") String bizCode,
            @RequestParam("env") String env,
            @RequestParam("appCode") String appCode) {
        return ResultRecord.success(cacheService.rebuild(
                bizCode, env, appCode));
    }

    @Operation(operationId = "tianshu.ddcCacheController.check")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/check")
    public ResultRecord<List<DdcCacheCheckRow>> check(
            @RequestParam("bizCode") String bizCode,
            @RequestParam("env") String env,
            @RequestParam("appCode") String appCode) {
        return ResultRecord.success(cacheService.check(
                bizCode, env, appCode));
    }

    @Operation(operationId = "tianshu.ddcCacheController.page")
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
