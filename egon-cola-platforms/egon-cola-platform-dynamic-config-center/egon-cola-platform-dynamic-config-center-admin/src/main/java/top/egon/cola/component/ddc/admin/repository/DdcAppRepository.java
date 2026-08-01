package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.component.ddc.admin.model.entity.DdcAppEntity;

import java.util.List;
import java.util.Optional;

public interface DdcAppRepository extends JpaRepository<DdcAppEntity, String> {

    Optional<DdcAppEntity> findByBizCodeAndAppCode(String bizCode, String appCode);

    Optional<DdcAppEntity> findFirstByAppCodeOrderByBizCodeAsc(String appCode);

    boolean existsByBizCodeAndAppCode(String bizCode, String appCode);

    boolean existsByBizCode(String bizCode);

    List<DdcAppEntity> findByBizCode(String bizCode);

    List<DdcAppEntity> findAllByIdIn(List<String> ids);

    List<DdcAppEntity> findByAppCodeContainingIgnoreCaseOrAppNameContainingIgnoreCase(
            String appCode, String appName);

    List<DdcAppEntity> findByBizCodeAndAppCodeContainingIgnoreCaseOrBizCodeAndAppNameContainingIgnoreCase(
            String bizCode, String appCode, String bizCode2, String appName);
}
