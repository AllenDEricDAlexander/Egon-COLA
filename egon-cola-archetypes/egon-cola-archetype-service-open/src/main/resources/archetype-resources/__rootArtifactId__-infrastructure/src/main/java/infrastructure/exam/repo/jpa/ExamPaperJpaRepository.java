#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.jpa;
import ${package}.infrastructure.exam.repo.po.ExamPaperPo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ExamPaperJpaRepository extends JpaRepository<ExamPaperPo, Long> {
    Optional<ExamPaperPo> findByExamIdAndId(Long examId, Long id);
    Optional<ExamPaperPo> findByExamId(Long examId);
}
