package top.egon.cola.component.tianshu.admin.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigItemBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Validated
public interface DdcConfigItemRepository {

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcConfigItemBO> findActiveById(@NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcConfigItemBO> findByPublicId(@NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(max = 64, groups = EgonColaModelValidationGroups.Query.class) String publicId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcConfigItemBO> findByBizCodeAndEnvAndAppCodeAndResourceName(String bizCode, String env,
            String appCode, String resourceName);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcConfigItemBO> findForPublishByBizCodeAndEnvAndAppCodeAndResourceName(String bizCode, String env,
            String appCode, String resourceName);

    @Validated(EgonColaModelValidationGroups.Query.class)
    boolean existsByEnv(String env);

    @Validated(EgonColaModelValidationGroups.Query.class)
    boolean existsByBizCodeAndAppCode(String bizCode, String appCode);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcConfigItemBO> findByBizCodeAndEnvAndAppCode(String bizCode, String env, String appCode);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcConfigItemBO> findByBizCodeAndEnvAndAppCodeAndDeletedFalse(String bizCode, String env, String appCode);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcConfigItemBO> search(String bizCode, String namespaceCode, String env, String appCode,
            String resourceName, boolean includeDeleted);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcPageResult<DdcConfigItemBO> search(String bizCode, String namespaceCode, String env, String appCode,
            String resourceName, boolean includeDeleted, PageQuery pageQuery);

    @Validated(EgonColaModelValidationGroups.Insert.class)
    DdcConfigItemBO insert(@Valid @NotNull(groups = EgonColaModelValidationGroups.Insert.class) DdcConfigItemBO command);

    @Validated(EgonColaModelValidationGroups.Update.class)
    DdcConfigItemBO update(@Valid @NotNull(groups = EgonColaModelValidationGroups.Update.class) DdcConfigItemBO command);

    @Validated(EgonColaModelValidationGroups.Update.class)
    int advancePublishedVersion(String publicId, Long expectedPublishedVersion, Long targetVersion,
            LocalDateTime updatedAt);
}
