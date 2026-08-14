package top.egon.cola.component.gateway.admin.shared.controller;


import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 中文说明：{@code GatewayAdminActorArgumentResolver} 是类型，位于当前 Gateway 模块的相关包中，负责网关管理端ActorArgumentResolver相关的职责与边界。
 * English summary: {@code GatewayAdminActorArgumentResolver} is a type in the current Gateway module; it owns the gateway admin actor argument resolver-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayAdminActorArgumentResolver
        implements HandlerMethodArgumentResolver {

    /**
     * 中文说明：执行 supports参数 操作；该方法是 {@code GatewayAdminActorArgumentResolver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the supports parameter operation; this method is the invocation entry point on {@code GatewayAdminActorArgumentResolver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminActorArgumentResolver.supportsParameter(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param parameter 参数 参数；parameter parameter。
     * @return 返回 supports参数 的处理结果；returns the result of the operation.
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AdminActor.class.equals(parameter.getParameterType());
    }

    /**
     * 中文说明：执行 resolveArgument 操作；该方法是 {@code GatewayAdminActorArgumentResolver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resolve argument operation; this method is the invocation entry point on {@code GatewayAdminActorArgumentResolver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminActorArgumentResolver.resolveArgument(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param parameter 参数 参数；parameter parameter。
     * @param mavContainer 参数 mavContainer；parameter mav container。
     * @param webRequest 参数 web请求；parameter web request。
     * @param binderFactory 参数 binder工厂；parameter binder factory。
     * @return 返回 resolveArgument 的处理结果；returns the result of the operation.
     */
    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException(
                    "GATEWAY_ADMIN_AUTHENTICATION_REQUIRED"
            );
        }
        String actorId;
        if (authentication instanceof JwtAuthenticationToken jwt) {
            actorId = jwt.getToken().getSubject();
        } else if (authentication.getPrincipal()
                instanceof ServiceIdentityPrincipal service) {
            actorId = service.subject();
        } else {
            actorId = authentication.getName();
        }
        return new AdminActor(
                actorId,
                authentication.getPrincipal() instanceof ServiceIdentityPrincipal
                        ? top.egon.cola.component.gateway.admin.shared.domain.enums.AdminActorTypeEnum.SERVICE
                        : top.egon.cola.component.gateway.admin.shared.domain.enums.AdminActorTypeEnum.USER,
                authorities(authentication, "CAP_"),
                authorities(authentication, "ROLE_")
        );
    }

    /**
     * 中文说明：执行 authorities 操作；该方法是 {@code GatewayAdminActorArgumentResolver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authorities operation; this method is the invocation entry point on {@code GatewayAdminActorArgumentResolver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminActorArgumentResolver.authorities(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param authentication 参数 authentication；parameter authentication。
     * @param prefix 参数 prefix；parameter prefix。
     * @return 返回 authorities 的处理结果；returns the result of the operation.
     */
    private Set<String> authorities(
            Authentication authentication,
            String prefix) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(prefix))
                .map(authority -> authority.substring(prefix.length()))
                .collect(Collectors.toUnmodifiableSet());
    }
}
