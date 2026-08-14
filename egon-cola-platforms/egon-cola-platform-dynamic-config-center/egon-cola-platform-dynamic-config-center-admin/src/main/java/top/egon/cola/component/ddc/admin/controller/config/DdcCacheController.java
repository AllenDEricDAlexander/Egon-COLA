package top.egon.cola.component.ddc.admin.controller.config;

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
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/cache")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "ddc-admin",
        entityDomainName = "Dynamic Config Center 管理实体域",
        code = "ddc-admin-ddc-cache-controller",
        name = "DdcCacheController 管理接口组")
public class DdcCacheController {

    private final DdcCacheService cacheService;

    public DdcCacheController(DdcCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GatewayOperation(externalAccessible = true)
    @PostMapping("/rebuild")
    public ResultRecord<Integer> rebuild(
            @RequestParam("bizCode") String bizCode,
            @RequestParam("env") String env,
            @RequestParam("appCode") String appCode) {
        return ResultRecord.success(cacheService.rebuild(
                bizCode, env, appCode));
    }

    @GatewayOperation(externalAccessible = true)
    @GetMapping("/check")
    public ResultRecord<List<DdcCacheCheckRow>> check(
            @RequestParam("bizCode") String bizCode,
            @RequestParam("env") String env,
            @RequestParam("appCode") String appCode) {
        return ResultRecord.success(cacheService.check(
                bizCode, env, appCode));
    }

    @GatewayOperation(externalAccessible = true)
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
