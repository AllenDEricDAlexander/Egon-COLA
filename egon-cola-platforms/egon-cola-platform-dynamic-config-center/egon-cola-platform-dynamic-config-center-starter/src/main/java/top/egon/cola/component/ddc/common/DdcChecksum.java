package top.egon.cola.component.ddc.common;

import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;
import top.egon.cola.component.ddc.model.dto.DdcPublishTarget;

import java.util.Comparator;

public final class DdcChecksum {

    private DdcChecksum() {
    }

    public static String sha256(DdcPublishMessage message) {
        return Digests.sha256Hex(String.join("|",
                safe(message.getChangeId()),
                safe(message.getAppCode()),
                safe(message.getEnv()),
                safe(message.getNamespace()),
                safe(message.getConfigKey()),
                safe(message.getConfigValue()),
                String.valueOf(message.getTargetVersion()),
                safe(message.getContentChecksum()),
                targets(message)));
    }

    public static String content(String value) {
        return Digests.sha256Hex(safe(value));
    }

    private static String targets(DdcPublishMessage message) {
        return message.getTargets().stream()
                .sorted(Comparator.comparing(DdcPublishTarget::instanceId)
                        .thenComparing(DdcPublishTarget::leaseId))
                .map(target -> safe(target.instanceId()) + ":" + safe(target.leaseId()))
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
