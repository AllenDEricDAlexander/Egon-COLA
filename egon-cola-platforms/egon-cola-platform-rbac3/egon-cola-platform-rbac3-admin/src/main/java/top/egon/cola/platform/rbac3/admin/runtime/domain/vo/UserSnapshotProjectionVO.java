package top.egon.cola.platform.rbac3.admin.runtime.domain.vo;

import top.egon.cola.platform.rbac3.contract.authorization.GatewayBizAppScopeSnapshot;
import top.egon.cola.platform.rbac3.contract.authorization.UserAuthorizationSnapshot;

import java.util.Objects;

/**
 * Runtime publication containing metadata and the immutable user snapshot.
 */
public record UserSnapshotProjectionVO(
        RuntimeUserAuthorizationVO user,
        UserAuthorizationSnapshot snapshot,
        GatewayBizAppScopeSnapshot gatewayScope) {

    public UserSnapshotProjectionVO {
        user = Objects.requireNonNull(user, "user");
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        gatewayScope = Objects.requireNonNull(gatewayScope, "gatewayScope");
    }
}
