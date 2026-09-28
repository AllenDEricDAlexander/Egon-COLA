package top.egon.cola.component.tianshu.admin.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;

import java.util.List;
import java.util.Optional;

@Validated
public interface DdcNamespaceRepository {
    @Validated(EgonColaModelValidationGroups.Query.class)
    boolean existsByBizCode(@NotNull(groups = EgonColaModelValidationGroups.Query.class) String bizCode);
    @Validated(EgonColaModelValidationGroups.Query.class)
    boolean existsByBizCodeAndNamespace(@NotNull(groups = EgonColaModelValidationGroups.Query.class) String bizCode,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) String namespace);
    @Validated(EgonColaModelValidationGroups.Query.class)
    boolean existsByBizCodeAndNamespaceAndIdNot(String bizCode, String namespace, Long id);
    @Validated(EgonColaModelValidationGroups.Query.class)
    boolean existsByBizCodeAndNamespaceCode(String bizCode, String namespaceCode);
    @Validated(EgonColaModelValidationGroups.Query.class)
    boolean existsByBizCodeAndNamespaceCodeAndIdNot(String bizCode, String namespaceCode, Long id);
    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcNamespaceBO> findByBizCodeAndNamespaceCode(String bizCode, String namespaceCode);
    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcNamespaceBO> findByPublicId(String publicId);
    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcNamespaceBO> findByBizCode(String bizCode);
    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcNamespaceBO> findByBizCodeAndNamespaceContainingIgnoreCaseOrBizCodeAndNamespaceCodeContainingIgnoreCase(
            String bizCode, String namespace, String bizCode2, String namespaceCode);
    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcPageResult<DdcNamespaceBO> search(String bizCode, String keyword, PageQuery pageQuery);
    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcNamespaceBO> findActiveById(@NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);
    @Validated(EgonColaModelValidationGroups.Insert.class)
    DdcNamespaceBO insert(@Valid @NotNull(groups = EgonColaModelValidationGroups.Insert.class) DdcNamespaceBO command);
    @Validated(EgonColaModelValidationGroups.Update.class)
    DdcNamespaceBO update(@Valid @NotNull(groups = EgonColaModelValidationGroups.Update.class) DdcNamespaceBO command);
    @Validated(EgonColaModelValidationGroups.Delete.class)
    boolean delete(@Valid @NotNull(groups = EgonColaModelValidationGroups.Delete.class) DdcNamespaceBO command);
}
