package top.egon.cola.platform.idp.admin.audit.domain.vo;

import java.util.List;

/**
 * 身份安全审计的分页结果。
 *
 * <p>Paginated identity security-audit result.</p>
 */
public record IdentityAuditPageVO(
        List<IdentityAuditVO> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
