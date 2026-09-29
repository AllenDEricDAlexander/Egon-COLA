package top.egon.cola.component.tianshu.admin.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionNodeBO;

import java.util.List;
import java.util.Optional;

@Validated
public interface DdcAiVersionNodeRepository {

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcAiVersionNodeBO> findActiveById(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcAiVersionNodeBO> findTreeByVersionId(
            @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long versionId);

    @Validated(EgonColaModelValidationGroups.Insert.class)
    List<DdcAiVersionNodeBO> insertAll(
            @NotNull(groups = EgonColaModelValidationGroups.Insert.class)
            @Valid List<@NotNull(groups = EgonColaModelValidationGroups.Insert.class) @Valid DdcAiVersionNodeBO> nodes);
}
