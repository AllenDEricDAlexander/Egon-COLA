package top.egon.cola.platform.idp.admin.resource.domain.dto;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * Resource Server 变更使用的乐观锁版本。
 *
 * <p>Optimistic-lock version used by a Resource Server mutation.</p>
 *
 * @param expectedVersion 客户端已读取的版本；version previously read by the caller
 */
public record ResourceVersionDTO(@PositiveOrZero long expectedVersion) {
}
