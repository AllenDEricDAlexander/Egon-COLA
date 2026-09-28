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
import top.egon.cola.component.tianshu.admin.model.bo.DdcAppBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAppResultConverter;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAppResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcAppService;
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
@RequiredArgsConstructor
@Validated
public class DdcAppController {

    @Qualifier("ddcAppService") private final DdcAppService appService;
    @Qualifier("ddcAppResultConverterImpl") private final DdcAppResultConverter resultConverter;

    @Operation(operationId = "tianshu.ddcAppController.list")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping
    public ResultRecord<List<DdcAppResult>> list(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "namespaceCode", required = false) String namespaceCode,
            @RequestParam(value = "env", required = false) String env,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResultRecord.success(resultConverter.toTargetList(appService.list(
                bizCode, namespaceCode, env, keyword)));
    }

    @Operation(operationId = "tianshu.ddcAppController.page")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/page")
    public PageResultRecord<DdcAppResult> page(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "namespaceCode", required = false) String namespaceCode,
            @RequestParam(value = "env", required = false) String env,
            @RequestParam(value = "keyword", required = false) String keyword,
            PageQuery pageQuery) {
        DdcPageResult<DdcAppBO> page = appService.page(bizCode, namespaceCode, env, keyword, pageQuery);
        return PageResultRecord.success(resultConverter.toTargetList(page.getRecords()), page.getTotal(),
                page.getPageNo(), page.getPageSize());
    }

    @Operation(operationId = "tianshu.ddcAppController.detail")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/{id}")
    public ResultRecord<DdcAppResult> detail(@PathVariable("id") String id) {
        return ResultRecord.success(appService.findById(id).map(resultConverter::toTarget).orElse(null));
    }

    @Operation(operationId = "tianshu.ddcAppController.save")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PostMapping
    public ResultRecord<DdcAppResult> save(
            @Validated(EgonColaModelValidationGroups.Insert.class) @RequestBody DdcAppBO request) {
        return ResultRecord.success(resultConverter.toTarget(appService.save(request)));
    }

    @Operation(operationId = "tianshu.ddcAppController.update")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping("/{id}")
    public ResultRecord<DdcAppResult> update(
            @PathVariable("id") String id,
            @Validated(EgonColaModelValidationGroups.Update.class) @RequestBody DdcAppBO request) {
        return ResultRecord.success(resultConverter.toTarget(appService.update(id, request)));
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
    public ResultRecord<DdcAppResult> setEnabled(
            @PathVariable("id") String id,
            @RequestParam("enabled") boolean enabled) {
        return ResultRecord.success(resultConverter.toTarget(appService.setEnabled(id, enabled)));
    }
}
