package top.egon.cola.platform.rbac3.admin.session.service;

import org.springframework.stereotype.Service;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.SessionVO;
import top.egon.cola.platform.rbac3.admin.session.repository.SessionManagementRepository;

import java.time.Instant;
import java.util.List;

/** 会话管理服务的默认实现。 Default session-management service. */
@Service
public class DefaultSessionManagementService implements SessionManagementService {

    /** 会话管理持久化端口。 Session-management persistence port. */
    private final SessionManagementRepository repository;

    /**
     * 创建默认会话管理服务。 Creates the default session-management service.
     *
     * @param repository 会话管理持久化端口；session-management persistence port
     */
    public DefaultSessionManagementService(SessionManagementRepository repository) {
        this.repository = repository;
    }

    /** {@inheritDoc} */
    @Override
    public List<SessionVO> findByUser(String tenantId, String userId) {
        return repository.findByUser(tenantId, userId);
    }

    /** {@inheritDoc} */
    @Override
    public boolean revoke(String tenantId, String sessionId, Instant now) {
        return repository.revoke(tenantId, sessionId, now);
    }

    /** {@inheritDoc} */
    @Override
    public int revokeAll(String tenantId, String userId, Instant now) {
        return repository.revokeAll(tenantId, userId, now);
    }
}
