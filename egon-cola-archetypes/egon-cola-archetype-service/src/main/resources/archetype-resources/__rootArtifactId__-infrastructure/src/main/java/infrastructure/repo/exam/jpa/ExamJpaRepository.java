#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.exam.jpa;
import ${package}.infrastructure.repo.exam.po.ExamPo;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ExamJpaRepository extends JpaRepository<ExamPo, String> { }
