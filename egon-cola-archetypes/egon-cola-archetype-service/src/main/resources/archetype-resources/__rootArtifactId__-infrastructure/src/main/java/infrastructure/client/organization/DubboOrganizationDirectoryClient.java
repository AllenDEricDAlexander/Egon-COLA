#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.client.organization;

import ${organizationFacadePackage}.facade.dto.teaching.SchoolClassDetailDTO;
import ${organizationFacadePackage}.facade.dto.user.UserDetailDTO;
import ${organizationFacadePackage}.facade.teaching.SchoolClassFacade;
import ${organizationFacadePackage}.facade.user.UserFacade;
import ${package}.domain.client.ExternalDependencyException;
import ${package}.domain.client.organization.OrganizationDirectoryPort;
import ${package}.domain.client.organization.OrganizationSchoolClass;
import ${package}.domain.client.organization.OrganizationUser;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "prod"})
public class DubboOrganizationDirectoryClient implements OrganizationDirectoryPort {

    @DubboReference(
            group = "${symbol_dollar}{app.integrations.organization.group:student-management-organization}",
            version = "${symbol_dollar}{app.integrations.organization.version:1.0.0}",
            retries = 0,
            check = true)
    private UserFacade userFacade;

    @DubboReference(
            group = "${symbol_dollar}{app.integrations.organization.group:student-management-organization}",
            version = "${symbol_dollar}{app.integrations.organization.version:1.0.0}",
            retries = 0,
            check = true)
    private SchoolClassFacade schoolClassFacade;

    public DubboOrganizationDirectoryClient() {
    }

    DubboOrganizationDirectoryClient(
            UserFacade userFacade,
            SchoolClassFacade schoolClassFacade) {
        this.userFacade = userFacade;
        this.schoolClassFacade = schoolClassFacade;
    }

    @Override
    public OrganizationUser getUser(String userId) {
        try {
            UserDetailDTO response = userFacade.getUser(userId);
            if (response == null) {
                throw OrganizationClientFailureMapper.incompatible("getUser");
            }
            return new OrganizationUser(response.id(), response.name(), response.status());
        } catch (ExternalDependencyException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw OrganizationClientFailureMapper.map(failure);
        }
    }

    @Override
    public OrganizationSchoolClass getSchoolClass(String schoolClassId) {
        try {
            SchoolClassDetailDTO response = schoolClassFacade.getSchoolClass(schoolClassId);
            if (response == null) {
                throw OrganizationClientFailureMapper.incompatible("getSchoolClass");
            }
            return new OrganizationSchoolClass(
                    response.id(),
                    response.name(),
                    response.gradeCode(),
                    response.status(),
                    response.userIds());
        } catch (ExternalDependencyException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw OrganizationClientFailureMapper.map(failure);
        }
    }
}
