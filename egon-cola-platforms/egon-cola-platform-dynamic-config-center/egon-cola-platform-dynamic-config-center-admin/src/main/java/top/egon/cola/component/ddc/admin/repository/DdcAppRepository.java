package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.component.ddc.admin.model.entity.DdcAppEntity;

import java.util.List;
import java.util.Optional;

public interface DdcAppRepository extends JpaRepository<DdcAppEntity, String> {

    Optional<DdcAppEntity> findByAppCode(String appCode);

    boolean existsByAppCode(String appCode);

    boolean existsByBizCode(String bizCode);

    List<DdcAppEntity> findAllByAppCodeIn(List<String> appCodes);
}
