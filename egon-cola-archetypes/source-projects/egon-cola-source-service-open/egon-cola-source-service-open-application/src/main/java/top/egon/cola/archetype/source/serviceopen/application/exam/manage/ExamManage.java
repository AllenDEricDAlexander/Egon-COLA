package top.egon.cola.archetype.source.serviceopen.application.exam.manage;

import top.egon.cola.archetype.source.serviceopen.application.exam.command.AttachExamPaperCommand;
import top.egon.cola.archetype.source.serviceopen.application.exam.command.CreateExamCommand;
import top.egon.cola.archetype.source.serviceopen.application.exam.command.PublishExamCommand;
import top.egon.cola.archetype.source.serviceopen.application.exam.query.GetExamQuery;
import top.egon.cola.archetype.source.serviceopen.application.exam.result.ExamDetailResult;
import top.egon.cola.archetype.source.serviceopen.application.exam.result.ExamPaperResult;

public interface ExamManage {
    ExamDetailResult create(CreateExamCommand command);
    ExamPaperResult attachPaper(AttachExamPaperCommand command);
    ExamDetailResult publish(PublishExamCommand command);
    ExamDetailResult get(GetExamQuery query);
}
