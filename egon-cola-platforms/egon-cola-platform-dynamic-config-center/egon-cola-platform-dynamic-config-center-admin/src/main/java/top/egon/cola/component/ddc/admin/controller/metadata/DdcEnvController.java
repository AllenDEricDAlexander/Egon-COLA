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
import top.egon.cola.component.ddc.admin.model.entity.DdcEnvEntity;
import top.egon.cola.component.ddc.admin.service.metadata.DdcEnvService;
import top.egon.cola.component.ddc.admin.support.DdcAdminPageSupport;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/envs")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "ddc-admin",
        entityDomainName = "Dynamic Config Center 管理实体域",
        code = "ddc-admin-ddc-env-controller",
        name = "DdcEnvController 管理接口组")
public class DdcEnvController {

    private final DdcEnvService envService;

    public DdcEnvController(DdcEnvService envService) {
        this.envService = envService;
    }

    @GatewayOperation(externalAccessible = true)
    @GetMapping
    public ResultRecord<List<DdcEnvEntity>> list(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "namespaceCode", required = false) String namespaceCode,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResultRecord.success(envService.list(
                bizCode, namespaceCode, keyword));
    }

    @GatewayOperation(externalAccessible = true)
    @GetMapping("/page")
    public PageResultRecord<DdcEnvEntity> page(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "namespaceCode", required = false) String namespaceCode,
            @RequestParam(value = "keyword", required = false) String keyword,
            PageQuery pageQuery) {
        return DdcAdminPageSupport.result(envService.page(
                bizCode, namespaceCode, keyword, pageQuery));
    }

    @GatewayOperation(externalAccessible = true)
    @GetMapping("/{code}")
    public ResultRecord<DdcEnvEntity> detail(@PathVariable("code") String code) {
        return ResultRecord.success(envService.findByEnvCode(code));
    }

    @GatewayOperation(externalAccessible = true)
    @PostMapping
    public ResultRecord<DdcEnvEntity> save(@RequestBody DdcEnvEntity request) {
        return ResultRecord.success(envService.save(request));
    }

    @GatewayOperation(externalAccessible = true)
    @PutMapping("/{code}")
    public ResultRecord<DdcEnvEntity> update(
            @PathVariable("code") String code,
            @RequestBody DdcEnvEntity request) {
        return ResultRecord.success(envService.update(code, request));
    }

    @GatewayOperation(externalAccessible = true)
    @DeleteMapping("/{code}")
    public ResultRecord<Void> delete(@PathVariable("code") String code) {
        envService.delete(code);
        return ResultRecord.success(null);
    }

    @GatewayOperation(externalAccessible = true)
    @PutMapping("/{code}/enabled")
    public ResultRecord<DdcEnvEntity> setEnabled(
            @PathVariable("code") String code,
            @RequestParam("enabled") boolean enabled) {
        return ResultRecord.success(envService.setEnabled(code, enabled));
    }
}
