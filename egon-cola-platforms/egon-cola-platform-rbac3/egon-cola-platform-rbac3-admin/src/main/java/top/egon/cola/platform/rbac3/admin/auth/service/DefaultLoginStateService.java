package top.egon.cola.platform.rbac3.admin.auth.service;

import top.egon.cola.platform.rbac3.admin.activation.service.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.LoginStateVO;
import top.egon.cola.platform.rbac3.admin.auth.repository.LoginStateDataRepository;
import top.egon.cola.platform.rbac3.admin.auth.repository.LoginStateRepository;

import java.time.Instant;

/**
 * 聚合基础登录状态与角色激活候选数据。
 * Aggregates base login state with role-activation candidates.
 */
public final class DefaultLoginStateService implements LoginStateRepository {

    /** 登录状态基础数据仓储。 Base login-state data repository. */
    private final LoginStateDataRepository stateData;
    /** 角色激活候选服务。 Role-activation candidate service. */
    private final RoleActivationCandidateService candidates;

    /**
     * 创建登录状态聚合服务。
     * Creates the login-state aggregation service.
     *
     * @param stateData 登录状态基础数据仓储；base login-state repository
     * @param candidates 角色激活候选服务；role-activation candidate service
     */
    public DefaultLoginStateService(
            LoginStateDataRepository stateData,
            RoleActivationCandidateService candidates) {
        this.stateData = stateData;
        this.candidates = candidates;
    }

    /** {@inheritDoc} */
    @Override
    public LoginStateVO load(String tenantCode, String userId, Instant now) {
        LoginStateVO base = stateData.loadBase(tenantCode, userId, now);
        int candidateCount = candidates.candidates(base.tenantId(), userId, now)
                .applications().stream()
                .mapToInt(application -> application.candidates().size())
                .sum();
        return new LoginStateVO(
                base.tenantId(), base.authVersion(), base.policyVersion(), candidateCount);
    }
}
