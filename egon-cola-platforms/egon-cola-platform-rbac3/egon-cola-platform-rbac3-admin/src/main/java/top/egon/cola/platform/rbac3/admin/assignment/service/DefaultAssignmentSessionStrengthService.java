package top.egon.cola.platform.rbac3.admin.assignment.service;

import org.springframework.stereotype.Service;
import top.egon.cola.platform.rbac3.admin.assignment.repository.AssignmentSessionStrengthRepository;

import java.time.Instant;
import java.util.Objects;

/**
 * 默认的分配会话强度服务，通过仓储端口读取会话状态。
 * Default assignment session-strength service backed by the repository port.
 */
@Service
public class DefaultAssignmentSessionStrengthService
        implements AssignmentSessionStrengthService {

    private final AssignmentSessionStrengthRepository repository;

    /**
     * 创建默认会话强度服务。
     * Creates the default session-strength service.
     *
     * @param repository 会话强度仓储；session-strength repository
     */
    public DefaultAssignmentSessionStrengthService(
            AssignmentSessionStrengthRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /** {@inheritDoc} */
    @Override
    public String authenticationStrength(
            String tenantId,
            String sessionId,
            Instant now) {
        return repository.authenticationStrength(tenantId, sessionId, now);
    }
}
