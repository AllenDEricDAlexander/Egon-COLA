#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam.repos;

import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.vos.ExamId;
import java.util.Optional;

public interface ExamRepository {
    Exam save(Exam exam);
    Optional<Exam> findById(ExamId examId);
}
