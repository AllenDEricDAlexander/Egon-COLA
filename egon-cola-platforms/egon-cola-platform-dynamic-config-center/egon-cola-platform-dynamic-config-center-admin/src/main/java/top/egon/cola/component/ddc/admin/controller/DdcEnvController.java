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
import top.egon.cola.component.ddc.admin.model.entity.DdcEnvEntity;
import top.egon.cola.component.ddc.admin.service.DdcEnvService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/envs")
public class DdcEnvController {

    private final DdcEnvService envService;

    public DdcEnvController(DdcEnvService envService) {
        this.envService = envService;
    }

    @GetMapping
    public ResultRecord<List<DdcEnvEntity>> list(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "namespaceCode", required = false) String namespaceCode,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResultRecord.success(envService.list(
                bizCode, namespaceCode, keyword));
    }

    @GetMapping("/{code}")
    public ResultRecord<DdcEnvEntity> detail(@PathVariable("code") String code) {
        return ResultRecord.success(envService.findByEnvCode(code));
    }

    @PostMapping
    public ResultRecord<DdcEnvEntity> save(@RequestBody DdcEnvEntity request) {
        return ResultRecord.success(envService.save(request));
    }

    @PutMapping("/{code}")
    public ResultRecord<DdcEnvEntity> update(
            @PathVariable("code") String code,
            @RequestBody DdcEnvEntity request) {
        return ResultRecord.success(envService.update(code, request));
    }

    @DeleteMapping("/{code}")
    public ResultRecord<Void> delete(@PathVariable("code") String code) {
        envService.delete(code);
        return ResultRecord.success(null);
    }

    @PutMapping("/{code}/enabled")
    public ResultRecord<DdcEnvEntity> setEnabled(
            @PathVariable("code") String code,
            @RequestParam("enabled") boolean enabled) {
        return ResultRecord.success(envService.setEnabled(code, enabled));
    }
}
