package top.egon.cola.evaluation.facade.rpc;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.evaluation.facade.rpc.proto.GetScoreRpcRequest;
import top.egon.cola.evaluation.facade.rpc.proto.PageScoreRpcRequest;
import top.egon.cola.evaluation.facade.rpc.proto.PageScoreRpcResponse;
import top.egon.cola.evaluation.facade.rpc.proto.RecordScoreRpcRequest;
import top.egon.cola.evaluation.facade.rpc.proto.ScoreRpcResponse;
import top.egon.cola.evaluation.facade.rpc.proto.ScoreServiceGrpc;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary transport for the existing Score facade. */
@EgonRpcService(grpcClass = ScoreServiceGrpc.class, group = "score", version = "1.0.0", retries = 0)
public interface ScoreRpcService {

    @EgonRpcMethod(name = "RecordScore", idempotent = false)
    ScoreRpcResponse recordScore(@NotNull RecordScoreRpcRequest request);

    @EgonRpcMethod(name = "GetScore", idempotent = true)
    ScoreRpcResponse getScore(@NotNull GetScoreRpcRequest request);

    @EgonRpcMethod(name = "PageScores", idempotent = true)
    PageScoreRpcResponse pageScores(@NotNull PageScoreRpcRequest request);
}
