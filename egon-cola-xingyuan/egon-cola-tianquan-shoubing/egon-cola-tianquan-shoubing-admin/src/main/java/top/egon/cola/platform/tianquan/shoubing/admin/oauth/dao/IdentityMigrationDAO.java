package top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.po.OAuthAuditPO;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.po.IdentityCredentialPO;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.po.IdentityUserPO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.AuditRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.CredentialRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.MembershipRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.ClientRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.GrantRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.RedirectUriRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.ResourceRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.OutboxEventMetadataRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.TenantRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.UserRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.po.IdentityTenantMembershipPO;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.po.IdentityTenantPO;

/** Fixed SQL boundary for importing the four legacy directories and identity audit history. */
@Mapper
@Validated
public interface IdentityMigrationDAO {

    @InterceptorIgnore(tenantLine = "true")
    List<UserRowDTO> selectUsersAfter(@Param("lastSourceId") String lastSourceId, @Min(1) @Max(500) @Param("limit") int limit);

    @InterceptorIgnore(tenantLine = "true")
    List<CredentialRowDTO> selectCredentialsAfter(@Param("lastSourceId") String lastSourceId,
            @Min(1) @Max(500) @Param("limit") int limit);

    @InterceptorIgnore(tenantLine = "true")
    List<TenantRowDTO> selectTenantsAfter(@Param("lastSourceId") String lastSourceId, @Min(1) @Max(500) @Param("limit") int limit);

    @InterceptorIgnore(tenantLine = "true")
    List<MembershipRowDTO> selectMembershipsAfter(@Param("lastSourceId") String lastSourceId,
            @Min(1) @Max(500) @Param("limit") int limit);

    @InterceptorIgnore(tenantLine = "true")
    List<AuditRowDTO> selectAuditAfter(@Param("lastSourceId") String lastSourceId, @Min(1) @Max(500) @Param("limit") int limit);

    @InterceptorIgnore(tenantLine = "true")
    List<ClientRowDTO> selectClientsAfter(@Param("lastSourceId") String lastSourceId, @Min(1) @Max(500) @Param("limit") int limit);

    @InterceptorIgnore(tenantLine = "true")
    ClientRowDTO selectClientById(@Param("clientId") String clientId);

    @InterceptorIgnore(tenantLine = "true")
    List<RedirectUriRowDTO> selectRedirectUrisForClient(@Param("clientId") String clientId,
            @Min(1) @Max(17) @Param("limit") int limit);

    @InterceptorIgnore(tenantLine = "true")
    List<GrantRowDTO> selectGrantsForClient(@Param("clientId") String clientId, @Min(1) @Max(65) @Param("limit") int limit);

    @InterceptorIgnore(tenantLine = "true")
    List<ResourceRowDTO> selectResourcesAfter(@Param("lastSourceId") String lastSourceId, @Min(1) @Max(500) @Param("limit") int limit);

    @InterceptorIgnore(tenantLine = "true")
    ResourceRowDTO selectResourceById(@Param("resourceServerId") String resourceServerId);

    @InterceptorIgnore(tenantLine = "true")
    List<OutboxEventMetadataRowDTO> selectOutboxEventsAfter(@Param("lastSourceId") String lastSourceId,
            @Min(1) @Max(500) @Param("limit") int limit);

    @InterceptorIgnore(tenantLine = "true")
    long countLegacyClientSecrets();

    @InterceptorIgnore(tenantLine = "true")
    long countLegacySigningKeys();

    @InterceptorIgnore(tenantLine = "true")
    int restoreClientHistory(@Positive @Param("partitionId") long partitionId, @Param("clientId") String clientId,
            @PositiveOrZero @Param("version") long version, @NotNull @Param("createdAt") java.time.Instant createdAt,
            @NotNull @Param("updatedAt") java.time.Instant updatedAt);

    @InterceptorIgnore(tenantLine = "true")
    int restoreResourceHistory(@Positive @Param("partitionId") long partitionId, @Param("resourceServerId") String resourceServerId,
            @PositiveOrZero @Param("version") long version, @NotNull @Param("createdAt") java.time.Instant createdAt,
            @NotNull @Param("updatedAt") java.time.Instant updatedAt);

    @InterceptorIgnore(tenantLine = "true")
    int restoreGrantHistory(@Positive @Param("partitionId") long partitionId, @Positive @Param("clientPk") long clientPk,
            @Param("resourceServerId") String resourceServerId, @Param("grantType") String grantType,
            @PositiveOrZero @Param("version") long version, @NotNull @Param("createdAt") java.time.Instant createdAt,
            @NotNull @Param("updatedAt") java.time.Instant updatedAt);

    @InterceptorIgnore(tenantLine = "true")
    int sourceUserExists(@Param("identitySub") String identitySub);

    @InterceptorIgnore(tenantLine = "true")
    int sourceTenantExists(@Param("tenantId") String tenantId);

    @InterceptorIgnore(tenantLine = "true")
    String selectSourceTenantStatus(@Param("tenantId") String tenantId);

    @InterceptorIgnore(tenantLine = "true")
    IdentityUserPO selectUserByIdentitySub(@Positive @Param("partitionId") long partitionId,
            @Param("identitySub") String identitySub);

    @InterceptorIgnore(tenantLine = "true")
    IdentityCredentialPO selectCredentialByLegacyId(@Positive @Param("partitionId") long partitionId,
            @Param("legacyId") String legacyId);

    @InterceptorIgnore(tenantLine = "true")
    IdentityTenantPO selectTenantByExternalId(@Positive @Param("partitionId") long partitionId,
            @Param("identityTenantId") String identityTenantId);

    @InterceptorIgnore(tenantLine = "true")
    IdentityTenantMembershipPO selectMembershipById(@Positive @Param("partitionId") long partitionId,
            @Param("membershipId") String membershipId);

    @InterceptorIgnore(tenantLine = "true")
    OAuthAuditPO selectAuditByEventId(@Positive @Param("partitionId") long partitionId, @Param("eventId") String eventId);

    @InterceptorIgnore(tenantLine = "true")
    @Validated(EgonColaModelValidationGroups.Persisted.class)
    int insertUserMigration(@NotNull @Valid @Param("row") IdentityUserPO row);

    @InterceptorIgnore(tenantLine = "true")
    @Validated(EgonColaModelValidationGroups.Persisted.class)
    int insertCredentialMigration(@NotNull @Valid @Param("row") IdentityCredentialPO row);

    @InterceptorIgnore(tenantLine = "true")
    @Validated(EgonColaModelValidationGroups.Persisted.class)
    int insertTenantMigration(@NotNull @Valid @Param("row") IdentityTenantPO row);

    @InterceptorIgnore(tenantLine = "true")
    @Validated(EgonColaModelValidationGroups.Persisted.class)
    int insertMembershipMigration(@NotNull @Valid @Param("row") IdentityTenantMembershipPO row);

    @InterceptorIgnore(tenantLine = "true")
    @Validated(EgonColaModelValidationGroups.Persisted.class)
    int insertAuditMigration(@NotNull @Valid @Param("row") OAuthAuditPO row);
}
