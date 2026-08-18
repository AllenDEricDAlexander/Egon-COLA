package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.egon.cola.component.ddc.admin.model.entity.DdcAppEntity;

import java.util.List;
import java.util.Optional;

public interface DdcAppRepository extends JpaRepository<DdcAppEntity, String> {

    Optional<DdcAppEntity> findByBizCodeAndAppCode(String bizCode, String appCode);

    Optional<DdcAppEntity> findFirstByAppCodeOrderByBizCodeAsc(String appCode);

    boolean existsByAppCode(String appCode);

    boolean existsByBizCodeAndAppCode(String bizCode, String appCode);

    boolean existsByBizCode(String bizCode);

    List<DdcAppEntity> findByBizCode(String bizCode);

    List<DdcAppEntity> findAllByIdIn(List<String> ids);

    List<DdcAppEntity> findByAppCodeContainingIgnoreCaseOrAppNameContainingIgnoreCase(
            String appCode, String appName);

    List<DdcAppEntity> findByBizCodeAndAppCodeContainingIgnoreCaseOrBizCodeAndAppNameContainingIgnoreCase(
            String bizCode, String appCode, String bizCode2, String appName);

    @Query("""
            select app from DdcAppEntity app
             where (:bizCode is null or app.bizCode = :bizCode)
               and (:keyword is null
                    or lower(app.appCode) like lower(concat('%', :keyword, '%'))
                    or lower(app.appName) like lower(concat('%', :keyword, '%')))
               and (:namespaceCode is null or :env is null or exists (
                    select binding.id
                      from DdcNamespaceEnvAppBindingEntity binding,
                           DdcNamespaceEntity namespace
                     where binding.namespaceId = namespace.id
                       and binding.appId = app.id
                       and binding.envCode = :env
                       and binding.enabled = true
                       and namespace.enabled = true
                       and namespace.bizCode = app.bizCode
                       and namespace.namespaceCode = :namespaceCode))
            """)
    Page<DdcAppEntity> search(
            @Param("bizCode") String bizCode,
            @Param("namespaceCode") String namespaceCode,
            @Param("env") String env,
            @Param("keyword") String keyword,
            Pageable pageable);
}
