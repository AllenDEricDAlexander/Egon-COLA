package top.egon.cola.component.ddc.admin.controller.metadata;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.gateway.openapi.annotation.EgonGatewayPolicy;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/envs")
@Tag(name = "ddc-admin-ddc-env-controller", description = "DdcEnvController 管理接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "ddc-admin",
        entityDomainName = "Dynamic Config Center 管理实体域",
        interfaceGroupCode = "ddc-admin-ddc-env-controller"
)
public class DdcEnvController {

    private final DdcEnvService envService;

    public DdcEnvController(DdcEnvService envService) {
        this.envService = envService;
    }

    @Operation(operationId = "ddc.ddcEnvController.list")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping
    public ResultRecord<List<DdcEnvEntity>> list(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "namespaceCode", required = false) String namespaceCode,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResultRecord.success(envService.list(
                bizCode, namespaceCode, keyword));
    }

    @Operation(operationId = "ddc.ddcEnvController.page")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/page")
    public PageResultRecord<DdcEnvEntity> page(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "namespaceCode", required = false) String namespaceCode,
            @RequestParam(value = "keyword", required = false) String keyword,
            PageQuery pageQuery) {
        return DdcAdminPageSupport.result(envService.page(
                bizCode, namespaceCode, keyword, pageQuery));
    }

    @Operation(operationId = "ddc.ddcEnvController.detail")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/{code}")
    public ResultRecord<DdcEnvEntity> detail(@PathVariable("code") String code) {
        return ResultRecord.success(envService.findByEnvCode(code));
    }

    @Operation(operationId = "ddc.ddcEnvController.save")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PostMapping
    public ResultRecord<DdcEnvEntity> save(@RequestBody DdcEnvEntity request) {
        return ResultRecord.success(envService.save(request));
    }

    @Operation(operationId = "ddc.ddcEnvController.update")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping("/{code}")
    public ResultRecord<DdcEnvEntity> update(
            @PathVariable("code") String code,
            @RequestBody DdcEnvEntity request) {
        return ResultRecord.success(envService.update(code, request));
    }

    @Operation(operationId = "ddc.ddcEnvController.delete")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @DeleteMapping("/{code}")
    public ResultRecord<Void> delete(@PathVariable("code") String code) {
        envService.delete(code);
        return ResultRecord.success(null);
    }

    @Operation(operationId = "ddc.ddcEnvController.setEnabled")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping("/{code}/enabled")
    public ResultRecord<DdcEnvEntity> setEnabled(
            @PathVariable("code") String code,
            @RequestParam("enabled") boolean enabled) {
        return ResultRecord.success(envService.setEnabled(code, enabled));
    }
}
