package top.egon.cola.component.ddc.admin.controller.metadata;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.admin.model.entity.DdcBizEntity;
import top.egon.cola.component.ddc.admin.service.metadata.DdcBizService;
import top.egon.cola.component.ddc.admin.support.DdcAdminPageSupport;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/bizs")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "ddc-admin",
        entityDomainName = "Dynamic Config Center 管理实体域",
        code = "ddc-admin-ddc-biz-controller",
        name = "DdcBizController 管理接口组")
public class DdcBizController {

    private final DdcBizService bizService;

    public DdcBizController(DdcBizService bizService) {
        this.bizService = bizService;
    }

    @GatewayOperation(externalAccessible = true)
    @GetMapping
    public ResultRecord<List<DdcBizEntity>> list(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResultRecord.success(bizService.list(keyword));
    }

    @GatewayOperation(externalAccessible = true)
    @GetMapping("/page")
    public PageResultRecord<DdcBizEntity> page(
            @RequestParam(value = "keyword", required = false) String keyword,
            PageQuery pageQuery) {
        return DdcAdminPageSupport.result(bizService.page(keyword, pageQuery));
    }

    @GatewayOperation(externalAccessible = true)
    @GetMapping("/{code}")
    public ResultRecord<DdcBizEntity> detail(@PathVariable("code") String code) {
        return ResultRecord.success(bizService.findByBizCode(code));
    }

    @GatewayOperation(externalAccessible = true)
    @PostMapping
    public ResultRecord<DdcBizEntity> save(@RequestBody DdcBizEntity request) {
        return ResultRecord.success(bizService.save(request));
    }

    @GatewayOperation(externalAccessible = true)
    @PutMapping("/{code}")
    public ResultRecord<DdcBizEntity> update(
            @PathVariable("code") String code,
            @RequestBody DdcBizEntity request) {
        return ResultRecord.success(bizService.update(code, request));
    }

    @GatewayOperation(externalAccessible = true)
    @DeleteMapping("/{code}")
    public ResultRecord<Void> delete(@PathVariable("code") String code) {
        bizService.delete(code);
        return ResultRecord.success(null);
    }

    @GatewayOperation(externalAccessible = true)
    @PutMapping("/{code}/enabled")
    public ResultRecord<DdcBizEntity> setEnabled(
            @PathVariable("code") String code,
            @RequestParam("enabled") boolean enabled) {
        return ResultRecord.success(bizService.setEnabled(code, enabled));
    }
}
