package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
