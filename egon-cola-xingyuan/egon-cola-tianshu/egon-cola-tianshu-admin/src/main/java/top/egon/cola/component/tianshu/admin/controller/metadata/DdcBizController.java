package top.egon.cola.component.tianshu.admin.controller.metadata;

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
import top.egon.cola.component.tianshu.admin.model.entity.DdcBizEntity;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcBizService;
import top.egon.cola.component.tianshu.admin.support.DdcAdminPageSupport;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tianshu/bizs")
@Tag(name = "tianshu-admin-tianshu-biz-controller", description = "DdcBizController 管理接口组")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "tianshu-admin",
        entityDomainName = "Dynamic Config Center 管理实体域",
        interfaceGroupCode = "tianshu"
)
public class DdcBizController {

    private final DdcBizService bizService;

    public DdcBizController(DdcBizService bizService) {
        this.bizService = bizService;
    }

    @Operation(operationId = "tianshu.ddcBizController.list")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping
    public ResultRecord<List<DdcBizEntity>> list(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResultRecord.success(bizService.list(keyword));
    }

    @Operation(operationId = "tianshu.ddcBizController.page")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/page")
    public PageResultRecord<DdcBizEntity> page(
            @RequestParam(value = "keyword", required = false) String keyword,
            PageQuery pageQuery) {
        return DdcAdminPageSupport.result(bizService.page(keyword, pageQuery));
    }

    @Operation(operationId = "tianshu.ddcBizController.detail")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/{code}")
    public ResultRecord<DdcBizEntity> detail(@PathVariable("code") String code) {
        return ResultRecord.success(bizService.findByBizCode(code));
    }

    @Operation(operationId = "tianshu.ddcBizController.save")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PostMapping
    public ResultRecord<DdcBizEntity> save(@RequestBody DdcBizEntity request) {
        return ResultRecord.success(bizService.save(request));
    }

    @Operation(operationId = "tianshu.ddcBizController.update")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping("/{code}")
    public ResultRecord<DdcBizEntity> update(
            @PathVariable("code") String code,
            @RequestBody DdcBizEntity request) {
        return ResultRecord.success(bizService.update(code, request));
    }

    @Operation(operationId = "tianshu.ddcBizController.delete")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @DeleteMapping("/{code}")
    public ResultRecord<Void> delete(@PathVariable("code") String code) {
        bizService.delete(code);
        return ResultRecord.success(null);
    }

    @Operation(operationId = "tianshu.ddcBizController.setEnabled")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping("/{code}/enabled")
    public ResultRecord<DdcBizEntity> setEnabled(
            @PathVariable("code") String code,
            @RequestParam("enabled") boolean enabled) {
        return ResultRecord.success(bizService.setEnabled(code, enabled));
    }
}
