package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.egon.cola.component.ddc.admin.model.entity.DdcNamespaceEntity;

import java.util.List;
import java.util.Optional;

public interface DdcNamespaceRepository extends JpaRepository<DdcNamespaceEntity, String> {

    Optional<DdcNamespaceEntity> findByAppCodeAndEnvAndNamespace(String appCode, String env, String namespace);

    List<DdcNamespaceEntity> findByAppCodeAndEnv(String appCode, String env);

    @Query("SELECT DISTINCT n.namespace FROM DdcNamespaceEntity n ORDER BY n.namespace")
    List<String> findDistinctNamespaces();

    @Query("SELECT DISTINCT n.appCode FROM DdcNamespaceEntity n WHERE n.namespace = :namespace")
    List<String> findDistinctAppCodesByNamespace(@Param("namespace") String namespace);
}
