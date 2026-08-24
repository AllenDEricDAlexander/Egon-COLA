package ${package}.adapter.user.rpc;

import ${package}.facade.organization.v1.PermissionService;
import ${package}.facade.organization.v1.RoleService;
import ${package}.facade.organization.v1.UserService;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Exposes the generated organization contracts through the configured Triple protocol. */
@Configuration(proxyBeanMethods = false)
public class UserRpcProvider {

    @Bean
    public ServiceBean<UserService> userService(UserService implementation) {
        return service(UserService.class, implementation);
    }

    @Bean
    public ServiceBean<RoleService> roleService(RoleService implementation) {
        return service(RoleService.class, implementation);
    }

    @Bean
    public ServiceBean<PermissionService> permissionService(PermissionService implementation) {
        return service(PermissionService.class, implementation);
    }

    private static <T> ServiceBean<T> service(Class<T> type, T implementation) {
        ServiceBean<T> bean = new ServiceBean<>(ApplicationModel.defaultModel().getDefaultModule());
        bean.setInterface(type);
        bean.setRef(implementation);
        bean.setGroup("student-management-organization");
        bean.setVersion("1.0.0");
        return bean;
    }
}
