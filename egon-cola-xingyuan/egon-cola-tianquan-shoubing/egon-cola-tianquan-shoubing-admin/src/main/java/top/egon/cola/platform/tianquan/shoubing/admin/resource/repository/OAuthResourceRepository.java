package top.egon.cola.platform.tianquan.shoubing.admin.resource.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthResourceBO;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Validated
public interface OAuthResourceRepository {

    List<OAuthResourceBO> listDirectory(
            @NotNull @Valid TrustedRealmBO platformRealm,
            @jakarta.validation.constraints.Min(1) @Max(10001) int limit);

    Optional<OAuthResourceBO> findByResourceServerId(
            @NotNull @Valid TrustedRealmBO platformRealm,
            @NotBlank @Size(max = 128) String resourceServerId);

    Optional<OAuthResourceBO> findByResourceServerIdForUpdate(
            @NotNull @Valid TrustedRealmBO platformRealm,
            @NotBlank @Size(max = 128) String resourceServerId);

    Optional<OAuthResourceBO> findByResourceUri(
            @NotNull @Valid TrustedRealmBO platformRealm,
            @NotNull URI resourceUri);

    Optional<OAuthResourceBO> findBySource(
            @NotNull @Valid TrustedRealmBO platformRealm,
            @NotBlank @Size(max = 128) String bizCode,
            @NotBlank @Size(max = 128) String appCode,
            @NotBlank @Size(max = 128) String environment);

    Optional<OAuthResourceBO> findByManagementClientPk(
            @NotNull @Valid TrustedRealmBO platformRealm,
            @Positive Long managementClientPk);

    List<OAuthResourceBO> findByBizCodeAndEnvironmentAndAppCodeIn(
            @NotNull @Valid TrustedRealmBO platformRealm,
            @NotBlank @Size(max = 128) String bizCode,
            @NotBlank @Size(max = 128) String environment,
            @NotNull @Size(min = 1, max = 100) Collection<@NotBlank @Size(max = 128) String> appCodes);

    OAuthResourceBO save(
            @NotNull @Valid TrustedRealmBO platformRealm,
            @NotNull @Valid OAuthResourceBO resource,
            @PositiveOrZero long expectedVersion);
}
