#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.impl;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.repos.ExamPaperRepository;
import ${package}.domain.exam.vos.ExamId;
import ${package}.infrastructure.exam.repo.converter.ExamPaperConverter;
import ${package}.infrastructure.exam.repo.jpa.ExamPaperJpaRepository;
import ${package}.infrastructure.exam.repo.po.ExamPaperPo;
import ${package}.infrastructure.validators.EvaluationPersistenceValidator;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
@Repository
@RequiredArgsConstructor
public class ExamPaperRepositoryImpl implements ExamPaperRepository {
    private final ExamPaperJpaRepository repository;
    private final ExamPaperConverter converter;
    private final EvaluationPersistenceValidator validator;
    private final EntityManager entityManager;
    @Transactional
    public ExamPaper save(ExamPaper paper) {
        ExamPaperPo po = repository.findByExamIdAndId(paper.getExamId().value(), paper.getId())
                .map(existing -> converter.updatePo(paper, existing))
                .orElseGet(() -> persist(converter.toPo(paper, Instant.now())));
        try {
            repository.flush();
            return converter.toDomain(po);
        }
        catch (DataIntegrityViolationException failure) { throw validator.translate("save exam paper", failure); }
    }
    public Optional<ExamPaper> findByExamId(ExamId id) {
        return repository.findByExamId(id.value()).map(converter::toDomain);
    }
    private ExamPaperPo persist(ExamPaperPo po) {
        entityManager.persist(po);
        return po;
    }
}
