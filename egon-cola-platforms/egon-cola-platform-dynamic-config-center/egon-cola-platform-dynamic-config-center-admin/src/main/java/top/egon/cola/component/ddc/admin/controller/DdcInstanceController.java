package top.egon.cola.component.ddc.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.admin.model.entity.DdcInstanceEntity;
import top.egon.cola.component.ddc.admin.service.DdcInstanceAdminService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/instances")
public class DdcInstanceController {

    private final DdcInstanceAdminService instanceAdminService;

    public DdcInstanceController(DdcInstanceAdminService instanceAdminService) {
        this.instanceAdminService = instanceAdminService;
    }

    @GetMapping
    public ResultRecord<List<DdcInstanceEntity>> list(
            @RequestParam("bizCode") String bizCode,
            @RequestParam("env") String env,
            @RequestParam("appCode") String appCode) {
        return ResultRecord.success(instanceAdminService.list(
                bizCode, env, appCode));
    }
}
