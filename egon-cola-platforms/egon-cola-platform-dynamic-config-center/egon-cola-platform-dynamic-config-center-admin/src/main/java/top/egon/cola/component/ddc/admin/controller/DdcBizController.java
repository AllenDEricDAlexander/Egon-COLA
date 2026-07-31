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
import top.egon.cola.component.ddc.admin.model.entity.DdcBizEntity;
import top.egon.cola.component.ddc.admin.service.DdcBizService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/bizs")
public class DdcBizController {

    private final DdcBizService bizService;

    public DdcBizController(DdcBizService bizService) {
        this.bizService = bizService;
    }

    @GetMapping
    public ResultRecord<List<DdcBizEntity>> list(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResultRecord.success(bizService.list(keyword));
    }

    @GetMapping("/{code}")
    public ResultRecord<DdcBizEntity> detail(@PathVariable("code") String code) {
        return ResultRecord.success(bizService.findByBizCode(code));
    }

    @PostMapping
    public ResultRecord<DdcBizEntity> save(@RequestBody DdcBizEntity request) {
        return ResultRecord.success(bizService.save(request));
    }

    @PutMapping("/{code}")
    public ResultRecord<DdcBizEntity> update(
            @PathVariable("code") String code,
            @RequestBody DdcBizEntity request) {
        return ResultRecord.success(bizService.update(code, request));
    }

    @DeleteMapping("/{code}")
    public ResultRecord<Void> delete(@PathVariable("code") String code) {
        bizService.delete(code);
        return ResultRecord.success(null);
    }

    @PutMapping("/{code}/enabled")
    public ResultRecord<DdcBizEntity> setEnabled(
            @PathVariable("code") String code,
            @RequestParam("enabled") boolean enabled) {
        return ResultRecord.success(bizService.setEnabled(code, enabled));
    }
}
