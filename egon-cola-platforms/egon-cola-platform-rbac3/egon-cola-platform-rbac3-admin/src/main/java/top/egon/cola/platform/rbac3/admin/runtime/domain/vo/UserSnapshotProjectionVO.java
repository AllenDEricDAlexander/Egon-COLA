package top.egon.cola.platform.rbac3.admin.runtime.domain.vo;

import top.egon.cola.platform.rbac3.contract.authorization.UserAuthorizationSnapshot;

/**
 * Runtime publication containing metadata and the immutable user snapshot.
 */
public record UserSnapshotProjectionVO(
        RuntimeUserAuthorizationVO user,
        UserAuthorizationSnapshot snapshot) {
}
