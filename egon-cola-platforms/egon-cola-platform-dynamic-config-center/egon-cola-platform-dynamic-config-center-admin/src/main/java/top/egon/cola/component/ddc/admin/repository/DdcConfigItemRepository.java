package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Optional<DdcConfigItemEntity> findByBizCodeAndEnvAndAppCodeAndResourceName(
            String bizCode, String env, String appCode, String resourceName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DdcConfigItemEntity>
    findForPublishByBizCodeAndEnvAndAppCodeAndResourceName(
            String bizCode,
            String env,
            String appCode,
            String resourceName);

    boolean existsByEnv(String env);

    boolean existsByBizCodeAndAppCode(String bizCode, String appCode);

    List<DdcConfigItemEntity> findByBizCodeAndEnvAndAppCode(
            String bizCode, String env, String appCode);

    List<DdcConfigItemEntity> findByBizCodeAndEnvAndAppCodeAndDeletedFalse(
            String bizCode, String env, String appCode);

    @Query(value = """
            select distinct c.*
              from ddc_config_item c
             where (:bizCode is null or c.biz_code = :bizCode)
               and (:env is null or c.env = :env)
               and (:appCode is null or c.app_code = :appCode)
               and (:resourceName is null or c.config_key like ('%' || :resourceName || '%'))
               and (:includeDeleted = true or c.deleted = false)
               and (:namespaceCode is null or exists (
                   select 1
                     from ddc_namespace_env_app b
                     join ddc_namespace n on n.id = b.namespace_id
                     join ddc_app a on a.id = b.app_id
                    where b.enabled = true
                      and n.enabled = true
                      and n.namespace_code = :namespaceCode
                      and n.biz_code = c.biz_code
                      and a.biz_code = c.biz_code
                      and a.app_code = c.app_code
                      and b.env_code = c.env))
             order by c.biz_code, c.env, c.app_code, c.config_key
            """, nativeQuery = true)
    List<DdcConfigItemEntity> search(
            @Param("bizCode") String bizCode,
            @Param("namespaceCode") String namespaceCode,
            @Param("env") String env,
            @Param("appCode") String appCode,
            @Param("resourceName") String resourceName,
            @Param("includeDeleted") boolean includeDeleted);

    @Query(value = """
            select distinct c.*
              from ddc_config_item c
             where (:bizCode is null or c.biz_code = :bizCode)
               and (:env is null or c.env = :env)
               and (:appCode is null or c.app_code = :appCode)
               and (:resourceName is null or c.config_key like ('%' || :resourceName || '%'))
               and (:includeDeleted = true or c.deleted = false)
               and (:namespaceCode is null or exists (
                   select 1
                     from ddc_namespace_env_app b
                     join ddc_namespace n on n.id = b.namespace_id
                     join ddc_app a on a.id = b.app_id
                    where b.enabled = true
                      and n.enabled = true
                      and n.namespace_code = :namespaceCode
                      and n.biz_code = c.biz_code
                      and a.biz_code = c.biz_code
                      and a.app_code = c.app_code
                      and b.env_code = c.env))
             order by c.biz_code, c.env, c.app_code, c.config_key, c.id
            """,
            countQuery = """
            select count(*)
              from ddc_config_item c
             where (:bizCode is null or c.biz_code = :bizCode)
               and (:env is null or c.env = :env)
               and (:appCode is null or c.app_code = :appCode)
               and (:resourceName is null or c.config_key like ('%' || :resourceName || '%'))
               and (:includeDeleted = true or c.deleted = false)
               and (:namespaceCode is null or exists (
                   select 1
                     from ddc_namespace_env_app b
                     join ddc_namespace n on n.id = b.namespace_id
                     join ddc_app a on a.id = b.app_id
                    where b.enabled = true
                      and n.enabled = true
                      and n.namespace_code = :namespaceCode
                      and n.biz_code = c.biz_code
                      and a.biz_code = c.biz_code
                      and a.app_code = c.app_code
                      and b.env_code = c.env))
            """,
            nativeQuery = true)
    Page<DdcConfigItemEntity> search(
            @Param("bizCode") String bizCode,
            @Param("namespaceCode") String namespaceCode,
            @Param("env") String env,
            @Param("appCode") String appCode,
            @Param("resourceName") String resourceName,
            @Param("includeDeleted") boolean includeDeleted,
            Pageable pageable);

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
