#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.exam.dto;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

public record PublishExamRequest(@NotBlank String examId) implements Serializable {
}
