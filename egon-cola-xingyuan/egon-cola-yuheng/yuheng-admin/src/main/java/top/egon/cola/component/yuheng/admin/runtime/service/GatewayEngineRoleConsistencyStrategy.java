package top.egon.cola.component.yuheng.admin.runtime.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.egon.cola.component.tianshu.model.management.DdcManagementConfigClientInstance;
import top.egon.cola.component.yuheng.contract.runtime.GatewayEngineRoleEnum;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 中文说明：仅由明确的节点元数据识别 Engine 角色，不从主机、名称或端口推断。
 * English summary: Applies the required role set independently of release acknowledgement checks.
 * 用法 / Usage: Pass the projection's online-node set; replicas count once for role completeness.
 */
@Slf4j
@RequiredArgsConstructor
@Service("gatewayEngineRoleConsistencyStrategy")
public class GatewayEngineRoleConsistencyStrategy {

    public static final String ROLE_METADATA_KEY = "gateway.engine.role";

    public Optional<GatewayEngineRoleEnum> roleOf(DdcManagementConfigClientInstance node) {
        return node == null ? Optional.empty()
                : GatewayEngineRoleEnum.fromWire(node.metadata().get(ROLE_METADATA_KEY));
    }

    public Set<GatewayEngineRoleEnum> missingRoles(List<DdcManagementConfigClientInstance> nodes) {
        EnumSet<GatewayEngineRoleEnum> missing = EnumSet.allOf(GatewayEngineRoleEnum.class);
        if (nodes != null) {
            nodes.forEach(node -> roleOf(node).ifPresent(missing::remove));
        }
        return Set.copyOf(missing);
    }

    public boolean hasUnknownRole(List<DdcManagementConfigClientInstance> nodes) {
        return nodes != null && nodes.stream().anyMatch(node -> roleOf(node).isEmpty());
    }
}
