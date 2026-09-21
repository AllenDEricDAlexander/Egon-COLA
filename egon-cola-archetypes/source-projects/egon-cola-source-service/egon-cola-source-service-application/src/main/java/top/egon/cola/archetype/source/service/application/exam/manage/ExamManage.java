package top.egon.cola.archetype.source.service.application.exam.manage;

import top.egon.cola.archetype.source.service.application.exam.pojo.command.AttachExamPaperCommand;
import top.egon.cola.archetype.source.service.application.exam.pojo.command.CreateExamCommand;
import top.egon.cola.archetype.source.service.application.exam.pojo.command.PublishExamCommand;
import top.egon.cola.archetype.source.service.application.exam.pojo.query.GetExamQuery;
import top.egon.cola.archetype.source.service.application.exam.pojo.result.ExamDetailResult;
import top.egon.cola.archetype.source.service.application.exam.pojo.result.ExamPaperResult;

public interface ExamManage {
    ExamDetailResult create(CreateExamCommand command);
    ExamPaperResult attachPaper(AttachExamPaperCommand command);
    ExamDetailResult publish(PublishExamCommand command);
    ExamDetailResult get(GetExamQuery query);
}
