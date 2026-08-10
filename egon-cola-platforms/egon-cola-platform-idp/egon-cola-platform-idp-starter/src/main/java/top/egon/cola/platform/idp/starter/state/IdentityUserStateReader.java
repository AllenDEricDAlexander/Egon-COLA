package top.egon.cola.platform.idp.starter.state;

import top.egon.cola.platform.idp.contract.IdentityUserState;

import java.util.Optional;

/**
 * 定义读取用户当前全局身份状态的端口，用于访问令牌即时失效判断。
 *
 * <p>Defines the port for reading current global user identity state used by immediate access-token
 * invalidation.</p>
 */
@FunctionalInterface
public interface IdentityUserStateReader {

    /**
     * 按统一用户主体标识读取当前状态。
     *
     * <p>Reads current state by unified user subject.</p>
     *
     * @param subject JWT 的统一用户主体标识；unified user subject from the JWT
     * @return 用户状态；不存在时返回空；user state, or empty when no projection exists
     */
    Optional<IdentityUserState> read(String subject);
}
