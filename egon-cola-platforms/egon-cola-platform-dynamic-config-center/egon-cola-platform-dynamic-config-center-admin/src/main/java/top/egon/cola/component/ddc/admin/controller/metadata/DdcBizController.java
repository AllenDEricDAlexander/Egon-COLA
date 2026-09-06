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
import top.egon.cola.component.ddc.admin.model.entity.DdcBizEntity;
import top.egon.cola.component.ddc.admin.service.metadata.DdcBizService;
import top.egon.cola.component.ddc.admin.support.DdcAdminPageSupport;
import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.gateway.openapi.annotation.EgonGatewayPolicy;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/bizs")
@Tag(name = "ddc-admin-ddc-biz-controller", description = "DdcBizController 管理接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "ddc-admin",
        entityDomainName = "Dynamic Config Center 管理实体域",
        interfaceGroupCode = "ddc"
)
public class DdcBizController {

    private final DdcBizService bizService;

    public DdcBizController(DdcBizService bizService) {
        this.bizService = bizService;
    }

    @Operation(operationId = "ddc.ddcBizController.list")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping
    public ResultRecord<List<DdcBizEntity>> list(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResultRecord.success(bizService.list(keyword));
    }

    @Operation(operationId = "ddc.ddcBizController.page")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/page")
    public PageResultRecord<DdcBizEntity> page(
            @RequestParam(value = "keyword", required = false) String keyword,
            PageQuery pageQuery) {
        return DdcAdminPageSupport.result(bizService.page(keyword, pageQuery));
    }

    @Operation(operationId = "ddc.ddcBizController.detail")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/{code}")
    public ResultRecord<DdcBizEntity> detail(@PathVariable("code") String code) {
        return ResultRecord.success(bizService.findByBizCode(code));
    }

    @Operation(operationId = "ddc.ddcBizController.save")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PostMapping
    public ResultRecord<DdcBizEntity> save(@RequestBody DdcBizEntity request) {
        return ResultRecord.success(bizService.save(request));
    }

    @Operation(operationId = "ddc.ddcBizController.update")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping("/{code}")
    public ResultRecord<DdcBizEntity> update(
            @PathVariable("code") String code,
            @RequestBody DdcBizEntity request) {
        return ResultRecord.success(bizService.update(code, request));
    }

    @Operation(operationId = "ddc.ddcBizController.delete")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @DeleteMapping("/{code}")
    public ResultRecord<Void> delete(@PathVariable("code") String code) {
        bizService.delete(code);
        return ResultRecord.success(null);
    }

    @Operation(operationId = "ddc.ddcBizController.setEnabled")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping("/{code}/enabled")
    public ResultRecord<DdcBizEntity> setEnabled(
            @PathVariable("code") String code,
            @RequestParam("enabled") boolean enabled) {
        return ResultRecord.success(bizService.setEnabled(code, enabled));
    }
}
