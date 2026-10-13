package top.egon.cola.archetype.source.light.adapter.handler;

import graphql.GraphQLContext;
import graphql.GraphQLError;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;
import top.egon.cola.archetype.source.light.adapter.graphql.NativeGraphQlSecurityInterceptor;
import top.egon.cola.archetype.source.light.common.exception.TeachingUseCaseException;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GraphQlExceptionResolverTest {
    @Test
    void securityMapperRequiresConfirmedAuditAndDoesNotPublishAnotherDenial() {
        var environment = mock(DataFetchingEnvironment.class);
        var step = mock(ExecutionStepInfo.class);
        Mockito.when(step.getPath()).thenReturn(ResultPath.parse("/user"));
        Mockito.when(environment.getExecutionStepInfo()).thenReturn(step);
        Mockito.when(environment.getField()).thenReturn(Field.newField("user").sourceLocation(new SourceLocation(1, 1)).build());
        var marker = new IdpAuthorizationObservationContextBO();
        var context = GraphQLContext.newContext().of(NativeGraphQlSecurityInterceptor.OBSERVATION_MARKER_KEY, marker).build();
        Mockito.when(environment.getGraphQlContext()).thenReturn(context);
        var resolver = new GraphQlExceptionResolver();
        var denied = new AccessDeniedException("private-scope-name");
        var pending = resolver.resolveForTest(denied, environment);
        assertThat(pending.getExtensions()).containsEntry("code", "OAUTH_AUDIT_DELIVERY_UNAVAILABLE");
        assertThat(marker.getDelivery()).isEqualTo(IdpAuthorizationObservationContextBO.DeliveryStateEnum.NEW);
        assertThat(marker.denyOnce(() -> true)).isTrue();
        var confirmed = resolver.resolveForTest(denied, environment);
        assertThat(confirmed.getExtensions()).containsEntry("code", "AUTHORIZATION_DENIED");
        assertThat(confirmed.getErrorType().toString()).isEqualTo("FORBIDDEN");
        assertThat(confirmed.getMessage()).doesNotContain("private-scope-name");
    }

    @Test
    void exposes_public_code_without_stack_trace() {
        GraphQLError error = new GraphQlExceptionResolver().resolveForTest(
                new TeachingUseCaseException("COURSE_EXISTS", "Course exists", new IllegalStateException("secret")),
                mock(DataFetchingEnvironment.class));
        assertThat(error.getExtensions()).containsEntry("code", "COURSE_EXISTS");
        assertThat(error.toString()).doesNotContain("secret");
    }
}
