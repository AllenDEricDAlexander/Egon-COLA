package top.egon.cola.platform.rbac3.admin.auth.repository;

import top.egon.cola.platform.rbac3.admin.auth.domain.vo.LoginStateVO;

import java.time.Instant;

/**
 * 登录状态基础数据仓储，不负责跨领域候选角色聚合。
 * Base login-state repository without cross-domain activation aggregation.
 */
public interface LoginStateDataRepository {

    /**
     * 读取登录所需的租户与版本基础状态。
     * Loads tenant and version state required by authentication.
     *
     * @param tenantCode 租户编码；tenant code
     * @param userId 用户标识；user identifier
     * @param now 当前时间；current time
     * @return 候选角色数尚未聚合的登录状态；login state before candidate aggregation
     */
    LoginStateVO loadBase(String tenantCode, String userId, Instant now);
}
