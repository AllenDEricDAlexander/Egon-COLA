package top.egon.cola.platform.idp.admin.audit.service.impl;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
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
import java.util.ArrayList;
import java.util.List;

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
        Specification<IdentityAuditLogEntity> filters = (root, criteria, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.actorSub() != null) {
                predicates.add(builder.equal(root.get("actorSub"), query.actorSub()));
            }
            if (query.eventType() != null) {
                predicates.add(builder.equal(root.get("eventType"), query.eventType()));
            }
            if (query.result() != null) {
                predicates.add(builder.equal(root.get("result"), query.result()));
            }
            if (query.traceId() != null) {
                predicates.add(builder.equal(root.get("traceId"), query.traceId()));
            }
            if (query.from() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("occurredAt"), query.from()));
            }
            if (query.to() != null) {
                predicates.add(builder.lessThan(root.get("occurredAt"), query.to()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        Page<IdentityAuditLogEntity> result = audits.findAll(filters, PageRequest.of(
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
