#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.manage.examing;

import ${package}.domain.entities.examing.ExamResult;

public interface ExamManage {

    ExamResult record(String courseId, String studentId, int score);

    ExamResult getById(String examResultId);
}
