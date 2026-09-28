package top.egon.cola.component.tianshu.admin.service.publish;

import org.springframework.stereotype.Service;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishTaskBO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcPublishTaskQueryRequest;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishTaskRepository;

@Service
public class DdcPublishTaskQueryService {

    private final DdcPublishTaskRepository publishTaskRepository;

    public DdcPublishTaskQueryService(
            DdcPublishTaskRepository publishTaskRepository) {
        this.publishTaskRepository = publishTaskRepository;
    }

    public DdcPageResult<DdcPublishTaskBO> page(
            DdcPublishTaskQueryRequest request,
            PageQuery pageQuery) {
        DdcPublishTaskQueryRequest query = request == null
                ? new DdcPublishTaskQueryRequest()
                : request;
        return publishTaskRepository.search(
                optional(query.getBizCode()),
                optional(query.getEnv()),
                optional(query.getAppCode()),
                optional(query.getStatus()),
                optional(query.getChangeId()), pageQuery
        );
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
