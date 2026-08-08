package top.egon.cola.component.ddc.common;

import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;
import top.egon.cola.component.ddc.model.dto.DdcPublishTarget;

import java.util.Comparator;

/**
 * 为发布消息和配置内容生成稳定的 SHA-256 摘要。 Generates stable SHA-256 digests for publication messages and configuration content.
 */
public final class DdcChecksum {

    /**
     * 禁止实例化摘要工具类。 Prevents instantiation of the digest utility.
     */
    private DdcChecksum() {
    }

    /**
     * 按固定字段顺序和排序后的目标租约计算发布消息摘要。 Computes a publication-message digest using fixed field order and sorted target leases.
     *
     * @param message 待摘要的发布消息。 publication message to digest
     * @return 小写十六进制 SHA-256 摘要。 lowercase hexadecimal SHA-256 digest
     */
    public static String sha256(DdcPublishMessage message) {
        return Digests.sha256Hex(String.join("|",
                safe(message.getChangeId()),
                safe(message.getBizCode()),
                safe(message.getAppCode()),
                safe(message.getEnv()),
                safe(message.getConfigKey()),
                safe(message.getConfigValue()),
                String.valueOf(message.getTargetVersion()),
                safe(message.getContentChecksum()),
                targets(message)));
    }

    /**
     * 计算配置文本摘要，并将 {@code null} 视为空字符串。 Computes a configuration-text digest, treating {@code null} as an empty string.
     *
     * @param value 配置文本。 configuration text
     * @return 小写十六进制 SHA-256 摘要。 lowercase hexadecimal SHA-256 digest
     */
    public static String content(String value) {
        return Digests.sha256Hex(safe(value));
    }

    /**
     * 将目标按实例和租约排序后编码为稳定字符串。 Encodes targets as a stable string after sorting by instance and lease.
     *
     * @param message 发布消息。 publication message
     * @return 规范化目标字符串。 canonical target string
     */
    private static String targets(DdcPublishMessage message) {
        return message.getTargets().stream()
                .sorted(Comparator.comparing(DdcPublishTarget::instanceId)
                        .thenComparing(DdcPublishTarget::leaseId))
                .map(target -> safe(target.instanceId()) + ":" + safe(target.leaseId()))
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    /**
     * 将空引用规范化为空字符串。 Normalizes a null reference to an empty string.
     *
     * @param value 原始文本。 source text
     * @return 非空文本。 non-null text
     */
    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
