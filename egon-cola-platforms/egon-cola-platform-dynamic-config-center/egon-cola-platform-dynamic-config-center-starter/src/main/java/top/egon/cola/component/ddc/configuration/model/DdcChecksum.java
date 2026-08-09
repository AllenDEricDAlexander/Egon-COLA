package top.egon.cola.component.ddc.configuration.model;

import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.component.ddc.configuration.model.DdcPublishMessage;
import top.egon.cola.component.ddc.configuration.model.DdcPublishTarget;

import java.util.Comparator;

/**
 * 为发布消息和配置资源生成稳定的 SHA-256 摘要。 Generates stable SHA-256 digests for publication messages and configuration resources.
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
                safe(message.getResourceName()),
                safe(message.getFormat()),
                safe(message.getContent()),
                String.valueOf(message.getTargetVersion()),
                safe(message.getResourceChecksum()),
                targets(message)));
    }

    /**
     * 计算覆盖资源名、格式和内容的配置资源摘要。
     * Computes a configuration-resource digest covering its name, format, and content.
     *
     * @param resourceName 配置资源名。 configuration resource name
     * @param format       配置格式。 configuration format
     * @param content      配置内容。 configuration content
     * @return 小写十六进制 SHA-256 摘要。 lowercase hexadecimal SHA-256 digest
     */
    public static String resource(
            String resourceName,
            String format,
            String content) {
        return Digests.sha256Hex(String.join("|",
                safe(resourceName),
                safe(format),
                safe(content)));
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
