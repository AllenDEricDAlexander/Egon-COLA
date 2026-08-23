#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.client.organization;

import ${package}.domain.client.ExternalDependencyException;
import ${package}.domain.client.ExternalDependencyFailure;
import ${package}.domain.client.organization.OrganizationDirectoryPort;
import ${package}.domain.client.organization.OrganizationSchoolClass;
import ${package}.domain.client.organization.OrganizationUser;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class LocalOrganizationDirectoryStub implements OrganizationDirectoryPort {

    @Override
    public OrganizationUser getUser(long userId) {
        rejectMissing(userId, "user");
        return new OrganizationUser(userId, "Local User " + userId, "ACTIVE");
    }

    @Override
    public OrganizationSchoolClass getSchoolClass(long gradeId, long schoolClassId) {
        rejectMissing(gradeId, "grade");
        rejectMissing(schoolClassId, "school class");
        return new OrganizationSchoolClass(
                schoolClassId,
                "Local Class " + schoolClassId,
                "LOCAL",
                "ACTIVE",
                List.of(2001L));
    }

    private static void rejectMissing(long id, String resource) {
        if (id <= 0 || id == 404L) {
            throw new ExternalDependencyException(
                    "organization",
                    ExternalDependencyFailure.NOT_FOUND,
                    "LOCAL_NOT_FOUND",
                    "local organization " + resource + " was not found",
                    null);
        }
    }
}
