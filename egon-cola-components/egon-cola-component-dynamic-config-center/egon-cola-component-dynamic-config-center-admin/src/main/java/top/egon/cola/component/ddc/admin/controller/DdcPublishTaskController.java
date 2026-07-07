package top.egon.cola.component.ddc.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.result.dto.ResultDto;
import top.egon.cola.component.common.result.factory.ResultDtos;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/publish-tasks")
public class DdcPublishTaskController {

    private final DdcPublishTaskRepository publishTaskRepository;

    public DdcPublishTaskController(DdcPublishTaskRepository publishTaskRepository) {
        this.publishTaskRepository = publishTaskRepository;
    }

    @GetMapping
    public ResultDto<List<DdcPublishTaskEntity>> list() {
        return ResultDtos.success(publishTaskRepository.findAll());
    }

    @GetMapping("/{changeId}")
    public ResultDto<DdcPublishTaskEntity> detail(@PathVariable("changeId") String changeId) {
        return ResultDtos.success(publishTaskRepository.findByChangeId(changeId).orElse(null));
    }
}
