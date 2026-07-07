package top.egon.cola.component.ddc.common;

import top.egon.cola.component.common.util.CryptoUtils;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;

public final class DdcChecksum {

    private DdcChecksum() {
    }

    public static String sha256(DdcPublishMessage message) {
        return CryptoUtils.sha256Hex(String.join("|",
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
