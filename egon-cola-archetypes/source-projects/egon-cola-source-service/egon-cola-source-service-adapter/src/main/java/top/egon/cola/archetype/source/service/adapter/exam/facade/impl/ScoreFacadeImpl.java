package top.egon.cola.archetype.source.service.adapter.exam.facade.impl;
import top.egon.cola.archetype.source.service.adapter.exam.converter.ScoreFacadeConverter;
import top.egon.cola.archetype.source.service.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.service.adapter.exam.validators.ScoreFacadeValidator;
import top.egon.cola.archetype.source.service.application.exam.manage.ScoreManage;
import top.egon.cola.archetype.source.service.application.exam.query.GetScoreQuery;
import top.egon.cola.archetype.source.service.application.exam.query.PageScoreQuery;
import top.egon.cola.evaluation.facade.exam.ScoreFacade;
import top.egon.cola.evaluation.facade.dto.PageResponse;
import top.egon.cola.evaluation.facade.dto.SingleResponse;
import top.egon.cola.evaluation.facade.exam.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
@Component("scoreFacadeImpl")
@Slf4j
@RequiredArgsConstructor
public class ScoreFacadeImpl implements ScoreFacade {
    @Qualifier("scoreManage") private final ScoreManage scoreManage; @Qualifier("scoreFacadeConverterImpl") private final ScoreFacadeConverter converter;
    @Qualifier("scoreFacadeValidator") private final ScoreFacadeValidator validator; @Qualifier("globalFacadeExceptionHandler") private final GlobalFacadeExceptionHandler handler;
    public SingleResponse<ScoreResponse> recordScore(RecordScoreRequest request) { try { validator.require(request); return SingleResponse.of(converter.toResponse(scoreManage.record(converter.toCommand(request)))); } catch (RuntimeException e) { return handler.toFailure(e); } }
    public SingleResponse<ScoreResponse> getScore(GetScoreRequest request) { try { validator.require(request); return SingleResponse.of(converter.toResponse(scoreManage.get(new GetScoreQuery(request.examId(), request.scoreId())))); } catch (RuntimeException e) { return handler.toFailure(e); } }
    public SingleResponse<PageResponse<ScoreResponse>> pageScores(PageScoreRequest request) { try { validator.require(request); var page = scoreManage.page(new PageScoreQuery(request.examId(), request.currentPage(), request.pageSize())); return SingleResponse.of(PageResponse.of(page.records().stream().map(converter::toResponse).toList(), page.currentPage(), page.totalPages(), page.pageSize(), page.totalCount())); } catch (RuntimeException e) { return handler.toFailure(e); } }
}
