package top.egon.cola.archetype.source.web.adapter.graphql;

import jakarta.validation.Validator;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletRequestAttributes;
import reactor.core.publisher.Mono;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.RuntimeUnavailableException;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3AuthenticationToken;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpSecurityObservationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO;

/** Captures verified facts on the HTTP thread; callbacks receive values rather than a mutable SecurityContext. */
@Component("nativeGraphQlSecurityInterceptor")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class NativeGraphQlSecurityInterceptor implements WebGraphQlInterceptor {
    public static final String SNAPSHOT_KEY = NativeGraphQlSecurityInterceptor.class.getName() + ".snapshot";
    public static final String SERVLET_REQUEST_KEY = NativeGraphQlSecurityInterceptor.class.getName() + ".request";
    public static final String REQUEST_ATTRIBUTES_KEY = NativeGraphQlSecurityInterceptor.class.getName() + ".attributes";
    public static final String OBSERVATION_MARKER_KEY = NativeGraphQlSecurityInterceptor.class.getName() + ".auditMarker";

    @Qualifier("defaultValidator")
    private final Validator validator;

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof Rbac3AuthenticationToken authentication)
                || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Verified USER context is required");
        }
        var identity = authentication.getPrincipal().identity();
        if (!(authentication.getDetails() instanceof TokenAuditProofBO proof)) {
            throw new RuntimeUnavailableException("AUTHORIZATION_AUDIT_PROOF_MISSING", identity);
        }
        var business = OrganizationRequestContextHolder.current().orElseThrow(() -> new RuntimeUnavailableException("AUTHORIZATION_REQUEST_SCOPE_MISSING", identity));
        var scopes = authentication.getAuthorities().stream().map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("SCOPE_")).map(authority -> authority.substring("SCOPE_".length())).collect(Collectors.toUnmodifiableSet());
        var snapshot = new ValidationUtils(validator).validate(new NativeGraphQlSecuritySnapshotBO(authentication.context(), scopes, proof, TraceContext.capture(), business));
        if (!(org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            throw new RuntimeUnavailableException("AUTHORIZATION_REQUEST_SCOPE_MISSING", identity);
        }
        var servletRequest = attributes.getRequest();
        var marker = IdpSecurityObservationFilter.authorizationObservationContext(servletRequest);
        if (marker == null || !marker.isActive()) {
            throw new RuntimeUnavailableException("OAUTH_AUDIT_DELIVERY_UNAVAILABLE", identity);
        }
        request.configureExecutionInput((input, builder) -> builder.graphQLContext(context -> {
            context.put(SNAPSHOT_KEY, snapshot);
            context.put(SERVLET_REQUEST_KEY, servletRequest);
            context.put(REQUEST_ATTRIBUTES_KEY, attributes);
            context.put(OBSERVATION_MARKER_KEY, marker);
        }).build());
        return chain.next(request);
    }
}
