#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.course.dto;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

public record GetCourseRequest(@NotBlank String courseId) implements Serializable {
}
