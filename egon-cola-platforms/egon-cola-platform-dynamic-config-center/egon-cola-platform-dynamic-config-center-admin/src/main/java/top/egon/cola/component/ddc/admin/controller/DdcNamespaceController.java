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
import top.egon.cola.component.common.core.pojo.ResultRecord;
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
    public ResultRecord<List<DdcNamespaceEntity>> list(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResultRecord.success(namespaceService.list(bizCode, keyword));
    }

    @PostMapping
    public ResultRecord<DdcNamespaceEntity> save(@RequestBody DdcNamespaceEntity request) {
        return ResultRecord.success(namespaceService.save(request));
    }

    @PutMapping("/{id}")
    public ResultRecord<DdcNamespaceEntity> update(
            @PathVariable("id") String id,
            @RequestBody DdcNamespaceEntity request) {
        return ResultRecord.success(namespaceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResultRecord<Void> delete(@PathVariable("id") String id) {
        namespaceService.delete(id);
        return ResultRecord.success(null);
    }

    @PutMapping("/{id}/enabled")
    public ResultRecord<DdcNamespaceEntity> setEnabled(
            @PathVariable("id") String id,
            @RequestParam("enabled") boolean enabled) {
        return ResultRecord.success(namespaceService.setEnabled(id, enabled));
    }
}
