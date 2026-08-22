package top.egon.cola.platform.idp.contract;

/**
 * SERVICE Access Token 的授权上下文。
 *
 * <p>Authorization context carried by a SERVICE Access Token.</p>
 */
public enum ServiceTokenContext {

    /** 租户业务上下文；tenant business context. */
    TENANT,

    /** 平台控制面上下文；platform control-plane context. */
    PLATFORM
}
