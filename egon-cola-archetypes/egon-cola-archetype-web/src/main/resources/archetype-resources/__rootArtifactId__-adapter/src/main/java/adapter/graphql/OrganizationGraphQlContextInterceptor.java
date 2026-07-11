package ${package}.adapter.graphql;

import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OrganizationGraphQlContextInterceptor implements WebGraphQlInterceptor {

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String key = request.getHeaders().getFirst("Idempotency-Key");
        if (key != null && !key.isBlank()) {
            request.configureExecutionInput((input, builder) ->
                    builder.graphQLContext(context -> context.put("idempotencyKey", key)).build());
        }
        return chain.next(request);
    }
}
