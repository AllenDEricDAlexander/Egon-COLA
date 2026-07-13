#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.exam.impl;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.repos.ExamRepository;
import ${package}.domain.exam.vos.ExamId;
import ${package}.infrastructure.repo.exam.converter.ExamConverter;
import ${package}.infrastructure.repo.exam.jpa.ExamJpaRepository;
import ${package}.infrastructure.validators.EvaluationPersistenceValidator;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
@Repository
@RequiredArgsConstructor
public class ExamRepositoryImpl implements ExamRepository {
    private final ExamJpaRepository repository;
    private final ExamConverter converter;
    private final EvaluationPersistenceValidator validator;
    public Exam save(Exam exam) {
        Instant createdAt = repository.findById(exam.getId().value())
                .map(it -> it.getCreatedAt()).orElseGet(Instant::now);
        try { return converter.toDomain(repository.saveAndFlush(converter.toPo(exam, createdAt))); }
        catch (DataIntegrityViolationException failure) { throw validator.translate("save exam", failure); }
    }
    public Optional<Exam> findById(ExamId id) { return repository.findById(id.value()).map(converter::toDomain); }
}
