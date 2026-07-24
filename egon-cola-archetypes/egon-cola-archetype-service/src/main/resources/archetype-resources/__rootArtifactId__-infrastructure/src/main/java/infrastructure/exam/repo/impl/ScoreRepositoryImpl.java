#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.impl;
import ${package}.domain.common.Page;
import ${package}.domain.exam.entities.Score;
import ${package}.domain.exam.repos.ScoreRepository;
import ${package}.domain.exam.vos.ExamId;
import ${package}.infrastructure.exam.repo.converter.ScoreConverter;
import ${package}.infrastructure.exam.repo.jpa.ScoreJpaRepository;
import ${package}.infrastructure.exam.repo.po.ScorePo;
import ${package}.infrastructure.validators.EvaluationPersistenceValidator;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
@Repository
@RequiredArgsConstructor
public class ScoreRepositoryImpl implements ScoreRepository {
    private final ScoreJpaRepository repository;
    private final ScoreConverter converter;
    private final EvaluationPersistenceValidator validator;
    private final EntityManager entityManager;
    @Transactional
    public Score save(Score score) {
        ScorePo po = repository.findByExamIdAndId(score.getExamId().value(), score.getId())
                .map(existing -> converter.updatePo(score, existing))
                .orElseGet(() -> persist(converter.toPo(score, Instant.now())));
        try {
            repository.flush();
            return converter.toDomain(po);
        }
        catch (DataIntegrityViolationException failure) { throw validator.translate("save score", failure); }
    }
    public Optional<Score> findByExamIdAndId(ExamId examId, String id) {
        return repository.findByExamIdAndId(examId.value(), id).map(converter::toDomain);
    }
    public boolean existsByExamIdAndStudentId(ExamId id, String studentId) {
        return repository.existsByExamIdAndStudentId(id.value(), studentId);
    }
    public Page<Score> findPageByExamId(ExamId id, int currentPage, int pageSize) {
        var pageable = PageRequest.of(Math.max(1, currentPage) - 1, pageSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
        var page = repository.findByExamId(id.value(), pageable);
        return Page.of(page.getContent().stream().map(converter::toDomain).toList(),
                currentPage, page.getTotalPages(), pageSize, page.getTotalElements());
    }
    private ScorePo persist(ScorePo po) {
        entityManager.persist(po);
        return po;
    }
}
