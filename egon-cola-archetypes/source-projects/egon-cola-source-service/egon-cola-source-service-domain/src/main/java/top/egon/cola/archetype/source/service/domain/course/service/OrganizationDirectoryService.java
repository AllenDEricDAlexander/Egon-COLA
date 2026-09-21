package top.egon.cola.archetype.source.service.domain.course.service;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.service.domain.course.vos.OrganizationSchoolClassBO;
import top.egon.cola.archetype.source.service.domain.course.vos.OrganizationUserBO;

/** Domain-facing view of the organization directory; the transport sits behind Infrastructure. */
public interface OrganizationDirectoryService {

    OrganizationUserBO getUser(@NotNull Long userId);

    OrganizationSchoolClassBO getSchoolClass(@NotNull Long gradeId, @NotNull Long schoolClassId);
}
