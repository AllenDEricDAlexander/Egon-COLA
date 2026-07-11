#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.repos.exam;

import ${package}.domain.entities.exam.Exam;
import ${package}.domain.vos.exam.ExamId;
import java.util.Optional;

public interface ExamRepository {
    Exam save(Exam exam);
    Optional<Exam> findById(ExamId examId);
}
