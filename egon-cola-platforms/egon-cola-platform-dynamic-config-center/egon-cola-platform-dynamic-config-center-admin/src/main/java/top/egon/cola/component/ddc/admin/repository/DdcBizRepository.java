package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.component.ddc.admin.model.entity.DdcBizEntity;

import java.util.List;
import java.util.Optional;

public interface DdcBizRepository extends JpaRepository<DdcBizEntity, String> {

    Optional<DdcBizEntity> findByBizCode(String bizCode);

    boolean existsByBizCode(String bizCode);

    boolean existsByBizCodeAndIdNot(String bizCode, String id);

    List<DdcBizEntity> findByBizCodeContainingIgnoreCaseOrBizNameContainingIgnoreCase(
            String bizCode, String bizName);
}
