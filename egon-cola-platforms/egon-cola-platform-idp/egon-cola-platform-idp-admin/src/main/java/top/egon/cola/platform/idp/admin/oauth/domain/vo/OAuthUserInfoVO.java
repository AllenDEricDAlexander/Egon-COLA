package top.egon.cola.platform.idp.admin.oauth.domain.vo;

import java.util.Set;

/**
 * OAuth UserInfo 端点返回的当前身份声明。
 *
 * <p>Current identity claims returned by the OAuth UserInfo endpoint.</p>
 */
public record OAuthUserInfoVO(
        String sub,
        String tid,
        Set<String> aud
) {
}
