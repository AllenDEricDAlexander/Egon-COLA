package top.egon.cola.component.tianshu.admin.model.query;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.groups.Default;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;

/** Immutable internal locator for one exact, tenant-scoped publication version. */
public record DdcAiVersionQuery(
        @NotNull(groups = {Default.class, EgonColaModelValidationGroups.Query.class})
        @Positive(groups = {Default.class, EgonColaModelValidationGroups.Query.class}) Long resourceId,
        @NotNull(groups = {Default.class, EgonColaModelValidationGroups.Query.class})
        @Positive(groups = {Default.class, EgonColaModelValidationGroups.Query.class}) Long versionNo) {
}
