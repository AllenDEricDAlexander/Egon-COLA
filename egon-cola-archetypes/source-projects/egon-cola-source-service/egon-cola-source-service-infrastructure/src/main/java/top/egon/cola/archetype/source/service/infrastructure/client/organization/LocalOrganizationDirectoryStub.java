package top.egon.cola.archetype.source.service.infrastructure.client.organization;

import top.egon.cola.archetype.source.service.domain.client.ExternalDependencyException;
import top.egon.cola.archetype.source.service.domain.client.ExternalDependencyFailure;
import top.egon.cola.archetype.source.service.domain.client.organization.OrganizationDirectoryPort;
import top.egon.cola.archetype.source.service.domain.client.organization.OrganizationSchoolClass;
import top.egon.cola.archetype.source.service.domain.client.organization.OrganizationUser;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class LocalOrganizationDirectoryStub implements OrganizationDirectoryPort {

    @Override
    public OrganizationUser getUser(Long userId) {
        rejectMissing(userId, "user");
        return new OrganizationUser(userId, "Local User " + userId, "ACTIVE");
    }

    @Override
    public OrganizationSchoolClass getSchoolClass(Long gradeId, Long schoolClassId) {
        rejectMissing(gradeId, "grade");
        rejectMissing(schoolClassId, "school class");
        return new OrganizationSchoolClass(
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
