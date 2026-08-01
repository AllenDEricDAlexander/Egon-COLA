package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
