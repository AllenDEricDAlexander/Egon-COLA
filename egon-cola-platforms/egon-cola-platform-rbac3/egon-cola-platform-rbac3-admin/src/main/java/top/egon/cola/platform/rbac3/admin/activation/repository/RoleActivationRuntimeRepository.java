package top.egon.cola.platform.rbac3.admin.activation.repository;

import top.egon.cola.platform.rbac3.admin.activation.domain.vo.RuntimePublicationVO;

import java.time.Duration;

/**
 * Runtime publication port keyed by tenant and user.
 */
public interface RoleActivationRuntimeRepository {

    void createFence(String tenantId, String identitySub, String mutationId, Duration ttl);

    void publish(RuntimePublicationVO publication);
}
