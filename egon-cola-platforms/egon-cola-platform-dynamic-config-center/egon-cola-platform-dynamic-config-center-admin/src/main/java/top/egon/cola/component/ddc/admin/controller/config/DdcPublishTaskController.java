package top.egon.cola.component.ddc.admin.controller.config;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.gateway.openapi.annotation.EgonGatewayPolicy;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/publish-tasks")
@Tag(name = "ddc-admin-ddc-publish-task-controller", description = "DdcPublishTaskController 管理接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "ddc-admin",
        entityDomainName = "Dynamic Config Center 管理实体域",
        interfaceGroupCode = "ddc"
)
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

    @Operation(operationId = "ddc.ddcPublishTaskController.list")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping
    public ResultRecord<List<DdcPublishTaskEntity>> list() {
        return ResultRecord.success(publishTaskRepository.findAll());
    }

    @Operation(operationId = "ddc.ddcPublishTaskController.page")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/page")
    public PageResultRecord<DdcPublishTaskEntity> page(
            DdcPublishTaskQueryRequest request,
            PageQuery pageQuery
    ) {
        return DdcAdminPageSupport.result(
                publishTaskQueryService.page(request, pageQuery));
    }

    @Operation(operationId = "ddc.ddcPublishTaskController.detail")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/{changeId}")
    public ResultRecord<DdcPublishTaskEntity> detail(@PathVariable("changeId") String changeId) {
        return ResultRecord.success(publishTaskRepository.findByChangeId(changeId).orElse(null));
    }

    @Operation(operationId = "ddc.ddcPublishTaskController.retry")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PostMapping("/{changeId}/retry")
    public ResultRecord<DdcPublishResultVO> retry(
            @PathVariable("changeId") String changeId) {
        return ResultRecord.success(publishService.retry(changeId));
    }
}
