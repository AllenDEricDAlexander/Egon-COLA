#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.exam.impl;
import ${package}.domain.common.Page;
import ${package}.domain.entities.exam.Score;
import ${package}.domain.repos.exam.ScoreRepository;
import ${package}.domain.vos.exam.ExamId;
import ${package}.infrastructure.repo.exam.converter.ScoreConverter;
import ${package}.infrastructure.repo.exam.jpa.ScoreJpaRepository;
import ${package}.infrastructure.repo.exam.po.ScorePo;
import ${package}.infrastructure.validators.EvaluationPersistenceValidator;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
@Repository
@RequiredArgsConstructor
public class ScoreRepositoryImpl implements ScoreRepository {
    private final ScoreJpaRepository repository;
    private final ScoreConverter converter;
    private final EvaluationPersistenceValidator validator;
    public Score save(Score score) {
        Instant createdAt = repository.findById(score.getId()).map(ScorePo::getCreatedAt).orElseGet(Instant::now);
        try { return converter.toDomain(repository.saveAndFlush(converter.toPo(score, createdAt))); }
        catch (DataIntegrityViolationException failure) { throw validator.translate("save score", failure); }
    }
    public Optional<Score> findById(String id) { return repository.findById(id).map(converter::toDomain); }
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
}
