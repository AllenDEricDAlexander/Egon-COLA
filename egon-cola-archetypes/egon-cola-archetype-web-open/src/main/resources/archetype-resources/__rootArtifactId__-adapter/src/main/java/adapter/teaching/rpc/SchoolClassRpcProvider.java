package ${package}.adapter.teaching.rpc;

import ${package}.facade.organization.v1.GradeService;
import ${package}.facade.organization.v1.SchoolClassService;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Exposes the generated teaching contracts through the configured Triple protocol. */
@Configuration(proxyBeanMethods = false)
public class SchoolClassRpcProvider {

    @Bean
    public ServiceBean<GradeService> gradeService(GradeService implementation) {
        return service(GradeService.class, implementation);
    }

    @Bean
    public ServiceBean<SchoolClassService> schoolClassService(SchoolClassService implementation) {
        return service(SchoolClassService.class, implementation);
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
