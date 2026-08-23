#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.impl;

import ${package}.domain.common.Page;
import ${package}.domain.exam.entities.Score;
import ${package}.domain.exam.repos.ScoreRepository;
import ${package}.domain.exam.vos.ExamId;
import ${package}.infrastructure.exam.repo.converter.ScoreConverter;
import ${package}.infrastructure.exam.repo.mapper.ScoreMapper;
import ${package}.infrastructure.exam.repo.po.ScorePo;
import ${package}.infrastructure.validators.EvaluationPersistenceValidator;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class ScoreRepositoryImpl implements ScoreRepository {

    private final ScoreMapper mapper;
    private final ScoreConverter converter;
    private final EvaluationPersistenceValidator validator;

    @Override
    @Transactional
    public Score save(Score score) {
        try {
            ScorePo existing = mapper.selectByExamIdAndId(
                    score.getExamId().value(), score.getId());
            ScorePo po;
            int affected;
            if (existing == null) {
                po = converter.toPo(score, Instant.now());
                affected = mapper.insert(po);
            } else {
                po = converter.updatePo(score, existing);
                affected = mapper.updateById(po);
            }
            requireAffected(affected, "save score");
            return converter.toDomain(po);
        } catch (DataIntegrityViolationException failure) {
            throw validator.translate("save score", failure);
        }
    }

    @Override
    public Optional<Score> findByExamIdAndId(ExamId examId, long id) {
        return Optional.ofNullable(mapper.selectByExamIdAndId(examId.value(), id))
                .map(converter::toDomain);
    }

    @Override
    public boolean existsByExamIdAndStudentId(ExamId id, long studentId) {
        return mapper.countByExamIdAndStudentId(id.value(), studentId) > 0;
    }

    @Override
    public Page<Score> findPageByExamId(ExamId id, int currentPage, int pageSize) {
        long offset = (long) (Math.max(1, currentPage) - 1) * pageSize;
        var records = mapper.selectPageByExamId(id.value(), offset, pageSize).stream()
                .map(converter::toDomain)
                .toList();
        long totalCount = mapper.countByExamId(id.value());
        int totalPages = pageSize <= 0 ? 0 : (int) ((totalCount + pageSize - 1) / pageSize);
        return Page.of(records, currentPage, totalPages, pageSize, totalCount);
    }

    private static void requireAffected(int affected, String operation) {
        if (affected != 1) {
            throw new IllegalStateException(operation + " affected " + affected + " rows");
        }
    }
}
