#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.manage.exam;

import ${package}.application.command.exam.AttachExamPaperCommand;
import ${package}.application.command.exam.CreateExamCommand;
import ${package}.application.command.exam.PublishExamCommand;
import ${package}.application.query.exam.GetExamQuery;
import ${package}.application.result.exam.ExamDetailResult;
import ${package}.application.result.exam.ExamPaperResult;

public interface ExamManage {
    ExamDetailResult create(CreateExamCommand command);
    ExamPaperResult attachPaper(AttachExamPaperCommand command);
    ExamDetailResult publish(PublishExamCommand command);
    ExamDetailResult get(GetExamQuery query);
}
