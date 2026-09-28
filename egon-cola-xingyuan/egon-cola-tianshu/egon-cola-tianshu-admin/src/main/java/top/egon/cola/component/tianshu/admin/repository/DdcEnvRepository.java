package top.egon.cola.component.tianshu.admin.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.bo.DdcEnvBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;

import java.util.List;
import java.util.Optional;

@Validated
public interface DdcEnvRepository {
    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcEnvBO> findByEnvCode(@NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 32, groups = EgonColaModelValidationGroups.Query.class) String envCode);
    @Validated(EgonColaModelValidationGroups.Query.class)
    boolean existsByEnvCode(@NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 32, groups = EgonColaModelValidationGroups.Query.class) String envCode);
    @Validated(EgonColaModelValidationGroups.Query.class)
    boolean existsByEnvCodeAndIdNot(@NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 32, groups = EgonColaModelValidationGroups.Query.class) String envCode,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);
    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcEnvBO> findAllByOrderBySortOrderAsc();
    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcEnvBO> findByEnvCodeContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) String envCode,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) String description);
    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcPageResult<DdcEnvBO> search(String bizCode, String namespaceCode, String keyword, PageQuery pageQuery);
    @Validated(EgonColaModelValidationGroups.Insert.class)
    DdcEnvBO insert(@Valid @NotNull(groups = EgonColaModelValidationGroups.Insert.class) DdcEnvBO command);
    @Validated(EgonColaModelValidationGroups.Update.class)
    DdcEnvBO update(@Valid @NotNull(groups = EgonColaModelValidationGroups.Update.class) DdcEnvBO command);
    @Validated(EgonColaModelValidationGroups.Delete.class)
    boolean delete(@Valid @NotNull(groups = EgonColaModelValidationGroups.Delete.class) DdcEnvBO command);
}
