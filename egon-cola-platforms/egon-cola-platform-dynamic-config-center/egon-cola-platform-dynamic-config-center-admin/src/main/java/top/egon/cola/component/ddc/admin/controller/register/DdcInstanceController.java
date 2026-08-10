package top.egon.cola.component.ddc.admin.controller.register;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.admin.model.entity.DdcInstanceEntity;
import top.egon.cola.component.ddc.admin.service.lease.DdcInstanceAdminService;
import top.egon.cola.component.ddc.admin.support.DdcAdminPageSupport;

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

    @GetMapping("/page")
    public PageResultRecord<DdcInstanceEntity> page(
            @RequestParam("bizCode") String bizCode,
            @RequestParam("env") String env,
            @RequestParam("appCode") String appCode,
            PageQuery pageQuery
    ) {
        return DdcAdminPageSupport.result(instanceAdminService.page(
                bizCode, env, appCode, pageQuery));
    }
}
