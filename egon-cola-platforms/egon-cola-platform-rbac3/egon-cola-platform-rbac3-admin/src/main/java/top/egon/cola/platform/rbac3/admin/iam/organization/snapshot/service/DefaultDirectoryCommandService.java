package top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.service;

import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.iam.user.repository.IdentityTenantMembershipDirectory;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.dto.DirectorySnapshotCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.vo.DirectorySyncVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.repository.DirectoryCommandRepository;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.dto.UserStatusCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.vo.UserDirectoryVO;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.dto.CreateTenantCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.dto.TenantStatusCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.vo.TenantVO;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

/** 目录命令服务的默认实现。 Default directory command service. */
@Service
@Primary
public class DefaultDirectoryCommandService implements DirectoryCommandService {
    private final DirectoryCommandRepository repository;
    private final EntityManager entityManager;
    private final IdentityTenantMembershipDirectory memberships;

    public DefaultDirectoryCommandService(
            DirectoryCommandRepository repository,
            EntityManager entityManager,
            IdentityTenantMembershipDirectory memberships) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.memberships = memberships;
    }
    @Override public DirectorySyncVO submit(String tenantId, DirectorySnapshotCommandDTO command) {
        return repository.submit(tenantId, command);
    }
    @Override public TenantVO createTenant(CreateTenantCommandDTO command, String actorId) {
        return repository.createTenant(command, actorId);
    }
    @Override public TenantVO changeTenantStatus(
            String tenantId, TenantStatusCommandDTO command, String actorId) {
        return repository.changeTenantStatus(tenantId, command, actorId);
    }
    @Override public UserDirectoryVO changeUserStatus(
            String tenantId, String userId, UserStatusCommandDTO command, String actorId) {
        if ("ACTIVE".equalsIgnoreCase(command.status().trim())) {
            UserPO user = entityManager.find(UserPO.class, Long.valueOf(userId));
            if (user == null || !Long.valueOf(tenantId).equals(user.getTenantId())) {
                throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
            }
            memberships.requireActive(tenantId, user.getIdentitySub());
        }
        return repository.changeUserStatus(tenantId, userId, command, actorId);
    }
}
