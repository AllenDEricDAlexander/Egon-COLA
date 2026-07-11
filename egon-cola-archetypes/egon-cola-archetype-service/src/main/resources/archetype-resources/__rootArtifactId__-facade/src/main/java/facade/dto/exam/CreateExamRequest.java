#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.dto.exam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;

public record CreateExamRequest(
        @NotBlank String courseId,
        @NotBlank String title,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt) implements Serializable {
}
