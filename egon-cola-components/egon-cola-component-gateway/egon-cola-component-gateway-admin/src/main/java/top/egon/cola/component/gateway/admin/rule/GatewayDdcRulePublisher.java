package top.egon.cola.component.gateway.admin.rule;

import top.egon.cola.component.ddc.management.DdcManagementClient;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.management.model.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishResult;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishStatus;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleChunkRef;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class GatewayDdcRulePublisher {

    public static final String ACTIVE_CONFIG_KEY = "gateway.rules.active";

    private final DdcManagementClient client;

    private final Duration timeout;

    public GatewayDdcRulePublisher(
            DdcManagementClient client,
            Duration timeout) {
        this.client = Objects.requireNonNull(client, "client");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }

    public GatewayRulePublishResult publish(
            CompiledGatewayRelease release,
            Long expectedActiveVersion,
            String changeId,
            String operator) {
        Objects.requireNonNull(release, "release");
        String appCode = appCode(
                release.snapshot().content().gatewayGroupCode()
        );
        String env = release.snapshot().content().env();
        String namespace = release.snapshot().content().namespace();
        ensureReadyTarget(appCode, env, namespace);

        List<DdcManagementPublishResult> chunkResults = new ArrayList<>();
        List<GatewayRuleChunkRef> chunks = release.activation().chunks()
                .stream()
                .sorted(Comparator.comparingInt(GatewayRuleChunkRef::index))
                .toList();
        for (GatewayRuleChunkRef chunk : chunks) {
            DdcManagementPublishResult result = publish(
                    appCode,
                    env,
                    namespace,
                    chunk.configKey(),
                    release.chunkValues().get(chunk.configKey()),
                    null,
                    changeId + "-chunk-" + chunk.index(),
                    operator
            );
            requireSuccess(result, "chunk " + chunk.index());
            chunkResults.add(result);
        }
        DdcManagementPublishResult activation = publish(
                appCode,
                env,
                namespace,
                ACTIVE_CONFIG_KEY,
                release.activationJson(),
                expectedActiveVersion,
                changeId,
                operator
        );
        requireSuccess(activation, "activation");
        return new GatewayRulePublishResult(chunkResults, activation);
    }

    public static String appCode(String gatewayGroupCode) {
        if (gatewayGroupCode == null
                || !gatewayGroupCode.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,63}")) {
            throw new IllegalArgumentException(
                    "gatewayGroupCode is not safe for a DDC scope"
            );
        }
        return "gateway-engine-" + gatewayGroupCode;
    }

    private void ensureReadyTarget(
            String appCode,
            String env,
            String namespace) {
        List<DdcManagementConfigClientInstance> targets =
                client.getConfigClients(new DdcManagementInstanceQuery(
                        appCode,
                        env,
                        namespace
                ));
        if (targets == null || targets.isEmpty()) {
            throw new IllegalStateException(
                    "GATEWAY_RELEASE_NO_READY_TARGET"
            );
        }
    }

    private DdcManagementPublishResult publish(
            String appCode,
            String env,
            String namespace,
            String configKey,
            String value,
            Long expectedVersion,
            String changeId,
            String operator) {
        return client.publish(new DdcManagementPublishRequest(
                appCode,
                env,
                namespace,
                configKey,
                value,
                expectedVersion,
                changeId,
                timeout.toMillis(),
                operator
        ));
    }

    private void requireSuccess(
            DdcManagementPublishResult result,
            String phase) {
        if (result.status() != DdcManagementPublishStatus.SUCCESS) {
            throw new IllegalStateException(
                    "GATEWAY_DDC_PUBLISH_FAILED: "
                            + phase
                            + " status="
                            + result.status()
            );
        }
    }
}
