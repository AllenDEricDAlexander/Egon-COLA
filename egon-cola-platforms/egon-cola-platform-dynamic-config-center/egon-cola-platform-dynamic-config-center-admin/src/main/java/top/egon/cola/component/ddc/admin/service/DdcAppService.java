package top.egon.cola.component.ddc.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.model.entity.DdcAppEntity;
import top.egon.cola.component.ddc.admin.repository.DdcAppRepository;
import top.egon.cola.component.ddc.admin.repository.DdcNamespaceRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DdcAppService {

    private final DdcAppRepository appRepository;
    private final DdcNamespaceRepository namespaceRepository;

    public DdcAppService(DdcAppRepository appRepository,
                         DdcNamespaceRepository namespaceRepository) {
        this.appRepository = appRepository;
        this.namespaceRepository = namespaceRepository;
    }

    @Transactional
    public DdcAppEntity save(DdcAppEntity app) {
        LocalDateTime now = LocalDateTime.now();
        if (app.getId() == null) {
            app.setId(UuidV7.simpleString());
            app.setCreatedAt(now);
        }
        if (app.getEnabled() == null) {
            app.setEnabled(true);
        }
        app.setUpdatedAt(now);
        return appRepository.save(app);
    }

    public Optional<DdcAppEntity> findByAppCode(String appCode) {
        return appRepository.findByAppCode(appCode);
    }

    public List<DdcAppEntity> list() {
        return appRepository.findAll();
    }

    public List<DdcAppEntity> findByNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return list();
        }
        List<String> appCodes = namespaceRepository.findDistinctAppCodesByNamespace(namespace.trim());
        if (appCodes.isEmpty()) {
            return List.of();
        }
        return appRepository.findAllByAppCodeIn(appCodes);
    }
}
