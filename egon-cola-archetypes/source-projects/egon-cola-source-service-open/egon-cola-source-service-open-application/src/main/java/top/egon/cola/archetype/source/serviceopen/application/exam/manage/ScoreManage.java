package top.egon.cola.archetype.source.serviceopen.application.exam.manage;

import top.egon.cola.archetype.source.serviceopen.application.exam.command.RecordScoreCommand;
import top.egon.cola.archetype.source.serviceopen.application.exam.query.GetScoreQuery;
import top.egon.cola.archetype.source.serviceopen.application.exam.query.PageScoreQuery;
import top.egon.cola.archetype.source.serviceopen.application.exam.result.ScoreResult;
import top.egon.cola.archetype.source.serviceopen.application.result.PageResult;

public interface ScoreManage {
    ScoreResult record(RecordScoreCommand command);
    ScoreResult get(GetScoreQuery query);
    PageResult<ScoreResult> page(PageScoreQuery query);
}
