package top.egon.cola.component.tianshu.admin.controller.metadata;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
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
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcNamespaceResultConverter;
import top.egon.cola.component.tianshu.admin.model.vo.DdcNamespaceResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcNamespaceService;
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
@RequiredArgsConstructor
@Validated
public class DdcNamespaceController {

    @Qualifier("ddcNamespaceService") private final DdcNamespaceService namespaceService;
    @Qualifier("ddcNamespaceResultConverterImpl") private final DdcNamespaceResultConverter resultConverter;

    @Operation(operationId = "tianshu.ddcNamespaceController.list")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping
    public ResultRecord<List<DdcNamespaceResult>> list(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResultRecord.success(resultConverter.toTargetList(namespaceService.list(bizCode, keyword)));
    }

    @Operation(operationId = "tianshu.ddcNamespaceController.page")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/page")
    public PageResultRecord<DdcNamespaceResult> page(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "keyword", required = false) String keyword,
            PageQuery pageQuery) {
        DdcPageResult<DdcNamespaceBO> page = namespaceService.page(bizCode, keyword, pageQuery);
        return PageResultRecord.success(resultConverter.toTargetList(page.getRecords()), page.getTotal(),
                page.getPageNo(), page.getPageSize());
    }

    @Operation(operationId = "tianshu.ddcNamespaceController.save")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PostMapping
    public ResultRecord<DdcNamespaceResult> save(
            @Validated(EgonColaModelValidationGroups.Insert.class) @RequestBody DdcNamespaceBO request) {
        return ResultRecord.success(resultConverter.toTarget(namespaceService.save(request)));
    }

    @Operation(operationId = "tianshu.ddcNamespaceController.update")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping("/{id}")
    public ResultRecord<DdcNamespaceResult> update(
            @PathVariable("id") String id,
            @Validated(EgonColaModelValidationGroups.Update.class) @RequestBody DdcNamespaceBO request) {
        return ResultRecord.success(resultConverter.toTarget(namespaceService.update(id, request)));
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
    public ResultRecord<DdcNamespaceResult> setEnabled(
            @PathVariable("id") String id,
            @RequestParam("enabled") boolean enabled) {
        return ResultRecord.success(resultConverter.toTarget(namespaceService.setEnabled(id, enabled)));
    }
}
