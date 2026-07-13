#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;

public record ScheduleCourseRequest(
        @NotBlank String courseId,
        @NotBlank String classId,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt) implements Serializable {
}
