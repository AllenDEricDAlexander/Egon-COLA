package top.egon.cola.component.ddc.admin.controller.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.admin.model.dto.DdcPublishTaskQueryRequest;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.vo.DdcPublishResultVO;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.ddc.admin.service.publish.DdcPublishService;
import top.egon.cola.component.ddc.admin.service.publish.DdcPublishTaskQueryService;
import top.egon.cola.component.ddc.admin.support.DdcAdminPageSupport;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/publish-tasks")
public class DdcPublishTaskController {

    private final DdcPublishTaskRepository publishTaskRepository;

    private final DdcPublishService publishService;

    private final DdcPublishTaskQueryService publishTaskQueryService;

    public DdcPublishTaskController(
            DdcPublishTaskRepository publishTaskRepository,
            DdcPublishService publishService,
            DdcPublishTaskQueryService publishTaskQueryService) {
        this.publishTaskRepository = publishTaskRepository;
        this.publishService = publishService;
        this.publishTaskQueryService = publishTaskQueryService;
    }

    @GetMapping
    public ResultRecord<List<DdcPublishTaskEntity>> list() {
        return ResultRecord.success(publishTaskRepository.findAll());
    }

    @GetMapping("/page")
    public PageResultRecord<DdcPublishTaskEntity> page(
            DdcPublishTaskQueryRequest request,
            PageQuery pageQuery
    ) {
        return DdcAdminPageSupport.result(
                publishTaskQueryService.page(request, pageQuery));
    }

    @GetMapping("/{changeId}")
    public ResultRecord<DdcPublishTaskEntity> detail(@PathVariable("changeId") String changeId) {
        return ResultRecord.success(publishTaskRepository.findByChangeId(changeId).orElse(null));
    }

    @PostMapping("/{changeId}/retry")
    public ResultRecord<DdcPublishResultVO> retry(
            @PathVariable("changeId") String changeId) {
        return ResultRecord.success(publishService.retry(changeId));
    }
}
