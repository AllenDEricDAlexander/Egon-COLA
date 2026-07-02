#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.examing.jpa;

import ${package}.infrastructure.repo.examing.po.ExamResultPo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamResultJpaRepository extends JpaRepository<ExamResultPo, String> {
}
