#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.dto.examing;

public record ExamResultDTO(String id, String courseId, String studentId, int score, String status) {
}
