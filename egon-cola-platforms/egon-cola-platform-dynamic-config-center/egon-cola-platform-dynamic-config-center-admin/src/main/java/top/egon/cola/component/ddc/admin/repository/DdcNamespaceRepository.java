package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.egon.cola.component.ddc.admin.model.entity.DdcNamespaceEntity;

import java.util.List;
import java.util.Optional;

public interface DdcNamespaceRepository extends JpaRepository<DdcNamespaceEntity, String> {

    boolean existsByAppCode(String appCode);

    boolean existsByAppCodeAndNamespace(String appCode, String namespace);

    boolean existsByAppCodeAndNamespaceAndIdNot(String appCode, String namespace, String id);

    boolean existsByNamespaceCode(String namespaceCode);

    boolean existsByNamespaceCodeAndIdNot(String namespaceCode, String id);

    Optional<DdcNamespaceEntity> findByAppCodeAndNamespace(String appCode, String namespace);

    List<DdcNamespaceEntity> findByAppCode(String appCode);

    List<DdcNamespaceEntity> findByAppCodeAndNamespaceContainingIgnoreCase(
            String appCode, String namespace);
}
