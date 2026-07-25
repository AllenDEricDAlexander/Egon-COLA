package top.egon.cola.component.gateway.admin.rule;

import top.egon.cola.component.ddc.management.model.DdcManagementPublishResult;

import java.util.List;
import java.util.Objects;

public record GatewayRulePublishResult(
        List<DdcManagementPublishResult> chunkResults,
        DdcManagementPublishResult activationResult
) {

    public GatewayRulePublishResult {
        chunkResults = List.copyOf(
                Objects.requireNonNull(chunkResults, "chunkResults")
        );
        activationResult = Objects.requireNonNull(
                activationResult,
                "activationResult"
        );
    }
}
