package top.egon.cola.component.ddc.common;

import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.component.common.crypto.hmac.Hmacs;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;

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
                String.valueOf(message.getTargetVersion())));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
