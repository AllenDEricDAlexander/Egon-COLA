package top.egon.cola.archetype.source.service.facade.exam;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.service.facade.proto.GetScoreRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.PageScoreRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.PageScoreRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.RecordScoreRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.ScoreRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.ScoreServiceGrpc;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary contract of the service-owned Score facade. */
@EgonRpcService(grpcClass = ScoreServiceGrpc.class, group = "score", version = "1.0.0", retries = 0)
public interface ScoreFacade {

    @EgonRpcMethod(name = "RecordScore", idempotent = false)
    ScoreRpcResponse recordScore(@NotNull RecordScoreRpcRequest request);

    @EgonRpcMethod(name = "GetScore", idempotent = true)
    ScoreRpcResponse getScore(@NotNull GetScoreRpcRequest request);

    @EgonRpcMethod(name = "PageScores", idempotent = true)
    PageScoreRpcResponse pageScores(@NotNull PageScoreRpcRequest request);
}
