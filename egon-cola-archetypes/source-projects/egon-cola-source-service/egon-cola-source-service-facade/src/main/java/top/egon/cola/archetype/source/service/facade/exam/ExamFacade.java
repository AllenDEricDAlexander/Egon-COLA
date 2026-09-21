package top.egon.cola.archetype.source.service.facade.exam;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.service.facade.proto.AttachExamPaperRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.CreateExamRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.ExamPaperRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.ExamRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.ExamServiceGrpc;
import top.egon.cola.archetype.source.service.facade.proto.GetExamRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.PublishExamRpcRequest;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary contract of the service-owned Exam facade. */
@EgonRpcService(grpcClass = ExamServiceGrpc.class, group = "exam", version = "1.0.0", retries = 0)
public interface ExamFacade {

    @EgonRpcMethod(name = "CreateExam", idempotent = false)
    ExamRpcResponse createExam(@NotNull CreateExamRpcRequest request);

    @EgonRpcMethod(name = "AttachPaper", idempotent = false)
    ExamPaperRpcResponse attachPaper(@NotNull AttachExamPaperRpcRequest request);

    @EgonRpcMethod(name = "PublishExam", idempotent = false)
    ExamRpcResponse publishExam(@NotNull PublishExamRpcRequest request);

    @EgonRpcMethod(name = "GetExam", idempotent = true)
    ExamRpcResponse getExam(@NotNull GetExamRpcRequest request);
}
