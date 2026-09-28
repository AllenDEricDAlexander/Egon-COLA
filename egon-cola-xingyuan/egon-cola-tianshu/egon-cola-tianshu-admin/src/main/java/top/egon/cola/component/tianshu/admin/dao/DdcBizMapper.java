package top.egon.cola.component.tianshu.admin.dao;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.po.DdcBizPO;

import java.util.List;
import java.util.Collection;

@Mapper
@Validated
public interface DdcBizMapper extends EgonColaMapper<DdcBizPO> {

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcBizPO selectActiveById(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(
                    groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("id") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(
                    groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcBizPO> selectActiveByIds(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(
                    groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("ids") @NotEmpty(groups = EgonColaModelValidationGroups.Query.class)
            Collection<@NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(
                    groups = EgonColaModelValidationGroups.Query.class) Long> ids);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcBizPO selectActiveByPublicId(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(
                    groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("publicId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Size(max = 64,
                    groups = EgonColaModelValidationGroups.Query.class) String publicId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcBizPO selectActiveByBizCode(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(
                    groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("bizCode") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Size(max = 128,
                    groups = EgonColaModelValidationGroups.Query.class) String bizCode);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcBizPO> selectList(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(
                    groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("keyword") @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String keyword,
            @Param("enabled") Boolean enabled);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcBizPO> selectPage(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(
                    groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("keyword") @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String keyword,
            @Param("enabled") Boolean enabled,
            @Param("offset") @Min(value = 0, groups = EgonColaModelValidationGroups.Query.class) long offset,
            @Param("limit") @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class) int limit);

    @Validated(EgonColaModelValidationGroups.Query.class)
    long countList(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(
                    groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("keyword") @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String keyword,
            @Param("enabled") Boolean enabled);

    @Validated(EgonColaModelValidationGroups.Query.class)
    long countByBizCode(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(
                    groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("bizCode") @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Size(max = 128,
                    groups = EgonColaModelValidationGroups.Query.class) String bizCode,
            @Param("excludeId") @Positive(groups = EgonColaModelValidationGroups.Query.class) Long excludeId);
}
