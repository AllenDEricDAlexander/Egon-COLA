package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishAckEntity;

import java.util.List;
import java.util.Optional;

public interface DdcPublishAckRepository extends JpaRepository<DdcPublishAckEntity, String> {

    Optional<DdcPublishAckEntity> findByChangeIdAndInstanceIdAndLeaseId(
            String changeId,
            String instanceId,
            String leaseId
    );

    List<DdcPublishAckEntity> findByChangeId(String changeId);
}
