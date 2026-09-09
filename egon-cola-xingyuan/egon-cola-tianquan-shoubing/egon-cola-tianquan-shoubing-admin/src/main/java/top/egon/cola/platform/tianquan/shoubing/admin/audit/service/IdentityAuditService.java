package top.egon.cola.platform.tianquan.shoubing.admin.audit.service;

import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.dto.IdentityAuditQueryDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.vo.IdentityAuditPageVO;

/**
 * 统一身份安全审计的查询用例入口。
 *
 * <p>Application entry point for identity security-audit queries.</p>
 */
public interface IdentityAuditService {

    IdentityAuditPageVO list(IdentityAuditQueryDTO query);
}
