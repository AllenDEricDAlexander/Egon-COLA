#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.client.organization;

import ${package}.domain.client.ExternalDependencyException;
import ${package}.domain.client.organization.OrganizationDirectoryPort;
import ${package}.domain.client.organization.OrganizationSchoolClass;
import ${package}.domain.client.organization.OrganizationUser;
import ${package}.facade.organization.v1.GetSchoolClassRequest;
import ${package}.facade.organization.v1.GetUserRequest;
import ${package}.facade.organization.v1.SchoolClass;
import ${package}.facade.organization.v1.SchoolClassService;
import ${package}.facade.organization.v1.User;
import ${package}.facade.organization.v1.UserService;
import java.util.List;
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
    private UserService userService;

    @DubboReference(
            group = "${symbol_dollar}{app.integrations.organization.group:student-management-organization}",
            version = "${symbol_dollar}{app.integrations.organization.version:1.0.0}",
            retries = 0,
            check = true)
    private SchoolClassService schoolClassService;

    public DubboOrganizationDirectoryClient() {
    }

    DubboOrganizationDirectoryClient(UserService userService, SchoolClassService schoolClassService) {
        this.userService = userService;
        this.schoolClassService = schoolClassService;
    }

    @Override
    public OrganizationUser getUser(Long userId) {
        try {
            User response = userService.getUser(GetUserRequest.newBuilder().setUserId(userId).build());
            if (response == null) {
                throw OrganizationClientFailureMapper.incompatible("getUser");
            }
            return new OrganizationUser(response.getId(), response.getName(), response.getStatus());
        } catch (ExternalDependencyException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw OrganizationClientFailureMapper.map(failure);
        }
    }

    @Override
    public OrganizationSchoolClass getSchoolClass(Long gradeId, Long schoolClassId) {
        try {
            SchoolClass response = schoolClassService.getSchoolClass(GetSchoolClassRequest.newBuilder()
                    .setGradeId(gradeId)
                    .setSchoolClassId(schoolClassId)
                    .build());
            if (response == null) {
                throw OrganizationClientFailureMapper.incompatible("getSchoolClass");
            }
            List<Long> userIds = response.getUserIdsList().stream().map(Long::valueOf).toList();
            return new OrganizationSchoolClass(
                    response.getId(), response.getName(), response.getGradeCode(),
                    response.getStatus(), userIds);
        } catch (ExternalDependencyException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw OrganizationClientFailureMapper.map(failure);
        }
    }
}
