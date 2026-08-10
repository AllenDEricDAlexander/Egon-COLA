package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.egon.cola.component.ddc.admin.model.entity.DdcNamespaceEntity;

import java.util.List;
import java.util.Optional;

public interface DdcNamespaceRepository extends JpaRepository<DdcNamespaceEntity, String> {

    boolean existsByBizCode(String bizCode);

    boolean existsByBizCodeAndNamespace(String bizCode, String namespace);

    boolean existsByBizCodeAndNamespaceAndIdNot(String bizCode, String namespace, String id);

    boolean existsByBizCodeAndNamespaceCode(String bizCode, String namespaceCode);

    boolean existsByBizCodeAndNamespaceCodeAndIdNot(
            String bizCode, String namespaceCode, String id);

    Optional<DdcNamespaceEntity> findByBizCodeAndNamespaceCode(
            String bizCode, String namespaceCode);

    List<DdcNamespaceEntity> findByBizCode(String bizCode);

    List<DdcNamespaceEntity> findByBizCodeAndNamespaceContainingIgnoreCaseOrBizCodeAndNamespaceCodeContainingIgnoreCase(
            String bizCode, String namespace, String bizCode2, String namespaceCode);

    @Query("""
            select namespace from DdcNamespaceEntity namespace
             where (:bizCode is null or namespace.bizCode = :bizCode)
               and (:keyword is null
                    or lower(namespace.namespaceCode) like lower(concat('%', :keyword, '%'))
                    or lower(namespace.namespace) like lower(concat('%', :keyword, '%')))
            """)
    Page<DdcNamespaceEntity> search(
            @Param("bizCode") String bizCode,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
