package top.egon.cola.archetype.source.webopen.infrastructure.client.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import top.egon.cola.archetype.source.webopen.common.exception.ExternalDependencyException;
import top.egon.cola.archetype.source.webopen.infrastructure.client.evaluation.impl.LocalEvaluationQueryClientImpl;
import top.egon.cola.archetype.source.webopen.common.enums.ExternalDependencyFailure;
import org.junit.jupiter.api.Test;

class LocalEvaluationQueryClientImplTest {

    private final LocalEvaluationQueryClientImpl client = new LocalEvaluationQueryClientImpl();

    @Test
    void returnsDeterministicLocalData() {
        assertThat(client.getCourse(1001L).name()).isEqualTo("Local Course 1001");
        assertThat(client.getExam(2001L).courseId()).isEqualTo(9001L);
        assertThat(client.getScore(2001L, 3001L).studentId()).isEqualTo(7001L);
    }

    @Test
    void mapsMissingIdentifiersToNotFound() {
        assertThatThrownBy(() -> client.getCourse(0L))
                .isInstanceOfSatisfying(ExternalDependencyException.class,
                        failure -> assertThat(failure.failure())
                                .isEqualTo(ExternalDependencyFailure.NOT_FOUND));
    }
}
