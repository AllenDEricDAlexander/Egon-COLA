package top.egon.cola.platform.idp.contract;

/**
 * IdP Access Token 的主体类型。
 *
 * <p>Principal type carried by an IdP Access Token.</p>
 */
public enum PrincipalType {

    /**
     * 登录用户主体。
     *
     * <p>Authenticated user principal.</p>
     */
    USER,

    /**
     * Confidential Client 服务主体。
     *
     * <p>Confidential Client service principal.</p>
     */
    SERVICE
}
