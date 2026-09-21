package top.egon.cola.archetype.source.service.infrastructure.client.organization.impl;

import top.egon.cola.archetype.source.service.common.enums.ExternalDependencyFailure;
import top.egon.cola.archetype.source.service.common.exception.ExternalDependencyException;
import top.egon.cola.archetype.source.service.domain.course.vos.OrganizationSchoolClassBO;
import top.egon.cola.archetype.source.service.domain.course.vos.OrganizationUserBO;
import top.egon.cola.archetype.source.service.infrastructure.client.organization.OrganizationDirectoryClient;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Broker-free profile-local implementation of the outbound organization directory client. */
@Component("organizationDirectoryClient")
@Profile("test")
@Slf4j
public class LocalOrganizationDirectoryClientImpl implements OrganizationDirectoryClient {

    @Override
    public OrganizationUserBO getUser(Long userId) {
        rejectMissing(userId, "user");
        return new OrganizationUserBO(userId, "Local User " + userId, "ACTIVE");
    }

    @Override
    public OrganizationSchoolClassBO getSchoolClass(Long gradeId, Long schoolClassId) {
        rejectMissing(gradeId, "grade");
        rejectMissing(schoolClassId, "school class");
        return new OrganizationSchoolClassBO(
                schoolClassId,
                "Local Class " + schoolClassId,
                "LOCAL",
                "ACTIVE",
                List.of(1001L));
    }

    private static void rejectMissing(Long id, String resource) {
        if (id == null || id <= 0) {
            throw new ExternalDependencyException(
                    "organization",
                    ExternalDependencyFailure.NOT_FOUND,
                    "LOCAL_NOT_FOUND",
                    "local organization " + resource + " was not found",
                    null);
        }
    }
}
