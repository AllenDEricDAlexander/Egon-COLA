package top.egon.cola.component.ddc.admin.controller.metadata;

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
import top.egon.cola.component.ddc.admin.model.dto.DdcNamespaceEnvAppBindingRequest;
import top.egon.cola.component.ddc.admin.model.vo.DdcNamespaceEnvAppBindingVO;
import top.egon.cola.component.ddc.admin.service.metadata.DdcNamespaceEnvAppBindingService;
import top.egon.cola.component.ddc.admin.support.DdcAdminPageSupport;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/namespace-env-app-bindings")
public class DdcNamespaceEnvAppBindingController {

    private final DdcNamespaceEnvAppBindingService bindingService;

    public DdcNamespaceEnvAppBindingController(
            DdcNamespaceEnvAppBindingService bindingService) {
        this.bindingService = bindingService;
    }

    @GetMapping
    public ResultRecord<List<DdcNamespaceEnvAppBindingVO>> list(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "namespaceCode", required = false) String namespaceCode,
            @RequestParam(value = "env", required = false) String env,
            @RequestParam(value = "appCode", required = false) String appCode) {
        return ResultRecord.success(bindingService.list(
                bizCode, namespaceCode, env, appCode));
    }

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

    @PostMapping
    public ResultRecord<DdcNamespaceEnvAppBindingVO> create(
            @RequestBody DdcNamespaceEnvAppBindingRequest request) {
        return ResultRecord.success(bindingService.create(request));
    }

    @PutMapping("/{id}")
    public ResultRecord<DdcNamespaceEnvAppBindingVO> update(
            @PathVariable("id") String id,
            @RequestBody DdcNamespaceEnvAppBindingRequest request) {
        return ResultRecord.success(bindingService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResultRecord<Void> delete(@PathVariable("id") String id) {
        bindingService.delete(id);
        return ResultRecord.success(null);
    }
}
