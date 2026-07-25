package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GatewayGroupRepository
        extends JpaRepository<GatewayGroupEntity, String> {

    List<GatewayGroupEntity> findAllByDeletedFalseOrderByCreatedAtDesc();

    Optional<GatewayGroupEntity> findByIdAndDeletedFalse(String id);
}
