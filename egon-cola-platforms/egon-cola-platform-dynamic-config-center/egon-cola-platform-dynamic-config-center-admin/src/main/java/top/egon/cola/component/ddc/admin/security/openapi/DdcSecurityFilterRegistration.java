package top.egon.cola.component.ddc.admin.security.openapi;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DdcSecurityFilterRegistration {

    @Bean
    public FilterRegistrationBean<DdcOpenApiHmacFilter>
            ddcOpenApiHmacFilterRegistration(
                    DdcOpenApiHmacFilter filter) {
        FilterRegistrationBean<DdcOpenApiHmacFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
