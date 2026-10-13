package top.egon.cola.archetype.source.light.adapter.handler;

import top.egon.cola.component.common.core.exception.ExceptionLogger;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.light.adapter.graphql.NativeGraphQlSecurityInterceptor;
import top.egon.cola.archetype.source.light.common.exception.TeachingUseCaseException;
import top.egon.cola.archetype.source.light.common.exception.UserUseCaseException;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.RuntimeUnavailableException;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.DefaultAuthorizationService.AuthorizationDeniedException;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO;

@Component("graphQlExceptionResolver")
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {
    @Override
    protected GraphQLError resolveToSingleError(Throwable exception, DataFetchingEnvironment environment) {
        if (exception instanceof RuntimeUnavailableException unavailable) {
            return securityError(environment, unavailable.reasonCode(), "Tianquan-Jianshen authorization runtime unavailable", ErrorType.INTERNAL_ERROR);
        }
        if (exception instanceof AuthenticationException) {
            return securityError(environment, "AUTHENTICATION_REQUIRED", "Tianquan-Shoubing authentication failed", ErrorType.UNAUTHORIZED);
        }
        if (exception instanceof AccessDeniedException || exception instanceof AuthorizationDeniedException) {
            IdpAuthorizationObservationContextBO marker = environment.getGraphQlContext().get(NativeGraphQlSecurityInterceptor.OBSERVATION_MARKER_KEY);
            if (marker == null || !marker.isConfirmed()) {
                return securityError(environment, "OAUTH_AUDIT_DELIVERY_UNAVAILABLE", "Tianquan-Jianshen authorization runtime unavailable", ErrorType.INTERNAL_ERROR);
            }
            String reason = exception instanceof AuthorizationDeniedException denied ? denied.reasonCode()
                    : "FIELD_ACCESS_DENIED".equals(exception.getMessage()) ? "FIELD_ACCESS_DENIED" : "AUTHORIZATION_DENIED";
            return securityError(environment, reason, "Tianquan-Jianshen authorization denied", ErrorType.FORBIDDEN);
        }
        if (exception instanceof UserUseCaseException failure) {
            ExceptionLogger.log(log, failure);
            return publicError(failure.getStatus(), failure.getMessage());
        }
        if (exception instanceof TeachingUseCaseException failure) {
            ExceptionLogger.log(log, failure);
            return publicError(failure.getStatus(), failure.getMessage());
        }
        return null;
    }

    GraphQLError resolveForTest(Throwable exception, DataFetchingEnvironment environment) {
        return resolveToSingleError(exception, environment);
    }

    private static GraphQLError securityError(DataFetchingEnvironment environment, String code, String message, ErrorType classification) {
        return GraphqlErrorBuilder.newError(environment).message(message).errorType(classification).extensions(Map.of("code", code)).build();
    }

    private static GraphQLError publicError(String code, String message) {
        return GraphqlErrorBuilder.newError()
                .message(message)
                .extensions(Map.of("code", code))
                .build();
    }
}
