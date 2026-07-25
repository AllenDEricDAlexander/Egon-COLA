package top.egon.cola.component.gateway.admin.interfaces.management;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import top.egon.cola.component.gateway.admin.domain.AdminActor;

import java.util.Set;
import java.util.stream.Collectors;

public final class GatewayAdminActorArgumentResolver
        implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AdminActor.class.equals(parameter.getParameterType());
    }

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
        String actorId = authentication instanceof JwtAuthenticationToken jwt
                ? jwt.getToken().getSubject()
                : authentication.getName();
        return new AdminActor(
                actorId,
                AdminActor.ActorType.USER,
                authorities(authentication, "CAP_"),
                authorities(authentication, "ROLE_")
        );
    }

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
