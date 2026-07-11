#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.dto.exam;

import java.io.Serializable;

public record ExamPaperResponse(
        String id,
        String examId,
        String title,
        int totalPoints,
        String status) implements Serializable {
}
