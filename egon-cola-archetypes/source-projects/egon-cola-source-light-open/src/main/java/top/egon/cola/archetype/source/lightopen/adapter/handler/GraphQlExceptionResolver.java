package top.egon.cola.archetype.source.lightopen.adapter.handler;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.archetype.source.lightopen.common.exception.TeachingUseCaseException;
import top.egon.cola.archetype.source.lightopen.common.exception.UserUseCaseException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {
    @Override
    protected GraphQLError resolveToSingleError(Throwable exception, DataFetchingEnvironment environment) {
        if (exception instanceof UserUseCaseException failure) {
            return publicError(failure.getStatus(), failure.getMessage());
        }
        if (exception instanceof TeachingUseCaseException failure) {
            return publicError(failure.getStatus(), failure.getMessage());
        }
        return null;
    }

    GraphQLError resolveForTest(Throwable exception, DataFetchingEnvironment environment) {
        return resolveToSingleError(exception, environment);
    }

    private static GraphQLError publicError(String code, String message) {
        return GraphqlErrorBuilder.newError()
                .message(message)
                .extensions(Map.of("code", code))
                .build();
    }
}
