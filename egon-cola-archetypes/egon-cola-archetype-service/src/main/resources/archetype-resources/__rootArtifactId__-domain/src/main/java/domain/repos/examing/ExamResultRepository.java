#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.repos.examing;

import java.util.Optional;

import ${package}.domain.entities.examing.ExamResult;

public interface ExamResultRepository {

    ExamResult save(ExamResult examResult);

    Optional<ExamResult> findById(String examResultId);
}
