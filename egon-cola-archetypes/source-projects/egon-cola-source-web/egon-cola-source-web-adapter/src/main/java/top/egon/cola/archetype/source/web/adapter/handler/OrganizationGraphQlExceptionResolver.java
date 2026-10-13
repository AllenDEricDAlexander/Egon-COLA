package top.egon.cola.archetype.source.web.adapter.handler;

import top.egon.cola.component.common.core.exception.ExceptionLogger;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.web.common.exception.OrganizationApplicationException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.archetype.source.web.adapter.graphql.NativeGraphQlSecurityInterceptor;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.RuntimeUnavailableException;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.DefaultAuthorizationService.AuthorizationDeniedException;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Map;

@Component("organizationGraphQlExceptionResolver")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class OrganizationGraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable error, DataFetchingEnvironment environment) {
        if (error instanceof RuntimeUnavailableException unavailable) {
            return securityError(environment, unavailable.reasonCode(), "Tianquan-Jianshen authorization runtime unavailable", ErrorType.INTERNAL_ERROR);
        }
        if (error instanceof AuthenticationException) {
            return securityError(environment, "AUTHENTICATION_REQUIRED", "Tianquan-Shoubing authentication failed", ErrorType.UNAUTHORIZED);
        }
        if (error instanceof AccessDeniedException || error instanceof AuthorizationDeniedException) {
            IdpAuthorizationObservationContextBO marker = environment.getGraphQlContext().get(NativeGraphQlSecurityInterceptor.OBSERVATION_MARKER_KEY);
            if (marker == null || !marker.isConfirmed()) {
                return securityError(environment, "OAUTH_AUDIT_DELIVERY_UNAVAILABLE", "Tianquan-Jianshen authorization runtime unavailable", ErrorType.INTERNAL_ERROR);
            }
            String code = error instanceof AuthorizationDeniedException denied ? denied.reasonCode()
                    : "FIELD_ACCESS_DENIED".equals(error.getMessage()) ? "FIELD_ACCESS_DENIED" : "AUTHORIZATION_DENIED";
            return securityError(environment, code, "Tianquan-Jianshen authorization denied", ErrorType.FORBIDDEN);
        }
        if (error instanceof OrganizationApplicationException failure) {
            ExceptionLogger.log(log, failure);
            return error(environment, failure.code(), failure.getMessage());
        }
        ExceptionLogger.log(log, error);
        return error(environment, "ORG_INTERNAL_ERROR", "Internal server error");
    }

    private static GraphQLError securityError(DataFetchingEnvironment environment, String code, String message, ErrorType classification) {
        String trace = MDC.get("traceId");
        return GraphqlErrorBuilder.newError(environment).message(message).errorType(classification)
                .extensions(Map.of("code", code, "traceId", trace == null ? "unknown" : trace,
                        "timestamp", Instant.now().toString(), "fieldErrors", Map.of())).build();
    }

    private static GraphQLError error(DataFetchingEnvironment environment, String code, String message) {
        String traceId = OrganizationRequestContextHolder.current()
                .map(context -> context.traceId())
                .orElseGet(() -> MDC.get("traceId") == null ? "unknown" : MDC.get("traceId"));
        return GraphqlErrorBuilder.newError(environment)
                .message(message)
                .extensions(Map.of(
                        "code", code,
                        "traceId", traceId,
                        "timestamp", Instant.now().toString(),
                        "fieldErrors", Map.of()))
                .build();
    }
}
