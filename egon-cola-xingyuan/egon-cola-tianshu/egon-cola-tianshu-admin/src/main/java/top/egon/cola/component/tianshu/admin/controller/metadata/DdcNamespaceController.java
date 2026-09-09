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
import top.egon.cola.component.tianshu.admin.model.entity.DdcNamespaceEntity;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcNamespaceService;
import top.egon.cola.component.tianshu.admin.support.DdcAdminPageSupport;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tianshu/namespaces")
@Tag(name = "tianshu-admin-tianshu-namespace-controller", description = "DdcNamespaceController 管理接口组")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "tianshu-admin",
        entityDomainName = "Dynamic Config Center 管理实体域",
        interfaceGroupCode = "tianshu"
)
public class DdcNamespaceController {

    private final DdcNamespaceService namespaceService;

    public DdcNamespaceController(DdcNamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    @Operation(operationId = "tianshu.ddcNamespaceController.list")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping
    public ResultRecord<List<DdcNamespaceEntity>> list(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResultRecord.success(namespaceService.list(bizCode, keyword));
    }

    @Operation(operationId = "tianshu.ddcNamespaceController.page")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/page")
    public PageResultRecord<DdcNamespaceEntity> page(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "keyword", required = false) String keyword,
            PageQuery pageQuery) {
        return DdcAdminPageSupport.result(
                namespaceService.page(bizCode, keyword, pageQuery));
    }

    @Operation(operationId = "tianshu.ddcNamespaceController.save")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PostMapping
    public ResultRecord<DdcNamespaceEntity> save(@RequestBody DdcNamespaceEntity request) {
        return ResultRecord.success(namespaceService.save(request));
    }

    @Operation(operationId = "tianshu.ddcNamespaceController.update")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping("/{id}")
    public ResultRecord<DdcNamespaceEntity> update(
            @PathVariable("id") String id,
            @RequestBody DdcNamespaceEntity request) {
        return ResultRecord.success(namespaceService.update(id, request));
    }

    @Operation(operationId = "tianshu.ddcNamespaceController.delete")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @DeleteMapping("/{id}")
    public ResultRecord<Void> delete(@PathVariable("id") String id) {
        namespaceService.delete(id);
        return ResultRecord.success(null);
    }

    @Operation(operationId = "tianshu.ddcNamespaceController.setEnabled")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping("/{id}/enabled")
    public ResultRecord<DdcNamespaceEntity> setEnabled(
            @PathVariable("id") String id,
            @RequestParam("enabled") boolean enabled) {
        return ResultRecord.success(namespaceService.setEnabled(id, enabled));
    }
}
