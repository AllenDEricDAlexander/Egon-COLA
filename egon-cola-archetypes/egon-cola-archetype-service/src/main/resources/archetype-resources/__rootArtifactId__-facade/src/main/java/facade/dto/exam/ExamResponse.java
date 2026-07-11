#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.dto.exam;

import java.io.Serializable;
import java.time.Instant;

public record ExamResponse(
        String id,
        String courseId,
        String title,
        Instant startsAt,
        Instant endsAt,
        String status) implements Serializable {
}
