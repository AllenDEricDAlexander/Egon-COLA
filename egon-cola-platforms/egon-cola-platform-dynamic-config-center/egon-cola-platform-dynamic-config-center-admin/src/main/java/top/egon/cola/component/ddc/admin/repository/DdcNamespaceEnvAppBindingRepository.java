package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.egon.cola.component.ddc.admin.model.entity.DdcNamespaceEnvAppBindingEntity;
import top.egon.cola.component.ddc.admin.model.vo.DdcNamespaceEnvAppBindingVO;

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

    @Query(value = """
            select new top.egon.cola.component.ddc.admin.model.vo.DdcNamespaceEnvAppBindingVO(
                   binding.id,
                   namespace.bizCode,
                   namespace.id,
                   namespace.namespaceCode,
                   binding.envCode,
                   app.id,
                   app.appCode,
                   app.appName,
                   binding.enabled)
              from DdcNamespaceEnvAppBindingEntity binding
              join DdcNamespaceEntity namespace on namespace.id = binding.namespaceId
              join DdcAppEntity app on app.id = binding.appId
             where (:bizCode is null or namespace.bizCode = :bizCode)
               and (:namespaceCode is null or namespace.namespaceCode = :namespaceCode)
               and (:env is null or binding.envCode = :env)
               and (:appCode is null or app.appCode = :appCode)
             order by namespace.bizCode, namespace.namespaceCode,
                      binding.envCode, app.appCode, binding.id
            """,
            countQuery = """
            select count(binding)
              from DdcNamespaceEnvAppBindingEntity binding
              join DdcNamespaceEntity namespace on namespace.id = binding.namespaceId
              join DdcAppEntity app on app.id = binding.appId
             where (:bizCode is null or namespace.bizCode = :bizCode)
               and (:namespaceCode is null or namespace.namespaceCode = :namespaceCode)
               and (:env is null or binding.envCode = :env)
               and (:appCode is null or app.appCode = :appCode)
            """)
    Page<DdcNamespaceEnvAppBindingVO> search(
            @Param("bizCode") String bizCode,
            @Param("namespaceCode") String namespaceCode,
            @Param("env") String env,
            @Param("appCode") String appCode,
            Pageable pageable);
}
