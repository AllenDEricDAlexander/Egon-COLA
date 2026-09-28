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
import top.egon.cola.component.tianshu.admin.model.bo.DdcEnvBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcEnvResultConverter;
import top.egon.cola.component.tianshu.admin.model.vo.DdcEnvResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcEnvService;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tianshu/envs")
@Tag(name = "tianshu-admin-tianshu-env-controller", description = "DdcEnvController 管理接口组")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "tianshu-admin",
        entityDomainName = "Dynamic Config Center 管理实体域",
        interfaceGroupCode = "tianshu"
)
@RequiredArgsConstructor
@Validated
public class DdcEnvController {

    @Qualifier("ddcEnvService") private final DdcEnvService envService;
    @Qualifier("ddcEnvResultConverterImpl") private final DdcEnvResultConverter resultConverter;

    @Operation(operationId = "tianshu.ddcEnvController.list")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping
    public ResultRecord<List<DdcEnvResult>> list(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "namespaceCode", required = false) String namespaceCode,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResultRecord.success(resultConverter.toTargetList(envService.list(
                bizCode, namespaceCode, keyword)));
    }

    @Operation(operationId = "tianshu.ddcEnvController.page")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/page")
    public PageResultRecord<DdcEnvResult> page(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "namespaceCode", required = false) String namespaceCode,
            @RequestParam(value = "keyword", required = false) String keyword,
            PageQuery pageQuery) {
        DdcPageResult<DdcEnvBO> page = envService.page(bizCode, namespaceCode, keyword, pageQuery);
        return PageResultRecord.success(resultConverter.toTargetList(page.getRecords()), page.getTotal(),
                page.getPageNo(), page.getPageSize());
    }

    @Operation(operationId = "tianshu.ddcEnvController.detail")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/{code}")
    public ResultRecord<DdcEnvResult> detail(@PathVariable("code") String code) {
        return ResultRecord.success(resultConverter.toTarget(envService.findByEnvCode(code)));
    }

    @Operation(operationId = "tianshu.ddcEnvController.save")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PostMapping
    public ResultRecord<DdcEnvResult> save(
            @Validated(EgonColaModelValidationGroups.Insert.class) @RequestBody DdcEnvBO request) {
        return ResultRecord.success(resultConverter.toTarget(envService.save(request)));
    }

    @Operation(operationId = "tianshu.ddcEnvController.update")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping("/{code}")
    public ResultRecord<DdcEnvResult> update(
            @PathVariable("code") String code,
            @Validated(EgonColaModelValidationGroups.Update.class) @RequestBody DdcEnvBO request) {
        return ResultRecord.success(resultConverter.toTarget(envService.update(code, request)));
    }

    @Operation(operationId = "tianshu.ddcEnvController.delete")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @DeleteMapping("/{code}")
    public ResultRecord<Void> delete(@PathVariable("code") String code) {
        envService.delete(code);
        return ResultRecord.success(null);
    }

    @Operation(operationId = "tianshu.ddcEnvController.setEnabled")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping("/{code}/enabled")
    public ResultRecord<DdcEnvResult> setEnabled(
            @PathVariable("code") String code,
            @RequestParam("enabled") boolean enabled) {
        return ResultRecord.success(resultConverter.toTarget(envService.setEnabled(code, enabled)));
    }
}
