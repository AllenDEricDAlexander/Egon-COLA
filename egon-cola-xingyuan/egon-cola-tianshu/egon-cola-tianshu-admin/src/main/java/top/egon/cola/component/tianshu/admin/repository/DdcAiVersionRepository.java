package top.egon.cola.component.tianshu.admin.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;

import java.util.Optional;

@Validated
public interface DdcAiVersionRepository {

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAiVersionBO> findActiveById(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAiVersionBO> findByResourceIdAndVersionNo(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long resourceId,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long versionNo);

    @Validated(EgonColaModelValidationGroups.Insert.class)
    DdcAiVersionBO insertVersion(
            @NotNull(groups = EgonColaModelValidationGroups.Insert.class) @Valid DdcAiVersionBO version);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcPageResult<DdcAiVersionBO> pageByResourceId(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long resourceId,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Valid PageQuery pageQuery);
}
