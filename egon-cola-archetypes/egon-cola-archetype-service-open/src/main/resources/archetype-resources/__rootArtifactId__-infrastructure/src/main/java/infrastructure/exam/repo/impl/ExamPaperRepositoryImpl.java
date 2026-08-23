#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.impl;

import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.repos.ExamPaperRepository;
import ${package}.domain.exam.vos.ExamId;
import ${package}.infrastructure.exam.repo.converter.ExamPaperConverter;
import ${package}.infrastructure.exam.repo.mapper.ExamPaperMapper;
import ${package}.infrastructure.exam.repo.po.ExamPaperPo;
import ${package}.infrastructure.validators.EvaluationPersistenceValidator;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class ExamPaperRepositoryImpl implements ExamPaperRepository {

    private final ExamPaperMapper mapper;
    private final ExamPaperConverter converter;
    private final EvaluationPersistenceValidator validator;

    @Override
    @Transactional
    public ExamPaper save(ExamPaper paper) {
        try {
            ExamPaperPo existing = mapper.selectByExamIdAndId(
                    paper.getExamId().value(), paper.getId());
            ExamPaperPo po;
            int affected;
            if (existing == null) {
                po = converter.toPo(paper, Instant.now());
                affected = mapper.insert(po);
            } else {
                po = converter.updatePo(paper, existing);
                affected = mapper.updateById(po);
            }
            requireAffected(affected, "save exam paper");
            return converter.toDomain(po);
        } catch (DataIntegrityViolationException failure) {
            throw validator.translate("save exam paper", failure);
        }
    }

    @Override
    public Optional<ExamPaper> findByExamId(ExamId id) {
        return Optional.ofNullable(mapper.selectByExamId(id.value())).map(converter::toDomain);
    }

    private static void requireAffected(int affected, String operation) {
        if (affected != 1) {
            throw new IllegalStateException(operation + " affected " + affected + " rows");
        }
    }
}
