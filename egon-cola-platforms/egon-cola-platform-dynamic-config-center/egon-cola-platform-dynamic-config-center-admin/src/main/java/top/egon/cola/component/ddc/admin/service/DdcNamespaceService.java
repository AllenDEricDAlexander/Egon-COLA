package top.egon.cola.component.ddc.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.model.entity.DdcNamespaceEntity;
import top.egon.cola.component.ddc.admin.repository.DdcNamespaceRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DdcNamespaceService {

    private final DdcNamespaceRepository namespaceRepository;

    public DdcNamespaceService(DdcNamespaceRepository namespaceRepository) {
        this.namespaceRepository = namespaceRepository;
    }

    @Transactional
    public DdcNamespaceEntity save(DdcNamespaceEntity namespace) {
        LocalDateTime now = LocalDateTime.now();
        if (namespace.getId() == null) {
            namespace.setId(UuidV7.simpleString());
            namespace.setCreatedAt(now);
        }
        if (namespace.getEnabled() == null) {
            namespace.setEnabled(true);
        }
        namespace.setUpdatedAt(now);
        return namespaceRepository.save(namespace);
    }

    public Optional<DdcNamespaceEntity> find(String appCode, String env, String namespace) {
        return namespaceRepository.findByAppCodeAndEnvAndNamespace(appCode, env, namespace);
    }

    public List<DdcNamespaceEntity> list(String appCode, String env) {
        return namespaceRepository.findByAppCodeAndEnv(appCode, env);
    }
}
