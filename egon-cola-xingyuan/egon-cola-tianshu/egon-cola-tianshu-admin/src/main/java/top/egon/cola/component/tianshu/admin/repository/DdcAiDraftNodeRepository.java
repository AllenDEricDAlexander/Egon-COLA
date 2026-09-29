package top.egon.cola.component.tianshu.admin.repository;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiDraftNodeBO;

import java.util.Optional;
import java.util.List;

@Validated
public interface DdcAiDraftNodeRepository {

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAiDraftNodeBO> findActiveById(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAiDraftNodeBO> findActiveByResourceId(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long resourceId);

    @Validated(EgonColaModelValidationGroups.Insert.class)
    DdcAiDraftNodeBO insert(
            @NotNull(groups = EgonColaModelValidationGroups.Insert.class)
            @jakarta.validation.Valid DdcAiDraftNodeBO node);

    @Validated(EgonColaModelValidationGroups.Delete.class)
    boolean delete(
            @NotNull(groups = EgonColaModelValidationGroups.Delete.class)
            @jakarta.validation.Valid DdcAiDraftNodeBO node);
}
