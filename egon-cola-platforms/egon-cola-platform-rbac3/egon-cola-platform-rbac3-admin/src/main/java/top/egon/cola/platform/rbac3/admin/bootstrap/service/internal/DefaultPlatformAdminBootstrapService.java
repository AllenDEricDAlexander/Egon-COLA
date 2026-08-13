package top.egon.cola.platform.rbac3.admin.bootstrap.service.internal;

import org.springframework.stereotype.Service;
import top.egon.cola.platform.rbac3.admin.bootstrap.repository.PlatformAdminBootstrapRepository;
import top.egon.cola.platform.rbac3.admin.bootstrap.service.PlatformAdminBootstrapService;

import java.util.Objects;

/**
 * 将平台管理员初始化入口委托给持久化边界。
 * Delegates the platform administrator bootstrap entry point to the persistence boundary.
 */
@Service
public final class DefaultPlatformAdminBootstrapService
        implements PlatformAdminBootstrapService {

    private final PlatformAdminBootstrapRepository repository;

    /**
     * 使用必需的初始化仓储创建服务。
     * Creates the service with its required bootstrap repository.
     *
     * @param repository 平台管理员初始化仓储；platform administrator bootstrap repository
     */
    public DefaultPlatformAdminBootstrapService(
            PlatformAdminBootstrapRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public void bootstrap(String tenantCode, String username, char[] password) {
        repository.bootstrap(tenantCode, username, password);
    }
}
