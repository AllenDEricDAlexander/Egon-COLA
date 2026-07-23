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
    public OrganizationUser getUser(String userId) {
        rejectMissing(userId, "user");
        return new OrganizationUser(userId, "Local User " + userId, "ACTIVE");
    }

    @Override
    public OrganizationSchoolClass getSchoolClass(String schoolClassId) {
        rejectMissing(schoolClassId, "school class");
        return new OrganizationSchoolClass(
                schoolClassId,
                "Local Class " + schoolClassId,
                "LOCAL",
                "ACTIVE",
                List.of("local-user"));
    }

    private static void rejectMissing(String id, String resource) {
        if (id == null || id.isBlank() || id.startsWith("missing-")) {
            throw new ExternalDependencyException(
                    "organization",
                    ExternalDependencyFailure.NOT_FOUND,
                    "LOCAL_NOT_FOUND",
                    "local organization " + resource + " was not found",
                    null);
        }
    }
}
