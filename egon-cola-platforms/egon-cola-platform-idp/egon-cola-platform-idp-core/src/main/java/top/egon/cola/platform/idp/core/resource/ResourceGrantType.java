package top.egon.cola.platform.idp.core.resource;

/**
 * Client 访问 Resource Server 的 OAuth 授权类型。
 *
 * <p>OAuth grant type authorizing a Client to access a Resource Server.</p>
 */
public enum ResourceGrantType {

    /**
     * 允许 Client 代表用户申请目标 Resource Token。
     *
     * <p>Allows the Client to request a target Resource Token on behalf of a user.</p>
     */
    USER_DELEGATION,

    /**
     * 允许 Confidential Client 以服务身份访问目标 Resource。
     *
     * <p>Allows a Confidential Client to access the target Resource as a service.</p>
     */
    CLIENT_CREDENTIALS
}
