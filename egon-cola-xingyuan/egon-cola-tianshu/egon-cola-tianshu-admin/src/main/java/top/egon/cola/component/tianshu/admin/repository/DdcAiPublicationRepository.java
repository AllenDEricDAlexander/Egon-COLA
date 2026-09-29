package top.egon.cola.component.tianshu.admin.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiPublicationBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;

import java.util.Optional;

@Validated
public interface DdcAiPublicationRepository {

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAiPublicationBO> findActiveById(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAiPublicationBO> findByTenantRequestId(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Size(min = 16, max = 64, groups = EgonColaModelValidationGroups.Query.class)
            @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$", groups = EgonColaModelValidationGroups.Query.class)
            String requestId);

    @Validated(EgonColaModelValidationGroups.Insert.class)
    DdcAiPublicationBO insertPublication(
            @NotNull(groups = EgonColaModelValidationGroups.Insert.class) @Valid DdcAiPublicationBO publication);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcPageResult<DdcAiPublicationBO> pageByResourceId(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long resourceId,
            @Size(min = 16, max = 64, groups = EgonColaModelValidationGroups.Query.class)
            @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$", groups = EgonColaModelValidationGroups.Query.class)
            String requestId,
            @NotNull(groups = EgonColaModelValidationGroups.Query.class) @Valid PageQuery pageQuery);
}
