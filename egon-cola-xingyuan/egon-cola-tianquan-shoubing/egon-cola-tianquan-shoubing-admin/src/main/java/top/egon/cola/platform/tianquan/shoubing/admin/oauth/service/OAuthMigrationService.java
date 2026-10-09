package top.egon.cola.platform.tianquan.shoubing.admin.oauth.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.command.OAuthMigrationCommand;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.result.OAuthMigrationReportResult;

/** Operator-only data migration use case; it has no HTTP mapping or startup trigger. */
@Validated
public interface OAuthMigrationService {

    @NotNull
    OAuthMigrationReportResult importRealm(@NotNull @Valid OAuthMigrationCommand command);
}
