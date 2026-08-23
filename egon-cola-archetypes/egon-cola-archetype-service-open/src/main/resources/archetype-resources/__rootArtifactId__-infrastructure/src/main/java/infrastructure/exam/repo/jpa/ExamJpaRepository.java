#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.jpa;
import ${package}.infrastructure.exam.repo.po.ExamPo;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ExamJpaRepository extends JpaRepository<ExamPo, String> { }
