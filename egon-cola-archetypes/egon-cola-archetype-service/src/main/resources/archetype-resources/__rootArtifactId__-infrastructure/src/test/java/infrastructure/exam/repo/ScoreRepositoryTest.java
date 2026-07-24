#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo;
import ${package}.domain.exam.entities.Score;
import ${package}.domain.exam.enums.ScoreStatus;
import ${package}.domain.course.vos.CourseId;
import ${package}.domain.exam.vos.ExamId;
import ${package}.domain.exam.vos.ScoreValue;
import ${package}.infrastructure.exam.repo.converter.ScoreConverter;
import ${package}.domain.exam.repos.ScoreRepository;
import ${package}.infrastructure.exam.repo.impl.ScoreRepositoryImpl;
import ${package}.infrastructure.exam.repo.jpa.ScoreJpaRepository;
import ${package}.infrastructure.exam.repo.po.ScorePo;
import ${package}.infrastructure.validators.EvaluationPersistenceValidator;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
class ScoreRepositoryTest {
    @Test void shouldRoundTripScore() {
        var converter = new ScoreConverter();
        var score = new Score("score-1", new ExamId("exam-1"), new CourseId("course-1"),
                "student-1", new ScoreValue(90), ScoreStatus.RECORDED);
        assertEquals(90, converter.toDomain(converter.toPo(score, Instant.EPOCH)).getPoints().value());
    }

    @Test
    void shouldExposeExamAwarePointLookup() throws NoSuchMethodException {
        ScoreRepository.class.getMethod("findByExamIdAndId", ExamId.class, String.class);
        ScoreJpaRepository.class.getMethod("findByExamIdAndId", String.class, String.class);
    }

    @Test
    void shouldPersistAssignedIdWithoutJpaMergeLookup() {
        ScoreJpaRepository jpaRepository = mock(ScoreJpaRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        ScoreRepositoryImpl repository = new ScoreRepositoryImpl(
                jpaRepository, new ScoreConverter(),
                new EvaluationPersistenceValidator(), entityManager);
        Score score = new Score(
                "score-1", new ExamId("exam-1"), new CourseId("course-1"),
                "student-1", new ScoreValue(90), ScoreStatus.RECORDED);
        when(jpaRepository.findByExamIdAndId("exam-1", "score-1"))
                .thenReturn(Optional.empty());

        repository.save(score);

        verify(entityManager).persist(any(ScorePo.class));
        verify(jpaRepository, never()).save(any(ScorePo.class));
    }
}
