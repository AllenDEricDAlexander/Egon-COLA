package top.egon.cola.component.gateway.admin.shared.controller;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 中文说明：{@code GatewayAdminWebMvcConfiguration} 是配置类，位于当前 Gateway 模块的相关包中，负责网关管理端WebMvc配置相关的职责与边界。
 * English summary: {@code GatewayAdminWebMvcConfiguration} is a gateway admin web mvc configuration configuration in the current Gateway module; it owns the gateway admin web mvc configuration-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Configuration(proxyBeanMethods = false)
public class GatewayAdminWebMvcConfiguration
        implements WebMvcConfigurer {

    /**
     * 中文说明：执行 addArgumentResolvers 操作；该方法是 {@code GatewayAdminWebMvcConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the add argument resolvers operation; this method is the invocation entry point on {@code GatewayAdminWebMvcConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminWebMvcConfiguration.addArgumentResolvers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param resolvers 参数 resolvers；parameter resolvers。
     */
    @Override
    public void addArgumentResolvers(
            List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new GatewayAdminActorArgumentResolver());
    }
}
