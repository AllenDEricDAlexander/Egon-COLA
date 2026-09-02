package top.egon.cola.archetype.source.webopen.adapter;

import top.egon.cola.archetype.source.webopen.adapter.teaching.rpc.SchoolClassRpcProvider;
import top.egon.cola.archetype.source.webopen.adapter.user.rpc.UserRpcProvider;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.GradeService;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.PermissionService;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.RoleService;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.SchoolClassService;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.UserService;
import org.apache.dubbo.config.spring.ServiceBean;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OrganizationDubboProviderConfigurationTest {

    @Test
    void exportsFiveGeneratedTripleBeansWithStableContractCoordinates() {
        UserRpcProvider userProvider = new UserRpcProvider();
        SchoolClassRpcProvider teachingProvider = new SchoolClassRpcProvider();

        List<ServiceBean<?>> services = List.of(
                userProvider.userService(mock(UserService.class)),
                userProvider.roleService(mock(RoleService.class)),
                userProvider.permissionService(mock(PermissionService.class)),
                teachingProvider.gradeService(mock(GradeService.class)),
                teachingProvider.schoolClassService(mock(SchoolClassService.class)));

        assertThat(services).hasSize(5).allSatisfy(service -> {
            assertThat(service.getGroup()).isEqualTo("student-management-organization");
            assertThat(service.getVersion()).isEqualTo("1.0.0");
            assertThat(service.getRef()).isNotNull();
        });
    }
}
