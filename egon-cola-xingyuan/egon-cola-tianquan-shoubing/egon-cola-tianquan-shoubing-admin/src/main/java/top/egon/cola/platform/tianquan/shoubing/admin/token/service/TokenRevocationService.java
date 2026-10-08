package top.egon.cola.platform.tianquan.shoubing.admin.token.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.command.OAuthClientRevokeCommand;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.command.OAuthSubjectRevokeCommand;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.command.RevokeOAuthTokenCommand;

@Validated
public interface TokenRevocationService {

    void revoke(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid RevokeOAuthTokenCommand command);

    void revokeSubject(@NotNull @Valid OAuthSubjectRevokeCommand command);

    void revokeClient(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthClientRevokeCommand command);
}
