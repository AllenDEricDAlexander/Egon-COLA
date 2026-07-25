package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GatewayApplicationRepository
        extends JpaRepository<GatewayApplicationEntity, String> {

    List<GatewayApplicationEntity> findAllByDeletedFalseOrderByCreatedAtDesc();

    Optional<GatewayApplicationEntity> findByIdAndDeletedFalse(String id);
}
