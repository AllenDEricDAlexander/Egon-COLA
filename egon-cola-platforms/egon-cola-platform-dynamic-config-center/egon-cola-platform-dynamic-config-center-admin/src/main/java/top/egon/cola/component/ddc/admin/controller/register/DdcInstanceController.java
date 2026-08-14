package top.egon.cola.component.ddc.admin.controller.register;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.admin.model.entity.DdcInstanceEntity;
import top.egon.cola.component.ddc.admin.service.lease.DdcInstanceAdminService;
import top.egon.cola.component.ddc.admin.support.DdcAdminPageSupport;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/instances")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "ddc-admin",
        entityDomainName = "Dynamic Config Center 管理实体域",
        code = "ddc-admin-ddc-instance-controller",
        name = "DdcInstanceController 管理接口组")
public class DdcInstanceController {

    private final DdcInstanceAdminService instanceAdminService;

    public DdcInstanceController(DdcInstanceAdminService instanceAdminService) {
        this.instanceAdminService = instanceAdminService;
    }

    @GatewayOperation(externalAccessible = true)
    @GetMapping
    public ResultRecord<List<DdcInstanceEntity>> list(
            @RequestParam("bizCode") String bizCode,
            @RequestParam("env") String env,
            @RequestParam("appCode") String appCode) {
        return ResultRecord.success(instanceAdminService.list(
                bizCode, env, appCode));
    }

    @GatewayOperation(externalAccessible = true)
    @GetMapping("/page")
    public PageResultRecord<DdcInstanceEntity> page(
            @RequestParam("bizCode") String bizCode,
            @RequestParam("env") String env,
            @RequestParam("appCode") String appCode,
            PageQuery pageQuery
    ) {
        return DdcAdminPageSupport.result(instanceAdminService.page(
                bizCode, env, appCode, pageQuery));
    }
}
