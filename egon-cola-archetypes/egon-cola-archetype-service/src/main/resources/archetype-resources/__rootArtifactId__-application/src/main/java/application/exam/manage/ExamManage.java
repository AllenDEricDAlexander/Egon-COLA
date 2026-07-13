#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam.manage;

import ${package}.application.exam.command.AttachExamPaperCommand;
import ${package}.application.exam.command.CreateExamCommand;
import ${package}.application.exam.command.PublishExamCommand;
import ${package}.application.exam.query.GetExamQuery;
import ${package}.application.exam.result.ExamDetailResult;
import ${package}.application.exam.result.ExamPaperResult;

public interface ExamManage {
    ExamDetailResult create(CreateExamCommand command);
    ExamPaperResult attachPaper(AttachExamPaperCommand command);
    ExamDetailResult publish(PublishExamCommand command);
    ExamDetailResult get(GetExamQuery query);
}
