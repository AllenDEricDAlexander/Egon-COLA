package top.egon.cola.component.rpc.tianshu.client.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.rpc.tianshu.contract.DdcAiResourceRpc;
import top.egon.cola.component.rpc.tianshu.mapping.DdcRpcStatusExceptionMapper;
import top.egon.cola.component.rpc.tianshu.security.DdcRpcOperation;
import top.egon.cola.component.tianshu.api.client.DdcAiSyncClient;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.tianshu.model.ai.DdcAiPublicationReceiptDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiResourceQuery;
import top.egon.cola.component.tianshu.model.ai.DdcAiRevisionDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiSkillDraftUploadDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiSkillPublishDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiVersionHeadDTO;

/** Direct RPC adapter for Skill workspace reconciliation and authoring upload. */
@Validated
public final class RpcDdcAiSyncClient implements DdcAiSyncClient {

    private final DdcAiResourceRpc rpc;
    private final DdcAiProtoConverter converter;
    private final DdcRpcStatusExceptionMapper errors;
    private final DdcProperties properties;

    public RpcDdcAiSyncClient(
            DdcAiResourceRpc rpc,
            DdcAiProtoConverter converter,
            DdcRpcStatusExceptionMapper errors,
            DdcProperties properties) {
        this.rpc = rpc;
        this.converter = converter;
        this.errors = errors;
        this.properties = properties;
    }

    @Override
    public DdcAiVersionHeadDTO getVersionHead(@NotNull @Valid DdcAiResourceQuery query) {
        if (query.versionNo() != null) {
            throw new IllegalArgumentException("versionNo must be absent for a version-head query");
        }
        var request = converter.toQuery(new DdcAiQueryDTO(
                properties.getBizCode(), query.namespaceCode(), properties.getEnv(), properties.getAppCode(),
                query.resourceKey(), null, query.resourceId()));
        var response = invoke(() -> rpc.getVersionHead(request), DdcRpcOperation.AI_READ);
        return converter.fromVersionHead(response);
    }

    @Override
    public DdcAiRevisionDTO replaceSkillDraft(@NotNull @Valid DdcAiSkillDraftUploadDTO upload) {
        var request = converter.toProto(upload);
        var response = invoke(() -> rpc.replaceSkillDraft(request), DdcRpcOperation.AI_WRITE);
        return converter.fromRevision(response);
    }

    @Override
    public DdcAiPublicationReceiptDTO publishSkillDraft(@NotNull @Valid DdcAiSkillPublishDTO publish) {
        var request = converter.toProto(publish);
        var response = invoke(() -> rpc.publishSkillDraft(request), DdcRpcOperation.AI_PUBLISH);
        return converter.fromPublicationReceipt(response);
    }

    private <T> T invoke(Invocation<T> invocation, DdcRpcOperation operation) {
        try {
            return invocation.get();
        } catch (RuntimeException failure) {
            throw errors.restore(failure, operation);
        }
    }

    @FunctionalInterface
    private interface Invocation<T> {
        T get();
    }
}
