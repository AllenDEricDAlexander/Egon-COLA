package top.egon.cola.component.tianshu.admin.repository;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiScopeDTO;

import java.util.Optional;

@Validated
public interface DdcAiResourceRepository {

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAiResourceBO> findActiveById(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAiResourceBO> findIncludingDeletedById(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAiResourceBO> findActiveForShareById(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAiResourceBO> findActiveForUpdateById(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated({Default.class, EgonColaModelValidationGroups.Query.class})
    void lockScopeParentsForShare(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Valid DdcAiScopeDTO scope);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAiResourceBO> findByBusinessKey(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String bizCode,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long namespaceId,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 32, groups = EgonColaModelValidationGroups.Query.class) String envCode,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String appCode,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) DdcAiKindEnum kind,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String resourceKey);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcPageResult<DdcAiResourceBO> search(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String bizCode,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long namespaceId,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 32, groups = EgonColaModelValidationGroups.Query.class) String envCode,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String appCode,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) DdcAiKindEnum kind,
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String resourceKeyPrefix,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Valid PageQuery pageQuery);

    @Validated(EgonColaModelValidationGroups.Insert.class)
    DdcAiResourceBO insert(
            @NotNull(groups = EgonColaModelValidationGroups.Insert.class)
            @Valid DdcAiResourceBO resource);

    @Validated(EgonColaModelValidationGroups.Update.class)
    Optional<DdcAiResourceBO> update(
            @NotNull(groups = EgonColaModelValidationGroups.Update.class)
            @Valid DdcAiResourceBO resource);

    @Validated(EgonColaModelValidationGroups.Delete.class)
    boolean delete(
            @NotNull(groups = EgonColaModelValidationGroups.Delete.class)
            @Valid DdcAiResourceBO resource);
}
