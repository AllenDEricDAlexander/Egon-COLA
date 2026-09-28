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
import top.egon.cola.component.tianshu.admin.model.bo.DdcBizBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcBizResultConverter;
import top.egon.cola.component.tianshu.admin.model.vo.DdcBizResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcBizService;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tianshu/bizs")
@Tag(name = "tianshu-admin-tianshu-biz-controller", description = "DdcBizController 管理接口组")
@EgonApiCatalog(businessDomainCode = "xingyuan", businessDomainName = "平台治理域", entityDomainCode = "tianshu-admin",
        entityDomainName = "Dynamic Config Center 管理实体域", interfaceGroupCode = "tianshu")
@RequiredArgsConstructor
@Validated
public class DdcBizController {
    @Qualifier("ddcBizService") private final DdcBizService bizService;
    @Qualifier("ddcBizResultConverterImpl") private final DdcBizResultConverter resultConverter;

    @Operation(operationId = "tianshu.ddcBizController.list")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping
    public ResultRecord<List<DdcBizResult>> list(@RequestParam(value = "keyword", required = false) String keyword) {
        return ResultRecord.success(resultConverter.toTargetList(bizService.list(keyword)));
    }
    @Operation(operationId = "tianshu.ddcBizController.page")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/page")
    public PageResultRecord<DdcBizResult> page(@RequestParam(value = "keyword", required = false) String keyword,
            PageQuery pageQuery) {
        DdcPageResult<DdcBizBO> page = bizService.page(keyword, pageQuery);
        return PageResultRecord.success(resultConverter.toTargetList(page.getRecords()), page.getTotal(),
                page.getPageNo(), page.getPageSize());
    }
    @Operation(operationId = "tianshu.ddcBizController.detail")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/{code}")
    public ResultRecord<DdcBizResult> detail(@PathVariable("code") String code) {
        return ResultRecord.success(resultConverter.toTarget(bizService.findByBizCode(code)));
    }
    @Operation(operationId = "tianshu.ddcBizController.save")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PostMapping
    public ResultRecord<DdcBizResult> save(@Validated(EgonColaModelValidationGroups.Insert.class) @RequestBody DdcBizBO request) {
        return ResultRecord.success(resultConverter.toTarget(bizService.save(request)));
    }
    @Operation(operationId = "tianshu.ddcBizController.update")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping("/{code}")
    public ResultRecord<DdcBizResult> update(@PathVariable("code") String code,
            @Validated(EgonColaModelValidationGroups.Update.class) @RequestBody DdcBizBO request) {
        return ResultRecord.success(resultConverter.toTarget(bizService.update(code, request)));
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
    public ResultRecord<DdcBizResult> setEnabled(@PathVariable("code") String code,
            @RequestParam("enabled") boolean enabled) {
        return ResultRecord.success(resultConverter.toTarget(bizService.setEnabled(code, enabled)));
    }
}
