package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigVersionEntity;

import java.util.List;
import java.util.Optional;

public interface DdcConfigVersionRepository extends JpaRepository<DdcConfigVersionEntity, String> {

    List<DdcConfigVersionEntity> findByConfigIdOrderByVersionDesc(String configId);

    Page<DdcConfigVersionEntity> findByConfigIdOrderByVersionDescIdDesc(
            String configId,
            Pageable pageable);

    Optional<DdcConfigVersionEntity> findByConfigIdAndVersion(String configId, Long version);

    List<DdcConfigVersionEntity> findByBizCodeAndEnvAndAppCodeAndResourceName(
            String bizCode, String env, String appCode, String resourceName);

    @Query("""
            select version
              from DdcConfigVersionEntity version,
                   DdcConfigItemEntity item
             where version.configId = item.id
               and version.version = item.publishedVersion
               and item.bizCode = :bizCode
               and item.env = :env
               and item.appCode = :appCode
               and item.deleted = false
               and (version.changeType is null or version.changeType <> :deleteType)
            """)
    Page<DdcConfigVersionEntity> findPublishedRuntimeVersions(
            @Param("bizCode") String bizCode,
            @Param("env") String env,
            @Param("appCode") String appCode,
            @Param("deleteType") String deleteType,
            Pageable pageable);
}
