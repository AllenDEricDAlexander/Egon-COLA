package top.egon.cola.archetype.source.serviceopen.infrastructure.client.organization;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.serviceopen.domain.course.vos.OrganizationSchoolClassBO;
import top.egon.cola.archetype.source.serviceopen.domain.course.vos.OrganizationUserBO;

/**
 * Outbound organization directory dependency. It is an infrastructure client, not a domain port:
 * the domain only sees {@code OrganizationDirectoryService}.
 */
public interface OrganizationDirectoryClient {

    OrganizationUserBO getUser(@NotNull Long userId);

    OrganizationSchoolClassBO getSchoolClass(@NotNull Long gradeId, @NotNull Long schoolClassId);
}
