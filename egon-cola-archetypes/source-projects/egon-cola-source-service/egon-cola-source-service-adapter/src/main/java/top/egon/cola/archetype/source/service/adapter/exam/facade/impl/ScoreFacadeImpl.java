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
import org.apache.dubbo.config.annotation.DubboService;
@DubboService(interfaceClass = ScoreFacade.class, version = "1.0.0", group = "score")
@RequiredArgsConstructor
public class ScoreFacadeImpl implements ScoreFacade {
    private final ScoreManage scoreManage; private final ScoreFacadeConverter converter;
    private final ScoreFacadeValidator validator; private final GlobalFacadeExceptionHandler handler;
    public SingleResponse<ScoreResponse> recordScore(RecordScoreRequest request) { try { validator.require(request); return SingleResponse.of(converter.toResponse(scoreManage.record(converter.toCommand(request)))); } catch (RuntimeException e) { return handler.toFailure(e); } }
    public SingleResponse<ScoreResponse> getScore(GetScoreRequest request) { try { validator.require(request); return SingleResponse.of(converter.toResponse(scoreManage.get(new GetScoreQuery(request.examId(), request.scoreId())))); } catch (RuntimeException e) { return handler.toFailure(e); } }
    public SingleResponse<PageResponse<ScoreResponse>> pageScores(PageScoreRequest request) { try { validator.require(request); var page = scoreManage.page(new PageScoreQuery(request.examId(), request.currentPage(), request.pageSize())); return SingleResponse.of(PageResponse.of(page.records().stream().map(converter::toResponse).toList(), page.currentPage(), page.totalPages(), page.pageSize(), page.totalCount())); } catch (RuntimeException e) { return handler.toFailure(e); } }
}
