package top.egon.cola.platform.idp.admin.audit.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.idp.admin.audit.domain.dto.IdentityAuditQueryDTO;
import top.egon.cola.platform.idp.admin.audit.domain.pojo.IdentityAuditLogEntity;
import top.egon.cola.platform.idp.admin.audit.domain.vo.IdentityAuditPageVO;
import top.egon.cola.platform.idp.admin.audit.domain.vo.IdentityAuditVO;
import top.egon.cola.platform.idp.admin.audit.repo.IdentityAuditLogRepository;
import top.egon.cola.platform.idp.admin.audit.service.IdentityAuditService;

import java.util.Objects;

@Service
public class IdentityAuditServiceImpl implements IdentityAuditService {

    private static final int MAXIMUM_PAGE_SIZE = 200;

    private final IdentityAuditLogRepository audits;

    public IdentityAuditServiceImpl(IdentityAuditLogRepository audits) {
        this.audits = Objects.requireNonNull(audits, "audits");
    }

    @Override
    @Transactional(readOnly = true)
    public IdentityAuditPageVO list(IdentityAuditQueryDTO query) {
        Objects.requireNonNull(query, "query");
        if (query.page() < 0
                || query.size() < 1
                || query.size() > MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException("invalid audit page request");
        }
        Page<IdentityAuditLogEntity> result = audits.findAll(PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.DESC, "occurredAt", "id")
        ));
        return new IdentityAuditPageVO(
                result.getContent().stream()
                        .map(IdentityAuditServiceImpl::view)
                        .toList(),
                query.page(),
                query.size(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private static IdentityAuditVO view(IdentityAuditLogEntity entity) {
        return new IdentityAuditVO(
                entity.getId(),
                entity.getEventType(),
                entity.getActorSub(),
                entity.getTargetSub(),
                entity.getResult(),
                entity.getReason(),
                entity.getOccurredAt()
        );
    }
}
