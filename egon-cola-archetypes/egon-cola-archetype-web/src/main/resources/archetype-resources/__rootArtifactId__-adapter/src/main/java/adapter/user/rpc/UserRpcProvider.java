package ${package}.adapter.user.rpc;

import ${package}.facade.user.PermissionFacade;
import ${package}.facade.user.RoleFacade;
import ${package}.facade.user.UserFacade;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class UserRpcProvider {

    @Bean
    public ServiceBean<UserFacade> userFacadeService(UserFacade implementation) {
        return service(UserFacade.class, implementation);
    }

    @Bean
    public ServiceBean<RoleFacade> roleFacadeService(RoleFacade implementation) {
        return service(RoleFacade.class, implementation);
    }

    @Bean
    public ServiceBean<PermissionFacade> permissionFacadeService(PermissionFacade implementation) {
        return service(PermissionFacade.class, implementation);
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
