package top.egon.cola.archetype.source.webopen.infrastructure.client.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import top.egon.cola.archetype.source.webopen.domain.client.ExternalDependencyException;
import top.egon.cola.archetype.source.webopen.domain.client.ExternalDependencyFailure;
import org.junit.jupiter.api.Test;

class LocalEvaluationQueryStubTest {

    private final LocalEvaluationQueryStub stub = new LocalEvaluationQueryStub();

    @Test
    void returnsDeterministicLocalData() {
        assertThat(stub.getCourse(1001L).name()).isEqualTo("Local Course 1001");
        assertThat(stub.getExam(2001L).courseId()).isEqualTo(9001L);
        assertThat(stub.getScore(2001L, 3001L).studentId()).isEqualTo(7001L);
    }

    @Test
    void mapsMissingIdentifiersToNotFound() {
        assertThatThrownBy(() -> stub.getCourse(0L))
                .isInstanceOfSatisfying(ExternalDependencyException.class,
                        failure -> assertThat(failure.failure())
                                .isEqualTo(ExternalDependencyFailure.NOT_FOUND));
    }
}
