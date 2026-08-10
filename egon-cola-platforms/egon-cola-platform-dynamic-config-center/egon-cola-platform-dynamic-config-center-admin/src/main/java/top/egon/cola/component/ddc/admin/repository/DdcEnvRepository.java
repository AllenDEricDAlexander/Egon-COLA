package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.egon.cola.component.ddc.admin.model.entity.DdcEnvEntity;

import java.util.List;
import java.util.Optional;

public interface DdcEnvRepository extends JpaRepository<DdcEnvEntity, String> {

    Optional<DdcEnvEntity> findByEnvCode(String envCode);

    boolean existsByEnvCode(String envCode);

    boolean existsByEnvCodeAndIdNot(String envCode, String id);

    List<DdcEnvEntity> findAllByOrderBySortOrderAsc();

    List<DdcEnvEntity> findByEnvCodeContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String envCode, String description);

    @Query("""
            select env from DdcEnvEntity env
             where (:keyword is null
                    or lower(env.envCode) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(env.description, '')) like lower(concat('%', :keyword, '%')))
               and (:bizCode is null or :namespaceCode is null or exists (
                    select binding.id
                      from DdcNamespaceEnvAppBindingEntity binding,
                           DdcNamespaceEntity namespace
                     where binding.namespaceId = namespace.id
                       and binding.envCode = env.envCode
                       and binding.enabled = true
                       and namespace.enabled = true
                       and namespace.bizCode = :bizCode
                       and namespace.namespaceCode = :namespaceCode))
            """)
    Page<DdcEnvEntity> search(
            @Param("bizCode") String bizCode,
            @Param("namespaceCode") String namespaceCode,
            @Param("keyword") String keyword,
            Pageable pageable);
}
