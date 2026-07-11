#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.exam.jpa;
import ${package}.infrastructure.repo.exam.po.ExamPaperPo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ExamPaperJpaRepository extends JpaRepository<ExamPaperPo, String> {
    Optional<ExamPaperPo> findByExamId(String examId);
}
