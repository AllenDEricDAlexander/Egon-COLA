package top.egon.cola.archetype.source.web.adapter.graphql;

import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.RuntimeUnavailableException;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.DefaultAuthorizationService.AuthorizationDeniedException;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3AuthenticationToken;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpSecurityObservationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO;

/** Runs each framework callback in its own verified scope, including callbacks after the original dispatch ends. */
@Component("nativeGraphQlSecurityInstrumentation")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class NativeGraphQlSecurityInstrumentation extends SimplePerformantInstrumentation {
    @Qualifier("defaultValidator")
    private final Validator validator;

    @Override
    public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> delegate, InstrumentationFieldFetchParameters parameters, InstrumentationState state) {
        return environment -> {
            Object result = withinSnapshot(delegate, environment);
            if (!(result instanceof CompletionStage<?> stage)) { return result; }
            return stage.handle((value, failure) -> {
                try {
                    return withinSnapshot(callback -> {
                        if (failure == null) { return value; }
                        Throwable cause = failure;
                        while (cause instanceof CompletionException && cause.getCause() != null) { cause = cause.getCause(); }
                        if (cause instanceof Error error) { throw error; }
                        if (cause instanceof Exception exception) { throw exception; }
                        throw new IllegalStateException("GraphQL callback failed", cause);
                    }, environment);
                } catch (RuntimeException | Error failureInScope) { throw failureInScope; }
                catch (Exception checked) { throw new CompletionException(checked); }
            }).toCompletableFuture();
        };
    }

    /** Initial invocation and async completion share the same bounded server-owned scope. */
    private Object withinSnapshot(DataFetcher<?> action, DataFetchingEnvironment environment) throws Exception {
        NativeGraphQlSecuritySnapshotBO snapshot = environment.getGraphQlContext().get(NativeGraphQlSecurityInterceptor.SNAPSHOT_KEY);
        if (snapshot == null) { throw new AuthenticationCredentialsNotFoundException("Verified GraphQL context is required"); }
        new ValidationUtils(validator).validate(snapshot);
        var identity = snapshot.authorizationContext().identity();
        if (snapshot.authorizationContext().fenced()) { throw new RuntimeUnavailableException("AUTHORIZATION_CONTEXT_FENCED", identity); }
        HttpServletRequest request = environment.getGraphQlContext().get(NativeGraphQlSecurityInterceptor.SERVLET_REQUEST_KEY);
        IdpAuthorizationObservationContextBO marker = environment.getGraphQlContext().get(NativeGraphQlSecurityInterceptor.OBSERVATION_MARKER_KEY);
        if (request == null || marker == null || !marker.isActive()) { throw new RuntimeUnavailableException("OAUTH_AUDIT_DELIVERY_UNAVAILABLE", identity); }
        ServletRequestAttributes captured = environment.getGraphQlContext().get(NativeGraphQlSecurityInterceptor.REQUEST_ATTRIBUTES_KEY);
        var attributes = new ServletRequestAttributes(request, captured == null ? null : captured.getResponse());
        var previousSecurity = SecurityContextHolder.getContext();
        var previousBusiness = OrganizationRequestContextHolder.current();
        var previousAttributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        var authorities = snapshot.verifiedScopes().stream().map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope)).toList();
        var authentication = new Rbac3AuthenticationToken(new Rbac3UserDetails(identity, snapshot.authorizationContext().snapshot()), authorities);
        authentication.setDetails(snapshot.auditProof());
        var current = SecurityContextHolder.createEmptyContext(); current.setAuthentication(authentication);
        Throwable primaryFailure = null;
        try (var traceScope = snapshot.traceContext().open()) {
            try {
                SecurityContextHolder.setContext(current);
                OrganizationRequestContextHolder.clear();
                OrganizationRequestContextHolder.set(snapshot.businessContext());
                org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(attributes);
                try {
                    return action.get(environment);
                } catch (AccessDeniedException | AuthorizationDeniedException denied) {
                    if (!IdpSecurityObservationFilter.beforeAuthorizationDenied(request) || !marker.isConfirmed()) {
                        throw new RuntimeUnavailableException("OAUTH_AUDIT_DELIVERY_UNAVAILABLE", identity);
                    }
                    throw denied;
                }
            } catch (Exception | Error failure) {
                // This callback owns cleanup only; retain the original business/security failure unchanged.
                primaryFailure = failure;
                throw failure;
            } finally {
                try { attributes.requestCompleted(); }
                catch (RuntimeException cleanupFailure) {
                    if (primaryFailure == null) { throw cleanupFailure; }
                    primaryFailure.addSuppressed(cleanupFailure);
                } finally {
                    OrganizationRequestContextHolder.clear(); previousBusiness.ifPresent(OrganizationRequestContextHolder::set);
                    if (previousAttributes == null) { org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes(); }
                    else { org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(previousAttributes); }
                    SecurityContextHolder.setContext(previousSecurity);
                }
            }
        }
    }
}
