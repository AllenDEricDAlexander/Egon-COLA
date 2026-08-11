package top.egon.cola.component.ddc.admin.security.admission;

/**
 * 校验 IdP Resource Server 准入票据并返回可持久化的审计声明。
 *
 * <p>Verifies an IdP Resource Server admission ticket and returns audit claims safe to
 * persist.</p>
 */
@FunctionalInterface
public interface DdcAdmissionVerifier {

    /**
     * 校验票据签名、用途、当前 Resource 状态以及实例五元绑定。
     *
     * <p>Validates the ticket signature, purpose, current Resource state, and exact instance
     * binding.</p>
     *
     * @param ticket IdP 签发的原始短期票据；raw short-lived ticket issued by IdP
     * @param bizCode 注册请求业务域；business domain in the registration request
     * @param appCode 注册请求应用；application in the registration request
     * @param env 注册请求环境；environment in the registration request
     * @param instanceId 注册请求实例标识；instance identifier in the registration request
     * @return 已验证且不含原始票据的审计声明；verified audit claims without the raw ticket
     * @throws DdcAdmissionException 票据缺失、无效、到期或绑定不一致时抛出；if the ticket is
     * missing, invalid, expired, or bound to a different instance
     */
    DdcAdmissionClaims verify(
            String ticket,
            String bizCode,
            String appCode,
            String env,
            String instanceId
    );
}
