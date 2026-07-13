package ${package}.adapter.teaching.rpc;

import ${package}.facade.teaching.GradeFacade;
import ${package}.facade.teaching.SchoolClassFacade;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SchoolClassRpcProvider {

    @Bean
    public ServiceBean<GradeFacade> gradeFacadeService(GradeFacade implementation) {
        return service(GradeFacade.class, implementation);
    }

    @Bean
    public ServiceBean<SchoolClassFacade> schoolClassFacadeService(SchoolClassFacade implementation) {
        return service(SchoolClassFacade.class, implementation);
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
