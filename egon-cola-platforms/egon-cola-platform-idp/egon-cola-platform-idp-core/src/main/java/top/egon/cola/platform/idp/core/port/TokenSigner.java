package top.egon.cola.platform.idp.core.port;

import top.egon.cola.platform.idp.core.token.AccessTokenClaims;
import top.egon.cola.platform.idp.core.token.RefreshTokenClaims;

public interface TokenSigner {

    String signAccess(AccessTokenClaims claims);

    String signRefresh(RefreshTokenClaims claims);

    RefreshTokenClaims verifyRefresh(String rawRefreshToken);
}
