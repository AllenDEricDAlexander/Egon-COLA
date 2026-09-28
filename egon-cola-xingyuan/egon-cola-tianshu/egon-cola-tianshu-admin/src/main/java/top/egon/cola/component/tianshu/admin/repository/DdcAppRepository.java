package top.egon.cola.component.tianshu.admin.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAppBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;

import java.util.List;
import java.util.Optional;

@Validated
public interface DdcAppRepository {
    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAppBO> findByBizCodeAndAppCode(@NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String bizCode,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String appCode);
    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAppBO> findFirstByAppCodeOrderByBizCodeAsc(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String appCode);
    @Validated(EgonColaModelValidationGroups.Query.class)
    boolean existsByAppCode(@NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String appCode);
    @Validated(EgonColaModelValidationGroups.Query.class)
    boolean existsByAppCodeAndIdNot(@NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 128, groups = EgonColaModelValidationGroups.Query.class) String appCode,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);
    @Validated(EgonColaModelValidationGroups.Query.class)
    boolean existsByBizCodeAndAppCode(@NotNull(groups = EgonColaModelValidationGroups.Query.class) String bizCode,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) String appCode);
    @Validated(EgonColaModelValidationGroups.Query.class)
    boolean existsByBizCode(@NotNull(groups = EgonColaModelValidationGroups.Query.class) String bizCode);
    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAppBO> findByBizCode(@NotNull(groups = EgonColaModelValidationGroups.Query.class) String bizCode);
    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAppBO> findAllByIdIn(List<Long> ids);
    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAppBO> findByAppCodeContainingIgnoreCaseOrAppNameContainingIgnoreCase(String appCode, String appName);
    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAppBO> findByBizCodeAndAppCodeContainingIgnoreCaseOrBizCodeAndAppNameContainingIgnoreCase(
            String bizCode, String appCode, String bizCode2, String appName);
    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAppBO> list(String bizCode, String keyword, Boolean enabled);
    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcPageResult<DdcAppBO> search(String bizCode, String namespaceCode, String env, String keyword,
            Boolean enabled, PageQuery pageQuery);
    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAppBO> findActiveById(@NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);
    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAppBO> findByPublicId(@NotNull(groups = EgonColaModelValidationGroups.Query.class) String publicId);
    @Validated(EgonColaModelValidationGroups.Insert.class)
    DdcAppBO insert(@Valid @NotNull(groups = EgonColaModelValidationGroups.Insert.class) DdcAppBO command);
    @Validated(EgonColaModelValidationGroups.Update.class)
    DdcAppBO update(@Valid @NotNull(groups = EgonColaModelValidationGroups.Update.class) DdcAppBO command);
    @Validated(EgonColaModelValidationGroups.Delete.class)
    boolean delete(@Valid @NotNull(groups = EgonColaModelValidationGroups.Delete.class) DdcAppBO command);
}
