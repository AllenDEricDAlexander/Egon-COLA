package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;

import java.util.Optional;

public interface DdcPublishTaskRepository extends JpaRepository<DdcPublishTaskEntity, String> {

    Optional<DdcPublishTaskEntity> findByChangeId(String changeId);
}
