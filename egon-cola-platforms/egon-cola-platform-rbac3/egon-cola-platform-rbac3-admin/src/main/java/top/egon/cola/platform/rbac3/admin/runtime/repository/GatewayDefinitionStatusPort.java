package top.egon.cola.platform.rbac3.admin.runtime.repository;

import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayDefinitionStatusVO;

/** Gateway 定义状态查询端口。 Gateway definition-status query port. */
@FunctionalInterface
public interface GatewayDefinitionStatusPort {

    /** @return 当前定义上报状态；current definition reporting status */
    GatewayDefinitionStatusVO status();
}
