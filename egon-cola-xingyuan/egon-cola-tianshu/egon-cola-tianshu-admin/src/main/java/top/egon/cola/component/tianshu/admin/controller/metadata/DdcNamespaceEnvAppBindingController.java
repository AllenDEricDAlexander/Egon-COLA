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
import top.egon.cola.component.tianshu.admin.model.dto.DdcNamespaceEnvAppBindingRequest;
import top.egon.cola.component.tianshu.admin.model.vo.DdcNamespaceEnvAppBindingVO;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcNamespaceEnvAppBindingService;
import top.egon.cola.component.tianshu.admin.support.DdcAdminPageSupport;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tianshu/namespace-env-app-bindings")
@Tag(name = "tianshu-admin-tianshu-namespace-env-app-binding-controller", description = "DdcNamespaceEnvAppBindingController 管理接口组")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "tianshu-admin",
        entityDomainName = "Dynamic Config Center 管理实体域",
        interfaceGroupCode = "tianshu"
)
public class DdcNamespaceEnvAppBindingController {

    private final DdcNamespaceEnvAppBindingService bindingService;

    public DdcNamespaceEnvAppBindingController(
            DdcNamespaceEnvAppBindingService bindingService) {
        this.bindingService = bindingService;
    }

    @Operation(operationId = "tianshu.ddcNamespaceEnvAppBindingController.list")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping
    public ResultRecord<List<DdcNamespaceEnvAppBindingVO>> list(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "namespaceCode", required = false) String namespaceCode,
            @RequestParam(value = "env", required = false) String env,
            @RequestParam(value = "appCode", required = false) String appCode) {
        return ResultRecord.success(bindingService.list(
                bizCode, namespaceCode, env, appCode));
    }

    @Operation(operationId = "tianshu.ddcNamespaceEnvAppBindingController.page")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/page")
    public PageResultRecord<DdcNamespaceEnvAppBindingVO> page(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "namespaceCode", required = false) String namespaceCode,
            @RequestParam(value = "env", required = false) String env,
            @RequestParam(value = "appCode", required = false) String appCode,
            PageQuery pageQuery) {
        return DdcAdminPageSupport.result(bindingService.page(
                bizCode, namespaceCode, env, appCode, pageQuery));
    }

    @Operation(operationId = "tianshu.ddcNamespaceEnvAppBindingController.create")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PostMapping
    public ResultRecord<DdcNamespaceEnvAppBindingVO> create(
            @RequestBody DdcNamespaceEnvAppBindingRequest request) {
        return ResultRecord.success(bindingService.create(request));
    }

    @Operation(operationId = "tianshu.ddcNamespaceEnvAppBindingController.update")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping("/{id}")
    public ResultRecord<DdcNamespaceEnvAppBindingVO> update(
            @PathVariable("id") String id,
            @RequestBody DdcNamespaceEnvAppBindingRequest request) {
        return ResultRecord.success(bindingService.update(id, request));
    }

    @Operation(operationId = "tianshu.ddcNamespaceEnvAppBindingController.delete")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @DeleteMapping("/{id}")
    public ResultRecord<Void> delete(@PathVariable("id") String id) {
        bindingService.delete(id);
        return ResultRecord.success(null);
    }
}
