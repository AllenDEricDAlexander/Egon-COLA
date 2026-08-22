package top.egon.cola.component.ddc.admin.security.registration;

/**
 * 校验 IdP PLATFORM SERVICE Token 并返回可持久化的 verified identity。
 *
 * <p>Verifies an IdP PLATFORM SERVICE token and returns an identity safe to persist.</p>
 */
@FunctionalInterface
public interface DdcRegistrationCredentialVerifier {

    /**
     * 校验 Token 签名、上下文、当前 Resource 状态以及请求绑定。
     *
     * <p>Validates the token signature, context, current Resource state, and exact request
     * binding.</p>
     *
     * @param registrationToken IdP 签发的原始 SERVICE Token；raw SERVICE token issued by IdP
     * @param bizCode 注册请求业务域；business domain in the registration request
     * @param appCode 注册请求应用；application in the registration request
     * @param env 注册请求环境；environment in the registration request
     * @param instanceId 注册请求实例标识；instance identifier in the registration request
     * @return 已验证且不含原始 Token 的身份；verified identity without the raw token
     * @throws DdcRegistrationAuthenticationException Token 缺失、无效、到期或绑定不一致时抛出
     *；if the token is missing, invalid, expired, or bound to a different request
     */
    VerifiedDdcRegistrationIdentity verify(
            String registrationToken,
            String bizCode,
            String appCode,
            String env,
            String instanceId
    );
}
