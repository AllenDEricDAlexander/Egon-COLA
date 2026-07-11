#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.dto.course;

import java.io.Serializable;

public record CourseResponse(
        String id,
        String code,
        String name,
        int credit,
        String status) implements Serializable {
}
