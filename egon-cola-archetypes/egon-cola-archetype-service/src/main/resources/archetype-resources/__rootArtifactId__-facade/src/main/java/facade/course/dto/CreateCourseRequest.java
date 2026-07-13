#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

public record CreateCourseRequest(
        @NotBlank(message = "code must not be blank")
        @Size(max = 96, message = "code length must be less than or equal to 96")
        String code,
        @NotBlank(message = "name must not be blank")
        @Size(max = 64, message = "name length must be less than or equal to 64")
        String name,
        @Positive(message = "credit must be positive")
        int credit
) implements Serializable {}
