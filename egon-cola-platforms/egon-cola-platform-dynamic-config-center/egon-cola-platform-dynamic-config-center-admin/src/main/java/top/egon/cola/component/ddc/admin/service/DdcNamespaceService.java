package top.egon.cola.component.ddc.admin.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.model.entity.DdcNamespaceEntity;
import top.egon.cola.component.ddc.admin.repository.DdcAppRepository;
import top.egon.cola.component.ddc.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.ddc.admin.repository.DdcNamespaceRepository;
import top.egon.cola.component.ddc.common.DdcErrorStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DdcNamespaceService {

    private final DdcNamespaceRepository namespaceRepository;

    private final DdcAppRepository appRepository;

    private final DdcConfigItemRepository configItemRepository;

    public DdcNamespaceService(DdcNamespaceRepository namespaceRepository,
                               DdcAppRepository appRepository,
                               DdcConfigItemRepository configItemRepository) {
        this.namespaceRepository = namespaceRepository;
        this.appRepository = appRepository;
        this.configItemRepository = configItemRepository;
    }

    public List<DdcNamespaceEntity> list(String appCode, String keyword) {
        boolean hasApp = appCode != null && !appCode.isBlank();
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (!hasApp && !hasKeyword) {
            return namespaceRepository.findAll(Sort.by(Sort.Direction.ASC, "appCode"));
        }
        if (hasApp && hasKeyword) {
            return namespaceRepository.findByAppCodeAndNamespaceContainingIgnoreCase(
                    appCode.trim(), keyword.trim());
        }
        if (hasApp) {
            return namespaceRepository.findByAppCode(appCode.trim());
        }
        return namespaceRepository.findAll(Sort.by(Sort.Direction.ASC, "appCode"))
                .stream()
                .filter(item -> item.getNamespace().toLowerCase().contains(keyword.trim().toLowerCase()))
                .toList();
    }

    public Optional<DdcNamespaceEntity> find(String appCode, String namespace) {
        return namespaceRepository.findByAppCodeAndNamespace(appCode, namespace);
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
        if (!appRepository.existsByAppCode(namespace.getAppCode())) {
            throw new CommonException(DdcErrorStatus.APP_NOT_FOUND);
        }
        if (namespaceRepository.existsByAppCodeAndNamespace(
                namespace.getAppCode(), namespace.getNamespace())) {
            throw new CommonException(DdcErrorStatus.NAMESPACE_CODE_EXISTS);
        }
        namespace.setUpdatedAt(now);
        return namespaceRepository.save(namespace);
    }

    @Transactional
    public DdcNamespaceEntity update(String id, DdcNamespaceEntity request) {
        DdcNamespaceEntity existing = require(id);
        if (namespaceRepository.existsByAppCodeAndNamespaceAndIdNot(
                existing.getAppCode(), request.getNamespace(), existing.getId())) {
            throw new CommonException(DdcErrorStatus.NAMESPACE_CODE_EXISTS);
        }
        existing.setNamespace(request.getNamespace());
        existing.setDescription(request.getDescription());
        existing.setUpdatedAt(LocalDateTime.now());
        return namespaceRepository.save(existing);
    }

    @Transactional
    public void delete(String id) {
        DdcNamespaceEntity existing = require(id);
        if (configItemRepository.existsByAppCodeAndNamespace(
                existing.getAppCode(), existing.getNamespace())) {
            throw new CommonException(DdcErrorStatus.NAMESPACE_IN_USE);
        }
        namespaceRepository.delete(existing);
    }

    @Transactional
    public DdcNamespaceEntity setEnabled(String id, boolean enabled) {
        DdcNamespaceEntity existing = require(id);
        existing.setEnabled(enabled);
        existing.setUpdatedAt(LocalDateTime.now());
        return namespaceRepository.save(existing);
    }

    private DdcNamespaceEntity require(String id) {
        return namespaceRepository.findById(id)
                .orElseThrow(() -> new CommonException(DdcErrorStatus.NAMESPACE_NOT_FOUND));
    }
}
