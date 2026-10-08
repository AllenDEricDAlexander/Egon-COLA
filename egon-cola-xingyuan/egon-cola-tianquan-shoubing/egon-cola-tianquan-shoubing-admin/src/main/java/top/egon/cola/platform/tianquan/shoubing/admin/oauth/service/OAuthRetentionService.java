package top.egon.cola.platform.tianquan.shoubing.admin.oauth.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;

@Validated
public interface OAuthRetentionService {

    int cleanExpired(@NotNull @Valid TrustedRealmBO trustedRealm);
}
