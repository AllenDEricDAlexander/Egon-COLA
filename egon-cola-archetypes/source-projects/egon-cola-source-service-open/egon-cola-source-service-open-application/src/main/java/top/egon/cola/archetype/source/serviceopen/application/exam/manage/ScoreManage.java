package top.egon.cola.archetype.source.serviceopen.application.exam.manage;

import top.egon.cola.archetype.source.serviceopen.application.exam.pojo.command.RecordScoreCommand;
import top.egon.cola.archetype.source.serviceopen.application.exam.pojo.query.GetScoreQuery;
import top.egon.cola.archetype.source.serviceopen.application.exam.pojo.query.PageScoreQuery;
import top.egon.cola.archetype.source.serviceopen.application.exam.pojo.result.ScoreResult;
import top.egon.cola.archetype.source.serviceopen.application.pojo.result.PageResult;

public interface ScoreManage {
    ScoreResult record(RecordScoreCommand command);
    ScoreResult get(GetScoreQuery query);
    PageResult<ScoreResult> page(PageScoreQuery query);
}
