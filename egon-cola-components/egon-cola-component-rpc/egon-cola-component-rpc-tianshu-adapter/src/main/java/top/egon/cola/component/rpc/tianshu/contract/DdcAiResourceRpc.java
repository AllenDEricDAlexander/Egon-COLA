package top.egon.cola.component.rpc.tianshu.contract;

import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiPromptResult;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiPublicationReceipt;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiResourceQuery;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiRevisionResult;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillDraftUpload;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillPublishRequest;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillResult;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportBatch;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportReceipt;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiVersionHead;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.DdcAiResourceServiceGrpc;

/** Tianshu Prompt/Skill 快照读取、使用反馈上报与 Skill 上行同步的 RPC 门面。 */
@EgonRpcService(
        grpcClass = DdcAiResourceServiceGrpc.class,
        group = "tianshu",
        version = "1.0.0")
public interface DdcAiResourceRpc {

    @EgonRpcMethod(name = "GetPrompt", idempotent = true)
    AiPromptResult getPrompt(AiResourceQuery query);

    @EgonRpcMethod(name = "GetSkill", idempotent = true)
    AiSkillResult getSkill(AiResourceQuery query);

    @EgonRpcMethod(name = "ReportUsage", idempotent = false)
    AiUsageReportReceipt reportUsage(AiUsageReportBatch batch);

    @EgonRpcMethod(name = "GetVersionHead", idempotent = true)
    AiVersionHead getVersionHead(AiResourceQuery query);

    @EgonRpcMethod(name = "ReplaceSkillDraft", idempotent = false)
    AiRevisionResult replaceSkillDraft(AiSkillDraftUpload upload);

    @EgonRpcMethod(name = "PublishSkillDraft", idempotent = false)
    AiPublicationReceipt publishSkillDraft(AiSkillPublishRequest request);
}
