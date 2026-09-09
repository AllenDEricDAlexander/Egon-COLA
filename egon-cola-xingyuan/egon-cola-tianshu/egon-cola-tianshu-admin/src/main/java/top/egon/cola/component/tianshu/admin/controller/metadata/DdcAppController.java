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
import top.egon.cola.component.tianshu.admin.model.entity.DdcAppEntity;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcAppService;
import top.egon.cola.component.tianshu.admin.support.DdcAdminPageSupport;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tianshu/apps")
@Tag(name = "tianshu-admin-tianshu-app-controller", description = "DdcAppController 管理接口组")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "tianshu-admin",
        entityDomainName = "Dynamic Config Center 管理实体域",
        interfaceGroupCode = "tianshu"
)
public class DdcAppController {

    private final DdcAppService appService;

    public DdcAppController(DdcAppService appService) {
        this.appService = appService;
    }

    @Operation(operationId = "tianshu.ddcAppController.list")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping
    public ResultRecord<List<DdcAppEntity>> list(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "namespaceCode", required = false) String namespaceCode,
            @RequestParam(value = "env", required = false) String env,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResultRecord.success(appService.list(
                bizCode, namespaceCode, env, keyword));
    }

    @Operation(operationId = "tianshu.ddcAppController.page")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/page")
    public PageResultRecord<DdcAppEntity> page(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "namespaceCode", required = false) String namespaceCode,
            @RequestParam(value = "env", required = false) String env,
            @RequestParam(value = "keyword", required = false) String keyword,
            PageQuery pageQuery) {
        return DdcAdminPageSupport.result(appService.page(
                bizCode, namespaceCode, env, keyword, pageQuery));
    }

    @Operation(operationId = "tianshu.ddcAppController.detail")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/{id}")
    public ResultRecord<DdcAppEntity> detail(@PathVariable("id") String id) {
        return ResultRecord.success(appService.findById(id).orElse(null));
    }

    @Operation(operationId = "tianshu.ddcAppController.save")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PostMapping
    public ResultRecord<DdcAppEntity> save(@RequestBody DdcAppEntity request) {
        return ResultRecord.success(appService.save(request));
    }

    @Operation(operationId = "tianshu.ddcAppController.update")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping("/{id}")
    public ResultRecord<DdcAppEntity> update(
            @PathVariable("id") String id,
            @RequestBody DdcAppEntity request) {
        return ResultRecord.success(appService.update(id, request));
    }

    @Operation(operationId = "tianshu.ddcAppController.delete")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @DeleteMapping("/{id}")
    public ResultRecord<Void> delete(@PathVariable("id") String id) {
        appService.delete(id);
        return ResultRecord.success(null);
    }

    @Operation(operationId = "tianshu.ddcAppController.setEnabled")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping("/{id}/enabled")
    public ResultRecord<DdcAppEntity> setEnabled(
            @PathVariable("id") String id,
            @RequestParam("enabled") boolean enabled) {
        return ResultRecord.success(appService.setEnabled(id, enabled));
    }
}
