#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.jpa;
import ${package}.infrastructure.exam.repo.po.ScorePo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ScoreJpaRepository extends JpaRepository<ScorePo, Long> {
    java.util.Optional<ScorePo> findByExamIdAndId(Long examId, Long id);
    long countByExamIdAndStudentId(Long examId, Long studentId);
    Page<ScorePo> findByExamId(Long examId, Pageable pageable);
}
