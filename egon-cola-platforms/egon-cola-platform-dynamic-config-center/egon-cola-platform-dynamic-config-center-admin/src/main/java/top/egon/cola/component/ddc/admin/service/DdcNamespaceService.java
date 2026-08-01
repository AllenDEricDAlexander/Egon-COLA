package top.egon.cola.component.ddc.admin.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.model.entity.DdcNamespaceEntity;
import top.egon.cola.component.ddc.admin.repository.DdcBizRepository;
import top.egon.cola.component.ddc.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.ddc.admin.repository.DdcNamespaceRepository;
import top.egon.cola.component.ddc.common.DdcErrorStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DdcNamespaceService {

    private final DdcNamespaceRepository namespaceRepository;

    private final DdcBizRepository bizRepository;

    private final DdcNamespaceEnvAppBindingRepository bindingRepository;

    public DdcNamespaceService(DdcNamespaceRepository namespaceRepository,
                               DdcBizRepository bizRepository,
                               DdcNamespaceEnvAppBindingRepository bindingRepository) {
        this.namespaceRepository = namespaceRepository;
        this.bizRepository = bizRepository;
        this.bindingRepository = bindingRepository;
    }

    public List<DdcNamespaceEntity> list(String bizCode, String keyword) {
        boolean hasBiz = bizCode != null && !bizCode.isBlank();
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (!hasBiz && !hasKeyword) {
            return namespaceRepository.findAll(Sort.by(
                    Sort.Direction.ASC, "bizCode", "namespaceCode"));
        }
        if (hasBiz && hasKeyword) {
            return namespaceRepository
                    .findByBizCodeAndNamespaceContainingIgnoreCaseOrBizCodeAndNamespaceCodeContainingIgnoreCase(
                            bizCode.trim(), keyword.trim(),
                            bizCode.trim(), keyword.trim());
        }
        if (hasBiz) {
            return namespaceRepository.findByBizCode(bizCode.trim());
        }
        String value = keyword.trim().toLowerCase();
        return namespaceRepository.findAll(Sort.by(
                        Sort.Direction.ASC, "bizCode", "namespaceCode"))
                .stream()
                .filter(item -> item.getNamespace().toLowerCase().contains(value)
                        || item.getNamespaceCode().toLowerCase().contains(value))
                .toList();
    }

    public Optional<DdcNamespaceEntity> find(
            String bizCode,
            String namespaceCode) {
        return namespaceRepository.findByBizCodeAndNamespaceCode(
                bizCode, namespaceCode);
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
        if (!bizRepository.existsByBizCode(namespace.getBizCode())) {
            throw new CommonException(DdcErrorStatus.BIZ_NOT_FOUND);
        }
        if (namespaceRepository.existsByBizCodeAndNamespaceCode(
                namespace.getBizCode(), namespace.getNamespaceCode())) {
            throw new CommonException(DdcErrorStatus.NAMESPACE_CODE_EXISTS);
        }
        if (namespaceRepository.existsByBizCodeAndNamespace(
                namespace.getBizCode(), namespace.getNamespace())) {
            throw new CommonException(DdcErrorStatus.NAMESPACE_CODE_EXISTS);
        }
        namespace.setUpdatedAt(now);
        return namespaceRepository.save(namespace);
    }

    @Transactional
    public DdcNamespaceEntity update(String id, DdcNamespaceEntity request) {
        DdcNamespaceEntity existing = require(id);
        if (namespaceRepository.existsByBizCodeAndNamespaceAndIdNot(
                existing.getBizCode(), request.getNamespace(), existing.getId())) {
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
        if (bindingRepository.existsByNamespaceId(existing.getId())) {
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
