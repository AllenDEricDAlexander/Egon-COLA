#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.client.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ${package}.domain.client.ExternalDependencyException;
import ${package}.domain.client.ExternalDependencyFailure;
import org.junit.jupiter.api.Test;

class LocalEvaluationQueryStubTest {

    private final LocalEvaluationQueryStub stub = new LocalEvaluationQueryStub();

    @Test
    void returnsDeterministicLocalData() {
        assertThat(stub.getCourse("course-1").name()).isEqualTo("Local Course course-1");
        assertThat(stub.getExam("exam-1").courseId()).isEqualTo("local-course");
        assertThat(stub.getScore("score-1").studentId()).isEqualTo("local-student");
    }

    @Test
    void mapsMissingIdentifiersToNotFound() {
        assertThatThrownBy(() -> stub.getCourse("missing-course"))
                .isInstanceOfSatisfying(ExternalDependencyException.class,
                        failure -> assertThat(failure.failure())
                                .isEqualTo(ExternalDependencyFailure.NOT_FOUND));
    }
}
