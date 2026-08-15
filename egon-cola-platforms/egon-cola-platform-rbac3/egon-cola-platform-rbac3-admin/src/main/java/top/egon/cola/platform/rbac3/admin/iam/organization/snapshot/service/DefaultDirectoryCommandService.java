package top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.service;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.dto.DirectorySnapshotCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.vo.DirectorySyncVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.repository.DirectoryCommandRepository;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.dto.UserStatusCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.vo.UserDirectoryVO;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.dto.CreateTenantCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.dto.TenantStatusCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.vo.TenantVO;

/** 目录命令服务的默认实现。 Default directory command service. */
@Service
@Primary
public class DefaultDirectoryCommandService implements DirectoryCommandService {
    private final DirectoryCommandRepository repository;
    public DefaultDirectoryCommandService(DirectoryCommandRepository repository) {
        this.repository = repository;
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
        return repository.changeUserStatus(tenantId, userId, command, actorId);
    }
}
