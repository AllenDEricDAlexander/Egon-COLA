package top.egon.cola.component.common.trace.autoconfigure;

import io.micrometer.context.ContextRegistry;
import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.WebFilter;

@AutoConfiguration
@EnableConfigurationProperties(TraceProperties.class)
@ConditionalOnProperty(prefix = TraceProperties.PREFIX, name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class TraceAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({Filter.class, OncePerRequestFilter.class})
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = TraceProperties.PREFIX + ".servlet",
            name = "enabled", havingValue = "true", matchIfMissing = true)
    static class ServletTraceConfiguration {

        @Bean
        @ConditionalOnMissingBean
        TraceServletFilter traceServletFilter(TraceProperties properties) {
            return new TraceServletFilter(properties);
        }

        @Bean
        @ConditionalOnMissingBean(name = "egonTraceServletFilterRegistration")
        FilterRegistrationBean<TraceServletFilter> egonTraceServletFilterRegistration(
                TraceServletFilter filter,
                TraceProperties properties) {
            FilterRegistrationBean<TraceServletFilter> registration =
                    new FilterRegistrationBean<>(filter);
            registration.setOrder(properties.getServlet().getOrder());
            registration.setName("egonTraceServletFilter");
            return registration;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(WebFilter.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnProperty(prefix = TraceProperties.PREFIX + ".webflux",
            name = "enabled", havingValue = "true", matchIfMissing = true)
    static class WebFluxTraceConfiguration {

        @Bean
        @ConditionalOnMissingBean
        TraceWebFilter traceWebFilter(TraceProperties properties) {
            return new TraceWebFilter(properties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({RestClient.class, RestClientCustomizer.class})
    @ConditionalOnProperty(prefix = TraceProperties.PREFIX + ".rest-client",
            name = "enabled", havingValue = "true", matchIfMissing = true)
    static class RestClientTraceConfiguration {

        @Bean
        @ConditionalOnMissingBean(TraceRestClientCustomizer.class)
        TraceRestClientCustomizer traceRestClientCustomizer(
                TraceProperties properties) {
            return new TraceRestClientCustomizer(properties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({WebClient.class, WebClientCustomizer.class})
    @ConditionalOnProperty(prefix = TraceProperties.PREFIX + ".web-client",
            name = "enabled", havingValue = "true", matchIfMissing = true)
    static class WebClientTraceConfiguration {

        @Bean
        @ConditionalOnMissingBean(TraceWebClientCustomizer.class)
        TraceWebClientCustomizer traceWebClientCustomizer(
                TraceProperties properties) {
            return new TraceWebClientCustomizer(properties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ContextRegistry.class)
    @ConditionalOnProperty(prefix = TraceProperties.PREFIX + ".reactor",
            name = "automatic-context-propagation", havingValue = "true",
            matchIfMissing = true)
    static class ReactorContextPropagationConfiguration {

        @Bean(destroyMethod = "close")
        @ConditionalOnMissingBean
        TraceThreadLocalAccessor traceThreadLocalAccessor() {
            return new TraceThreadLocalAccessor();
        }
    }
}
