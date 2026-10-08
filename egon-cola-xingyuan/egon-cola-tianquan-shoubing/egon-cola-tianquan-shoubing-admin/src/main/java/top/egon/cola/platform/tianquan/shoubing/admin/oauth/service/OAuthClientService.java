package top.egon.cola.platform.tianquan.shoubing.admin.oauth.service;

import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.RotateClientSecretDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.UpdateOAuthClientDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.CreatedOAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.RotatedClientSecretVO;

import java.util.List;

/**
 * OAuth 客户端注册信息的管理用例入口。
 *
 * <p>Application entry point for OAuth client registration administration.</p>
 */
public interface OAuthClientService {

    List<OAuthClientVO> list(TrustedRealmBO trustedRealm);

    CreatedOAuthClientVO create(
            TrustedRealmBO trustedRealm,
            TrustedRealmBO platformRealm,
            CreateOAuthClientDTO command,
            String operatorSub);

    RotatedClientSecretVO rotateSecret(
            TrustedRealmBO trustedRealm,
            String clientId,
            RotateClientSecretDTO command,
            String operatorSub
    );

    OAuthClientVO update(TrustedRealmBO trustedRealm, String clientId, UpdateOAuthClientDTO command);

    OAuthClientVO putRedirectUri(TrustedRealmBO trustedRealm, String clientId, String redirectUri);

    OAuthClientVO deleteRedirectUri(TrustedRealmBO trustedRealm, String clientId, String redirectUri);

    OAuthClientVO putResourceUri(TrustedRealmBO trustedRealm, TrustedRealmBO platformRealm,
            String clientId, String resourceUri);

    OAuthClientVO deleteResourceUri(TrustedRealmBO trustedRealm, TrustedRealmBO platformRealm,
            String clientId, String resourceUri);
}
