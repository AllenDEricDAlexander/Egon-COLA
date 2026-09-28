package top.egon.cola.component.tianshu.admin.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.bo.DdcBizBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;

import java.util.List;
import java.util.Optional;

@Validated
public interface DdcBizRepository {

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcBizBO> findActiveById(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(
                    groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcBizBO> findByPublicId(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Size(max = 64,
                    groups = EgonColaModelValidationGroups.Query.class) String publicId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcBizBO> findByBizCode(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Size(max = 128,
                    groups = EgonColaModelValidationGroups.Query.class) String bizCode);

    @Validated(EgonColaModelValidationGroups.Query.class)
    boolean existsByBizCode(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Size(max = 128,
                    groups = EgonColaModelValidationGroups.Query.class) String bizCode);

    @Validated(EgonColaModelValidationGroups.Query.class)
    boolean existsByBizCodeAndIdNot(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Size(max = 128,
                    groups = EgonColaModelValidationGroups.Query.class) String bizCode,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Positive(
                    groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcBizBO> list(String keyword, Boolean enabled);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcPageResult<DdcBizBO> page(String keyword, Boolean enabled, PageQuery pageQuery);

    @Validated(EgonColaModelValidationGroups.Insert.class)
    DdcBizBO insert(@Valid @NotNull(groups = EgonColaModelValidationGroups.Insert.class) DdcBizBO command);

    @Validated(EgonColaModelValidationGroups.Update.class)
    DdcBizBO update(@Valid @NotNull(groups = EgonColaModelValidationGroups.Update.class) DdcBizBO command);

    @Validated(EgonColaModelValidationGroups.Delete.class)
    boolean delete(@Valid @NotNull(groups = EgonColaModelValidationGroups.Delete.class) DdcBizBO command);
}
