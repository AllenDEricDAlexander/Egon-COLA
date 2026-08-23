#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.impl;

import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.repos.ExamRepository;
import ${package}.domain.exam.vos.ExamId;
import ${package}.infrastructure.exam.repo.converter.ExamConverter;
import ${package}.infrastructure.exam.repo.mapper.ExamMapper;
import ${package}.infrastructure.exam.repo.po.ExamPo;
import ${package}.infrastructure.validators.EvaluationPersistenceValidator;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ExamRepositoryImpl implements ExamRepository {

    private final ExamMapper mapper;
    private final ExamConverter converter;
    private final EvaluationPersistenceValidator validator;

    @Override
    public Exam save(Exam exam) {
        ExamPo existing = mapper.selectById(exam.getId().value());
        Instant createdAt = existing == null ? Instant.now() : existing.getCreatedAt();
        ExamPo po = converter.toPo(exam, createdAt);
        try {
            int affected = existing == null ? mapper.insert(po) : mapper.updateById(po);
            requireAffected(affected, "save exam");
            return converter.toDomain(po);
        } catch (DataIntegrityViolationException failure) {
            throw validator.translate("save exam", failure);
        }
    }

    @Override
    public Optional<Exam> findById(ExamId id) {
        return Optional.ofNullable(mapper.selectById(id.value())).map(converter::toDomain);
    }

    private static void requireAffected(int affected, String operation) {
        if (affected != 1) {
            throw new IllegalStateException(operation + " affected " + affected + " rows");
        }
    }
}
