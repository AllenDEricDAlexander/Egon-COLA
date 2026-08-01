package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.egon.cola.component.ddc.admin.model.entity.DdcNamespaceEnvAppBindingEntity;

import java.util.List;

public interface DdcNamespaceEnvAppBindingRepository
        extends JpaRepository<DdcNamespaceEnvAppBindingEntity, String> {

    boolean existsByNamespaceId(String namespaceId);

    boolean existsByAppId(String appId);

    boolean existsByEnvCode(String envCode);

    boolean existsByNamespaceIdAndEnvCodeAndAppId(
            String namespaceId, String envCode, String appId);

    boolean existsByNamespaceIdAndEnvCodeAndAppIdAndIdNot(
            String namespaceId, String envCode, String appId, String id);

    List<DdcNamespaceEnvAppBindingEntity>
            findByNamespaceIdAndEnvCodeAndEnabledTrue(
                    String namespaceId, String envCode);

    List<DdcNamespaceEnvAppBindingEntity>
            findByNamespaceIdAndEnabledTrue(String namespaceId);

    @Query(value = """
            select distinct n.namespace_code
              from ddc_namespace_env_app b
              join ddc_namespace n on n.id = b.namespace_id
              join ddc_app a on a.id = b.app_id
             where b.enabled = true
               and n.enabled = true
               and n.biz_code = :bizCode
               and b.env_code = :env
               and a.biz_code = :bizCode
               and a.app_code = :appCode
             order by n.namespace_code
            """, nativeQuery = true)
    List<String> findVisibleNamespaceCodes(
            @Param("bizCode") String bizCode,
            @Param("env") String env,
            @Param("appCode") String appCode);

    @Query(value = """
            select distinct a.biz_code, b.env_code, a.app_code
              from ddc_namespace_env_app b
              join ddc_namespace n on n.id = b.namespace_id
              join ddc_app a on a.id = b.app_id
             where b.enabled = true
               and n.enabled = true
               and n.namespace_code = :namespaceCode
               and (:bizCode is null or n.biz_code = :bizCode)
               and a.biz_code = n.biz_code
            """, nativeQuery = true)
    List<Object[]> findVisiblePhysicalScopes(
            @Param("bizCode") String bizCode,
            @Param("namespaceCode") String namespaceCode);
}
