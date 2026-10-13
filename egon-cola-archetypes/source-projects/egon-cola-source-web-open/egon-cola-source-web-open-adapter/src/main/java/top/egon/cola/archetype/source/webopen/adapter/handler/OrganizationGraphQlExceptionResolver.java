package top.egon.cola.archetype.source.webopen.adapter.handler;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.common.core.exception.ExceptionLogger;
import top.egon.cola.archetype.source.webopen.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.webopen.common.exception.OrganizationApplicationException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.MDC;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component("organizationGraphQlExceptionResolver")
public class OrganizationGraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable error, DataFetchingEnvironment environment) {
        if (error instanceof OrganizationApplicationException failure) {
            ExceptionLogger.log(log, failure);
            return error(environment, failure.code(), failure.getMessage());
        }
        ExceptionLogger.log(log, error);
        return error(environment, "ORG_INTERNAL_ERROR", "Internal server error");
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
