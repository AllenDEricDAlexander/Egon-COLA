#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.exam.facade.impl;
import ${package}.adapter.exam.converter.ScoreFacadeConverter;
import ${package}.adapter.handler.GlobalFacadeExceptionHandler;
import ${package}.adapter.exam.validators.ScoreFacadeValidator;
import ${package}.application.exam.manage.ScoreManage;
import ${package}.application.exam.query.GetScoreQuery;
import ${package}.application.exam.query.PageScoreQuery;
import ${package}.facade.exam.ScoreFacade;
import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.SingleResponse;
import ${package}.facade.exam.dto.*;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
@DubboService(interfaceClass = ScoreFacade.class, version = "1.0.0", group = "score")
@RequiredArgsConstructor
public class ScoreFacadeImpl implements ScoreFacade {
    private final ScoreManage scoreManage; private final ScoreFacadeConverter converter;
    private final ScoreFacadeValidator validator; private final GlobalFacadeExceptionHandler handler;
    public SingleResponse<ScoreResponse> recordScore(RecordScoreRequest request) { try { validator.require(request); return SingleResponse.of(converter.toResponse(scoreManage.record(converter.toCommand(request)))); } catch (RuntimeException e) { return handler.toFailure(e); } }
    public SingleResponse<ScoreResponse> getScore(GetScoreRequest request) { try { validator.require(request); return SingleResponse.of(converter.toResponse(scoreManage.get(new GetScoreQuery(request.scoreId())))); } catch (RuntimeException e) { return handler.toFailure(e); } }
    public SingleResponse<PageResponse<ScoreResponse>> pageScores(PageScoreRequest request) { try { validator.require(request); var page = scoreManage.page(new PageScoreQuery(request.examId(), request.currentPage(), request.pageSize())); return SingleResponse.of(PageResponse.of(page.records().stream().map(converter::toResponse).toList(), page.currentPage(), page.totalPages(), page.pageSize(), page.totalCount())); } catch (RuntimeException e) { return handler.toFailure(e); } }
}
