package top.egon.cola.component.gateway.admin.mcp.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface McpServerRepository
        extends JpaRepository<McpServerEntity, String> {

    List<McpServerEntity> findAllByGatewayGroupIdAndDeletedFalseOrderByServerCode(
            String gatewayGroupId);

    Optional<McpServerEntity> findByGatewayGroupIdAndServerCodeAndDeletedFalse(
            String gatewayGroupId,
            String serverCode);

    Optional<McpServerEntity> findByIdAndDeletedFalse(String id);
}
