package top.egon.cola.component.tianshu.admin.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigVersionBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;

import java.util.List;
import java.util.Optional;

@Validated
public interface DdcConfigVersionRepository {

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcConfigVersionBO> findByConfigIdOrderByVersionDesc(String configPublicId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcPageResult<DdcConfigVersionBO> findByConfigIdOrderByVersionDescIdDesc(String configPublicId,
            PageQuery pageQuery);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcConfigVersionBO> findByConfigIdAndVersion(String configPublicId, Long configRevision);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcConfigVersionBO> findByConfigItemIdAndRevision(@NotNull(groups = EgonColaModelValidationGroups.Query.class)
            Long configItemId, @NotNull(groups = EgonColaModelValidationGroups.Query.class) Long configRevision);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcConfigVersionBO> findByBizCodeAndEnvAndAppCodeAndResourceName(
            String bizCode, String env, String appCode, String resourceName);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcPageResult<DdcConfigVersionBO> findPublishedRuntimeVersions(String bizCode, String env, String appCode,
            String deleteType, PageQuery pageQuery);

    @Validated(EgonColaModelValidationGroups.Insert.class)
    DdcConfigVersionBO insert(
            @Valid @NotNull(groups = EgonColaModelValidationGroups.Insert.class) DdcConfigVersionBO command);
}
