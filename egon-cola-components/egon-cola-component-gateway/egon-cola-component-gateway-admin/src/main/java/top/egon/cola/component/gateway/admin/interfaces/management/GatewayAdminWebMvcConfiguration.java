package top.egon.cola.component.gateway.admin.interfaces.management;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class GatewayAdminWebMvcConfiguration
        implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(
            List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new GatewayAdminActorArgumentResolver());
    }
}
