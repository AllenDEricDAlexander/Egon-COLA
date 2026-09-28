package top.egon.cola.component.tianshu.admin.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceEnvAppBindingBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Validated
public interface DdcNamespaceEnvAppBindingRepository {
    @Validated(EgonColaModelValidationGroups.Query.class)
    boolean existsByNamespaceId(@NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long namespaceId);
    @Validated(EgonColaModelValidationGroups.Query.class)
    boolean existsByAppId(@NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long appId);
    @Validated(EgonColaModelValidationGroups.Query.class)
    boolean existsByEnvCode(@NotNull(groups = EgonColaModelValidationGroups.Query.class) String envCode);
    @Validated(EgonColaModelValidationGroups.Query.class)
    boolean existsByNamespaceIdAndEnvCodeAndAppId(Long namespaceId, String envCode, Long appId);
    @Validated(EgonColaModelValidationGroups.Query.class)
    boolean existsByNamespaceIdAndEnvCodeAndAppIdAndIdNot(Long namespaceId, String envCode, Long appId, Long id);
    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcNamespaceEnvAppBindingBO> findByNamespaceIdAndEnvCodeAndEnabledTrue(Long namespaceId, String envCode);
    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcNamespaceEnvAppBindingBO> findByNamespaceIdAndEnabledTrue(Long namespaceId);
    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcNamespaceEnvAppBindingBO> findAll(String bizCode, String namespaceCode, String envCode, String appCode);
    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcPageResult<DdcNamespaceEnvAppBindingBO> search(String bizCode, String namespaceCode, String envCode,
            String appCode, PageQuery pageQuery);
    @Validated(EgonColaModelValidationGroups.Query.class)
    List<String> findVisibleNamespaceCodes(String bizCode, String envCode, String appCode);
    @Validated(EgonColaModelValidationGroups.Query.class)
    List<Map<String, Object>> findVisiblePhysicalScopes(String bizCode, String namespaceCode);
    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcNamespaceEnvAppBindingBO> findByPublicId(String publicId);
    @Validated(EgonColaModelValidationGroups.Insert.class)
    DdcNamespaceEnvAppBindingBO insert(
            @Valid @NotNull(groups = EgonColaModelValidationGroups.Insert.class) DdcNamespaceEnvAppBindingBO command);
    @Validated(EgonColaModelValidationGroups.Update.class)
    DdcNamespaceEnvAppBindingBO update(
            @Valid @NotNull(groups = EgonColaModelValidationGroups.Update.class) DdcNamespaceEnvAppBindingBO command);
    @Validated(EgonColaModelValidationGroups.Delete.class)
    boolean delete(
            @Valid @NotNull(groups = EgonColaModelValidationGroups.Delete.class) DdcNamespaceEnvAppBindingBO command);
}
