#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.jpa;
import ${package}.infrastructure.exam.repo.po.ScorePo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ScoreJpaRepository extends JpaRepository<ScorePo, String> {
    boolean existsByExamIdAndStudentId(String examId, String studentId);
    Page<ScorePo> findByExamId(String examId, Pageable pageable);
}
