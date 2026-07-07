package top.egon.cola.component.ddc.admin.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.result.Result;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigCreateRequest;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigQueryRequest;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigRollbackRequest;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigUpdateRequest;
import top.egon.cola.component.ddc.admin.model.dto.DdcPublishRequest;
import top.egon.cola.component.ddc.admin.model.vo.DdcConfigVO;
import top.egon.cola.component.ddc.admin.model.vo.DdcConfigVersionVO;
import top.egon.cola.component.ddc.admin.model.vo.DdcPublishResultVO;
import top.egon.cola.component.ddc.admin.service.DdcConfigService;
import top.egon.cola.component.ddc.admin.service.DdcPublishService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/configs")
public class DdcConfigController {

    private final DdcConfigService configService;

    private final DdcPublishService publishService;

    public DdcConfigController(DdcConfigService configService, DdcPublishService publishService) {
        this.configService = configService;
        this.publishService = publishService;
    }

    @GetMapping
    public Result<List<DdcConfigVO>> list(DdcConfigQueryRequest request) {
        return Result.success(configService.list(request));
    }

    @PostMapping
    public Result<DdcConfigVO> create(@RequestBody DdcConfigCreateRequest request,
                                      @RequestParam(name = "operator", defaultValue = "system") String operator) {
        return Result.success(configService.create(request, operator));
    }

    @PutMapping("/{id}")
    public Result<DdcConfigVO> update(@PathVariable("id") String id,
                                      @RequestBody DdcConfigUpdateRequest request,
                                      @RequestParam(name = "operator", defaultValue = "system") String operator) {
        request.setId(id);
        return Result.success(configService.update(request, operator));
    }

    @DeleteMapping("/{id}")
    public Result<DdcConfigVO> delete(@PathVariable("id") String id,
                                      @RequestParam(name = "operator", defaultValue = "system") String operator,
                                      @RequestParam(name = "reason", defaultValue = "delete config") String reason) {
        return Result.success(configService.delete(id, operator, reason));
    }

    @PostMapping("/{id}/publish")
    public Result<DdcPublishResultVO> publish(@PathVariable("id") String id,
                                              @RequestBody DdcPublishRequest request,
                                              @RequestParam(name = "operator", defaultValue = "system") String operator) {
        DdcConfigVO config = configService.get(id);
        request.setAppCode(config.getAppCode());
        request.setEnv(config.getEnv());
        request.setNamespace(config.getNamespace());
        request.setConfigKey(config.getConfigKey());
        return Result.success(publishService.publish(request, operator));
    }

    @GetMapping("/{id}/versions")
    public Result<List<DdcConfigVersionVO>> versions(@PathVariable("id") String id) {
        return Result.success(configService.versions(id));
    }

    @PostMapping("/{id}/rollback")
    public Result<DdcConfigVO> rollback(@PathVariable("id") String id,
                                        @RequestBody DdcConfigRollbackRequest request,
                                        @RequestParam(name = "operator", defaultValue = "system") String operator) {
        request.setConfigId(id);
        return Result.success(configService.rollback(request, operator));
    }
}
