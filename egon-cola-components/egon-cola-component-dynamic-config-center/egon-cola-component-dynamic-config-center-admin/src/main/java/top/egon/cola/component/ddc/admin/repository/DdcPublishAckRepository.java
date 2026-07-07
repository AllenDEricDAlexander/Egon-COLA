package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishAckEntity;

import java.util.List;
import java.util.Optional;

public interface DdcPublishAckRepository extends JpaRepository<DdcPublishAckEntity, String> {

    Optional<DdcPublishAckEntity> findByChangeIdAndInstanceId(String changeId, String instanceId);

    List<DdcPublishAckEntity> findByChangeId(String changeId);
}
