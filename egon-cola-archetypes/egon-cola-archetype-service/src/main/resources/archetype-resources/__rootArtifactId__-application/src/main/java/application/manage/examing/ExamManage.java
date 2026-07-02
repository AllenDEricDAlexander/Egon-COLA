#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.manage.examing;

import ${package}.application.view.examing.ExamResultView;

public interface ExamManage {

    ExamResultView record(String courseId, String studentId, int score);

    ExamResultView getById(String examResultId);
}
