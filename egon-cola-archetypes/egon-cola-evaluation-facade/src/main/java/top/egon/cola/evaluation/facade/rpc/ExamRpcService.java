package top.egon.cola.evaluation.facade.rpc;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.evaluation.facade.rpc.proto.AttachExamPaperRpcRequest;
import top.egon.cola.evaluation.facade.rpc.proto.CreateExamRpcRequest;
import top.egon.cola.evaluation.facade.rpc.proto.ExamPaperRpcResponse;
import top.egon.cola.evaluation.facade.rpc.proto.ExamRpcResponse;
import top.egon.cola.evaluation.facade.rpc.proto.ExamServiceGrpc;
import top.egon.cola.evaluation.facade.rpc.proto.GetExamRpcRequest;
import top.egon.cola.evaluation.facade.rpc.proto.PublishExamRpcRequest;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary transport for the existing Exam facade. */
@EgonRpcService(grpcClass = ExamServiceGrpc.class, group = "exam", version = "1.0.0", retries = 0)
public interface ExamRpcService {

    @EgonRpcMethod(name = "CreateExam", idempotent = false)
    ExamRpcResponse createExam(@NotNull CreateExamRpcRequest request);

    @EgonRpcMethod(name = "AttachPaper", idempotent = false)
    ExamPaperRpcResponse attachPaper(@NotNull AttachExamPaperRpcRequest request);

    @EgonRpcMethod(name = "PublishExam", idempotent = false)
    ExamRpcResponse publishExam(@NotNull PublishExamRpcRequest request);

    @EgonRpcMethod(name = "GetExam", idempotent = true)
    ExamRpcResponse getExam(@NotNull GetExamRpcRequest request);
}
