package top.egon.cola.component.yaoguang.admin.observability.dao;

import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.yaoguang.admin.observability.domain.po.GatewayAuditLogRecordPO;

/**
 * 中文说明：知识与 Wiki 审计只使用受守卫的本地 CRUD，不查询网关组或发布记录。
 * English summary: Knowledge and Wiki audit uses guarded local CRUD without gateway group or release queries.
 */
public interface GatewayAuditLogDAO extends EgonColaMapper<GatewayAuditLogRecordPO> {
}
