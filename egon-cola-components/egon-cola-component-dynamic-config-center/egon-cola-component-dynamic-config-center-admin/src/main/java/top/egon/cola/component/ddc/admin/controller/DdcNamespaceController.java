package top.egon.cola.component.ddc.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.result.Result;
import top.egon.cola.component.ddc.admin.model.entity.DdcNamespaceEntity;
import top.egon.cola.component.ddc.admin.service.DdcNamespaceService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/namespaces")
public class DdcNamespaceController {

    private final DdcNamespaceService namespaceService;

    public DdcNamespaceController(DdcNamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    @GetMapping
    public Result<List<DdcNamespaceEntity>> list(@RequestParam("appCode") String appCode,
                                                 @RequestParam("env") String env) {
        return Result.success(namespaceService.list(appCode, env));
    }

    @PostMapping
    public Result<DdcNamespaceEntity> save(@RequestBody DdcNamespaceEntity request) {
        return Result.success(namespaceService.save(request));
    }
}
