package top.egon.cola.component.gateway.admin.rule;

import top.egon.cola.component.ddc.management.DdcManagementClient;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.management.model.DdcInstanceStatus;
import top.egon.cola.component.ddc.management.model.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishResult;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class GatewayDdcRulePublisher {

    public static final String ACTIVE_CONFIG_KEY =
            GatewayDdcYamlDocument.ACTIVE_CONFIG_KEY;

    private final DdcManagementClient client;

    public GatewayDdcRulePublisher(DdcManagementClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    public DdcManagementPublishResult publish(
            GatewayDdcPublicationCommand command) {
        Objects.requireNonNull(command, "command");
        return client.publish(new DdcManagementPublishRequest(
                command.bizCode(),
                command.env(),
                command.appCode(),
                command.value(),
                command.expectedVersion(),
                command.changeId(),
                command.timeout().toMillis(),
                command.operator()
        ));
    }

    public void ensureReadyTarget(
            String bizCode,
            String env,
            String appCode) {
        List<DdcManagementConfigClientInstance> targets =
                client.getConfigClients(new DdcManagementInstanceQuery(
                        bizCode,
                        env,
                        appCode
                ));
        Instant now = Instant.now();
        boolean ready = targets != null && targets.stream()
                .filter(Objects::nonNull)
                .anyMatch(target -> target.normalizedStatus()
                        == DdcInstanceStatus.ONLINE
                        && target.expireAt() != null
                        && target.expireAt().isAfter(now));
        if (!ready) {
            throw new IllegalStateException(
                    "GATEWAY_RELEASE_NO_READY_TARGET"
            );
        }
    }

}
