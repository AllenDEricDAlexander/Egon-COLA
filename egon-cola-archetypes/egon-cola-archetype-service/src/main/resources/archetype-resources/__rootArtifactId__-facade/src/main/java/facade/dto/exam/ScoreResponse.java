#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.dto.exam;

import java.io.Serializable;

public record ScoreResponse(
        String id,
        String examId,
        String courseId,
        String studentId,
        int points,
        String status) implements Serializable {
}
