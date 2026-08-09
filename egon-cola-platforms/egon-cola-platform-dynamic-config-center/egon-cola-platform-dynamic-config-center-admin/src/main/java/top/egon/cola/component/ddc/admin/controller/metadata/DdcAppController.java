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
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.admin.model.entity.DdcAppEntity;
import top.egon.cola.component.ddc.admin.service.metadata.DdcAppService;

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
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "namespaceCode", required = false) String namespaceCode,
            @RequestParam(value = "env", required = false) String env,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResultRecord.success(appService.list(
                bizCode, namespaceCode, env, keyword));
    }

    @GetMapping("/{id}")
    public ResultRecord<DdcAppEntity> detail(@PathVariable("id") String id) {
        return ResultRecord.success(appService.findById(id).orElse(null));
    }

    @PostMapping
    public ResultRecord<DdcAppEntity> save(@RequestBody DdcAppEntity request) {
        return ResultRecord.success(appService.save(request));
    }

    @PutMapping("/{id}")
    public ResultRecord<DdcAppEntity> update(
            @PathVariable("id") String id,
            @RequestBody DdcAppEntity request) {
        return ResultRecord.success(appService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResultRecord<Void> delete(@PathVariable("id") String id) {
        appService.delete(id);
        return ResultRecord.success(null);
    }

    @PutMapping("/{id}/enabled")
    public ResultRecord<DdcAppEntity> setEnabled(
            @PathVariable("id") String id,
            @RequestParam("enabled") boolean enabled) {
        return ResultRecord.success(appService.setEnabled(id, enabled));
    }
}
