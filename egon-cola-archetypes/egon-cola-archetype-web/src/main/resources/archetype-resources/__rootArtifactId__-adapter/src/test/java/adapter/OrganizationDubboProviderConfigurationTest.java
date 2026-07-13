package ${package}.adapter;

import ${package}.adapter.teaching.rpc.SchoolClassRpcProvider;
import ${package}.adapter.user.rpc.UserRpcProvider;
import ${package}.facade.teaching.GradeFacade;
import ${package}.facade.teaching.SchoolClassFacade;
import ${package}.facade.user.PermissionFacade;
import ${package}.facade.user.RoleFacade;
import ${package}.facade.user.UserFacade;
import org.apache.dubbo.config.spring.ServiceBean;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OrganizationDubboProviderConfigurationTest {

    @Test
    void exportsFiveFacadeBeansWithStableContractCoordinates() {
        UserRpcProvider userProvider = new UserRpcProvider();
        SchoolClassRpcProvider teachingProvider = new SchoolClassRpcProvider();

        List<ServiceBean<?>> services = List.of(
                userProvider.userFacadeService(mock(UserFacade.class)),
                userProvider.roleFacadeService(mock(RoleFacade.class)),
                userProvider.permissionFacadeService(mock(PermissionFacade.class)),
                teachingProvider.gradeFacadeService(mock(GradeFacade.class)),
                teachingProvider.schoolClassFacadeService(mock(SchoolClassFacade.class)));

        assertThat(services).hasSize(5).allSatisfy(service -> {
            assertThat(service.getGroup()).isEqualTo("student-management-organization");
            assertThat(service.getVersion()).isEqualTo("1.0.0");
            assertThat(service.getRef()).isNotNull();
        });
    }
}
