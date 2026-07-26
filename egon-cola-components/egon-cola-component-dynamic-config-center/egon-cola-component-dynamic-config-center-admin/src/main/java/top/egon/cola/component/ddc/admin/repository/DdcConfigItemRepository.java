package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigItemEntity;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface DdcConfigItemRepository extends JpaRepository<DdcConfigItemEntity, String> {

    Optional<DdcConfigItemEntity> findByAppCodeAndEnvAndNamespaceAndConfigKey(String appCode, String env, String namespace, String configKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DdcConfigItemEntity>
    findForPublishByAppCodeAndEnvAndNamespaceAndConfigKey(
            String appCode,
            String env,
            String namespace,
            String configKey);

    List<DdcConfigItemEntity> findByAppCodeAndEnvAndNamespace(String appCode, String env, String namespace);

    List<DdcConfigItemEntity> findByAppCodeAndEnvAndNamespaceAndDeletedFalse(String appCode, String env, String namespace);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DdcConfigItemEntity item
               set item.publishedVersion = :targetVersion,
                   item.updatedAt = :updatedAt
             where item.id = :configId
               and ((:expectedPublishedVersion is null and item.publishedVersion is null)
                    or item.publishedVersion = :expectedPublishedVersion)
            """)
    int advancePublishedVersion(
            @Param("configId") String configId,
            @Param("expectedPublishedVersion") Long expectedPublishedVersion,
            @Param("targetVersion") Long targetVersion,
            @Param("updatedAt") java.time.LocalDateTime updatedAt
    );
}
