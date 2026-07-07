package top.egon.cola.component.ddc.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.result.dto.ResultDto;
import top.egon.cola.component.common.result.factory.ResultDtos;
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
    public ResultDto<List<DdcAppEntity>> list() {
        return ResultDtos.success(appService.list());
    }

    @PostMapping
    public ResultDto<DdcAppEntity> save(@RequestBody DdcAppEntity request) {
        return ResultDtos.success(appService.save(request));
    }
}
