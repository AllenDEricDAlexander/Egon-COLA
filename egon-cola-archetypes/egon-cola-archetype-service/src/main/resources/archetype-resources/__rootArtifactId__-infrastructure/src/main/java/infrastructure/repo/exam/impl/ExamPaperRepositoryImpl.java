#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.exam.impl;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.repos.ExamPaperRepository;
import ${package}.domain.exam.vos.ExamId;
import ${package}.infrastructure.repo.exam.converter.ExamPaperConverter;
import ${package}.infrastructure.repo.exam.jpa.ExamPaperJpaRepository;
import ${package}.infrastructure.validators.EvaluationPersistenceValidator;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
@Repository
@RequiredArgsConstructor
public class ExamPaperRepositoryImpl implements ExamPaperRepository {
    private final ExamPaperJpaRepository repository;
    private final ExamPaperConverter converter;
    private final EvaluationPersistenceValidator validator;
    public ExamPaper save(ExamPaper paper) {
        Instant createdAt = repository.findById(paper.getId()).map(it -> it.getCreatedAt()).orElseGet(Instant::now);
        try { return converter.toDomain(repository.saveAndFlush(converter.toPo(paper, createdAt))); }
        catch (DataIntegrityViolationException failure) { throw validator.translate("save exam paper", failure); }
    }
    public Optional<ExamPaper> findByExamId(ExamId id) {
        return repository.findByExamId(id.value()).map(converter::toDomain);
    }
}
