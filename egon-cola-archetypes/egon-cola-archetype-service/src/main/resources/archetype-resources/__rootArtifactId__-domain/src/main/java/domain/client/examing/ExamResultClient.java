#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.client.examing;

import java.util.Optional;

import ${package}.domain.entities.examing.ExamResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface ExamResultClient {

    ExamResult save(@NotNull ExamResult examResult);

    Optional<ExamResult> findById(@NotBlank String examResultId);
}
