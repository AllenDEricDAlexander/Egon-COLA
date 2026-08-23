#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.jpa;
import ${package}.infrastructure.exam.repo.po.ExamPaperPo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ExamPaperJpaRepository extends JpaRepository<ExamPaperPo, String> {
    Optional<ExamPaperPo> findByExamIdAndId(String examId, String id);
    Optional<ExamPaperPo> findByExamId(String examId);
}
