package top.egon.cola.component.rpc.tianshu.client.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.rpc.tianshu.contract.DdcAiResourceRpc;
import top.egon.cola.component.rpc.tianshu.mapping.DdcRpcStatusExceptionMapper;
import top.egon.cola.component.rpc.tianshu.security.DdcRpcOperation;
import top.egon.cola.component.tianshu.api.client.DdcSkillClient;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.tianshu.model.ai.DdcAiResourceQuery;
import top.egon.cola.component.tianshu.model.ai.DdcSkillResult;

/** Direct RPC adapter for the read-only published Skill Port. */
@Validated
public final class RpcDdcSkillClient implements DdcSkillClient {

    private final DdcAiResourceRpc rpc;
    private final DdcAiProtoConverter converter;
    private final DdcRpcStatusExceptionMapper errors;
    private final DdcProperties properties;

    public RpcDdcSkillClient(
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
    public DdcSkillResult get(@NotNull @Valid DdcAiResourceQuery query) {
        var request = converter.toQuery(new DdcAiQueryDTO(
                properties.getBizCode(), query.namespaceCode(), properties.getEnv(), properties.getAppCode(),
                query.resourceKey(), query.versionNo(), query.resourceId()));
        var response = invoke(() -> rpc.getSkill(request));
        return converter.fromSkillResult(response);
    }

    private <T> T invoke(Invocation<T> invocation) {
        try {
            return invocation.get();
        } catch (RuntimeException failure) {
            throw errors.restore(failure, DdcRpcOperation.AI_READ);
        }
    }

    @FunctionalInterface
    private interface Invocation<T> {
        T get();
    }
}
