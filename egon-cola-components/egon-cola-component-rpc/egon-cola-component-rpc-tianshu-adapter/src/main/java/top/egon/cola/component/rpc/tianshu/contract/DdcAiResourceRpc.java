package top.egon.cola.component.rpc.tianshu.contract;

import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiPromptResult;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiResourceQuery;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillResult;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.DdcAiResourceServiceGrpc;

/** Tianshu 已发布 Prompt/Skill 快照的只读 RPC 门面。 */
@EgonRpcService(
        grpcClass = DdcAiResourceServiceGrpc.class,
        group = "tianshu",
        version = "1.0.0")
public interface DdcAiResourceRpc {

    @EgonRpcMethod(name = "GetPrompt", idempotent = true)
    AiPromptResult getPrompt(AiResourceQuery query);

    @EgonRpcMethod(name = "GetSkill", idempotent = true)
    AiSkillResult getSkill(AiResourceQuery query);
}
