#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.dto.exam;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

public record GetScoreRequest(@NotBlank String scoreId) implements Serializable {
}
