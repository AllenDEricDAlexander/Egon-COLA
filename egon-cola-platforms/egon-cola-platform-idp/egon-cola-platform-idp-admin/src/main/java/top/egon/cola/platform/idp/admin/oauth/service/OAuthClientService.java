package top.egon.cola.platform.idp.admin.oauth.service;

import top.egon.cola.platform.idp.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.UpdateOAuthClientDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthClientVO;

import java.util.List;

/**
 * OAuth 客户端注册信息的管理用例入口。
 *
 * <p>Application entry point for OAuth client registration administration.</p>
 */
public interface OAuthClientService {

    List<OAuthClientVO> list();

    OAuthClientVO create(CreateOAuthClientDTO command);

    OAuthClientVO update(String clientId, UpdateOAuthClientDTO command);

    OAuthClientVO putRedirectUri(String clientId, String redirectUri);

    OAuthClientVO deleteRedirectUri(String clientId, String redirectUri);

    OAuthClientVO putAudience(String clientId, String audience);

    OAuthClientVO deleteAudience(String clientId, String audience);
}
