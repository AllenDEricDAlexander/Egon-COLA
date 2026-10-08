package top.egon.cola.platform.tianquan.shoubing.admin.audit.dao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.po.OAuthAuditPO;

@Mapper
@Validated
public interface OAuthAuditMapper extends EgonColaMapper<OAuthAuditPO> {

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthAuditPO selectByEventId(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("eventId") @NotBlank(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 64, groups = EgonColaModelValidationGroups.Query.class) String eventId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthAuditPO selectByEventIdForUpdate(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("eventId") @NotBlank(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 64, groups = EgonColaModelValidationGroups.Query.class) String eventId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<OAuthAuditPO> selectByOccurredAt(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("fromInclusive") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            Instant fromInclusive,
            @Param("toExclusive") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            Instant toExclusive,
            @Param("afterOccurredAt") Instant afterOccurredAt,
            @Param("afterId") @PositiveOrZero(groups = EgonColaModelValidationGroups.Query.class) Long afterId,
            @Param("limit") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
            @Max(value = 500, groups = EgonColaModelValidationGroups.Query.class) Integer limit);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<OAuthAuditPO> selectExpired(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("cutoff") @NotNull(groups = EgonColaModelValidationGroups.Query.class) Instant cutoff,
            @Param("afterId") @PositiveOrZero(groups = EgonColaModelValidationGroups.Query.class) Long afterId,
            @Param("limit") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
            @Max(value = 500, groups = EgonColaModelValidationGroups.Query.class) Integer limit);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<OAuthAuditPO> selectIdentityPage(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("actorSub") @Size(max = 64, groups = EgonColaModelValidationGroups.Query.class) String actorSub,
            @Param("eventType") @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String eventType,
            @Param("result") @Pattern(regexp = "SUCCESS|FAILURE", groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 16, groups = EgonColaModelValidationGroups.Query.class) String result,
            @Param("traceId") @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String traceId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("offset") @PositiveOrZero(groups = EgonColaModelValidationGroups.Query.class) Long offset,
            @Param("limit") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
            @Max(value = 200, groups = EgonColaModelValidationGroups.Query.class) Integer limit);

    @Validated(EgonColaModelValidationGroups.Query.class)
    long countIdentityPage(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("actorSub") @Size(max = 64, groups = EgonColaModelValidationGroups.Query.class) String actorSub,
            @Param("eventType") @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String eventType,
            @Param("result") @Pattern(regexp = "SUCCESS|FAILURE", groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 16, groups = EgonColaModelValidationGroups.Query.class) String result,
            @Param("traceId") @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String traceId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Validated(EgonColaModelValidationGroups.Query.class)
    OAuthAuditPO selectActiveById(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("id") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<OAuthAuditPO> selectActiveByIds(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("ids") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(min = 1, max = 500, groups = EgonColaModelValidationGroups.Query.class)
            Collection<@NotNull(groups = EgonColaModelValidationGroups.Query.class)
                    @Positive(groups = EgonColaModelValidationGroups.Query.class) Long> ids);

    @Validated(EgonColaModelValidationGroups.Delete.class)
    int deleteVersionedById(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Delete.class)
            @Positive(groups = EgonColaModelValidationGroups.Delete.class) Long tenantId,
            @Param("po") @NotNull(groups = EgonColaModelValidationGroups.Delete.class)
            @Valid OAuthAuditPO entity,
            @Param("expectedVersion") @NotNull(groups = EgonColaModelValidationGroups.Delete.class)
            @PositiveOrZero(groups = EgonColaModelValidationGroups.Delete.class) Long expectedVersion);
}
