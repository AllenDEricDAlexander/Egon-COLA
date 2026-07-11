#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.dto.exam;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

public record GetExamRequest(@NotBlank String examId) implements Serializable {
}
