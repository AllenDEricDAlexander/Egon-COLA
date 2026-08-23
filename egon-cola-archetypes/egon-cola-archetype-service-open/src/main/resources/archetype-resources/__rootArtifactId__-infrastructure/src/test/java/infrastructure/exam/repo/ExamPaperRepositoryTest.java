#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.enums.ExamPaperStatus;
import ${package}.domain.exam.vos.ExamId;
import ${package}.infrastructure.exam.repo.converter.ExamPaperConverter;
import ${package}.infrastructure.exam.repo.impl.ExamPaperRepositoryImpl;
import ${package}.infrastructure.exam.repo.jpa.ExamPaperJpaRepository;
import ${package}.infrastructure.exam.repo.po.ExamPaperPo;
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
class ExamPaperRepositoryTest {
    @Test void shouldRoundTripPaper() {
        var converter = new ExamPaperConverter();
        var paper = new ExamPaper(1003L, new ExamId(1002L), "Paper", 100, ExamPaperStatus.DRAFT);
        assertEquals(100, converter.toDomain(converter.toPo(paper, Instant.EPOCH)).getTotalPoints());
    }

    @Test
    void shouldExposeExamAwarePointLookup() throws NoSuchMethodException {
        ExamPaperJpaRepository.class.getMethod(
                "findByExamIdAndId", Long.class, Long.class);
    }

    @Test
    void shouldPersistAssignedIdWithoutJpaMergeLookup() {
        ExamPaperJpaRepository jpaRepository = mock(ExamPaperJpaRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        ExamPaperRepositoryImpl repository = new ExamPaperRepositoryImpl(
                jpaRepository, new ExamPaperConverter(),
                new EvaluationPersistenceValidator(), entityManager);
        ExamPaper paper = new ExamPaper(
                1003L, new ExamId(1002L), "Paper", 100, ExamPaperStatus.DRAFT);
        when(jpaRepository.findByExamIdAndId(1002L, 1003L))
                .thenReturn(Optional.empty());

        repository.save(paper);

        verify(entityManager).persist(any(ExamPaperPo.class));
        verify(jpaRepository, never()).save(any(ExamPaperPo.class));
    }
}
