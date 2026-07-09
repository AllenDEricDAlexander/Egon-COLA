package ${package}.adapter.handler;

import ${package}.application.teaching.manage.TeachingUseCaseException;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GraphQlExceptionResolverTest {
    @Test
    void exposes_public_code_without_stack_trace() {
        GraphQLError error = new GraphQlExceptionResolver().resolveForTest(
                new TeachingUseCaseException("COURSE_EXISTS", "Course exists", new IllegalStateException("secret")),
                mock(DataFetchingEnvironment.class));
        assertThat(error.getExtensions()).containsEntry("code", "COURSE_EXISTS");
        assertThat(error.toString()).doesNotContain("secret");
    }
}
