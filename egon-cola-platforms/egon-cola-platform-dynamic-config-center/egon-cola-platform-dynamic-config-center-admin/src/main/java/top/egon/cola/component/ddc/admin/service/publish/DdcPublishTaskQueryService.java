package top.egon.cola.component.ddc.admin.service.publish;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.ddc.admin.model.dto.DdcPublishTaskQueryRequest;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.ddc.admin.support.DdcAdminPageSupport;

@Service
public class DdcPublishTaskQueryService {

    private final DdcPublishTaskRepository publishTaskRepository;

    public DdcPublishTaskQueryService(
            DdcPublishTaskRepository publishTaskRepository) {
        this.publishTaskRepository = publishTaskRepository;
    }

    public Page<DdcPublishTaskEntity> page(
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
                optional(query.getChangeId()),
                DdcAdminPageSupport.pageable(
                        pageQuery,
                        Sort.by(Sort.Direction.DESC, "createdAt", "id")
                )
        );
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
