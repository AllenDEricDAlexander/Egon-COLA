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
import top.egon.cola.component.ddc.admin.model.entity.DdcAppEntity;
import top.egon.cola.component.ddc.admin.service.DdcAppService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/apps")
public class DdcAppController {

    private final DdcAppService appService;

    public DdcAppController(DdcAppService appService) {
        this.appService = appService;
    }

    @GetMapping
    public ResultRecord<List<DdcAppEntity>> list(
            @RequestParam(value = "biz", required = false) String bizCode,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResultRecord.success(appService.list(bizCode, keyword));
    }

    @GetMapping("/{appCode}")
    public ResultRecord<DdcAppEntity> detail(@PathVariable("appCode") String appCode) {
        return ResultRecord.success(appService.findByAppCode(appCode).orElse(null));
    }

    @PostMapping
    public ResultRecord<DdcAppEntity> save(@RequestBody DdcAppEntity request) {
        return ResultRecord.success(appService.save(request));
    }

    @PutMapping("/{appCode}")
    public ResultRecord<DdcAppEntity> update(
            @PathVariable("appCode") String appCode,
            @RequestBody DdcAppEntity request) {
        return ResultRecord.success(appService.update(appCode, request));
    }

    @DeleteMapping("/{appCode}")
    public ResultRecord<Void> delete(@PathVariable("appCode") String appCode) {
        appService.delete(appCode);
        return ResultRecord.success(null);
    }

    @PutMapping("/{appCode}/enabled")
    public ResultRecord<DdcAppEntity> setEnabled(
            @PathVariable("appCode") String appCode,
            @RequestParam("enabled") boolean enabled) {
        return ResultRecord.success(appService.setEnabled(appCode, enabled));
    }
}
