#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCourseRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 64, message = "name length must be less than or equal to 64")
        String name,
        @Positive(message = "credit must be positive")
        int credit
) {
}
