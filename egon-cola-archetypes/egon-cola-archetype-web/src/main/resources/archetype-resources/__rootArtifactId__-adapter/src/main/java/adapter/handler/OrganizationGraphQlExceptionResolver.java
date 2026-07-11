package ${package}.adapter.handler;

import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.exceptions.OrganizationApplicationException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.MDC;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class OrganizationGraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable error, DataFetchingEnvironment environment) {
        if (error instanceof OrganizationApplicationException failure) {
            return error(environment, failure.code(), failure.getMessage());
        }
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
