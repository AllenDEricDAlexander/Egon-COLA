package top.egon.cola.component.tianshu.admin.service.metadata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcBizRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceRepository;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service("ddcNamespaceService")
@RequiredArgsConstructor
public class DdcNamespaceService {
    @Qualifier("ddcNamespaceRepository") private final DdcNamespaceRepository namespaceRepository;
    @Qualifier("ddcBizRepository") private final DdcBizRepository bizRepository;
    @Qualifier("ddcNamespaceEnvAppBindingRepository") private final DdcNamespaceEnvAppBindingRepository bindingRepository;

    public List<DdcNamespaceBO> list(String bizCode, String keyword) {
        boolean hasBiz = hasText(bizCode);
        boolean hasKeyword = hasText(keyword);
        if (!hasBiz && !hasKeyword)
            return namespaceRepository.findByBizCodeAndNamespaceContainingIgnoreCaseOrBizCodeAndNamespaceCodeContainingIgnoreCase(
                    null, null, null, null);
        if (hasBiz && !hasKeyword) return namespaceRepository.findByBizCode(bizCode.trim());
        return namespaceRepository.findByBizCodeAndNamespaceContainingIgnoreCaseOrBizCodeAndNamespaceCodeContainingIgnoreCase(
                hasBiz ? bizCode.trim() : null, keyword.trim(), hasBiz ? bizCode.trim() : null, keyword.trim());
    }

    public DdcPageResult<DdcNamespaceBO> page(String bizCode, String keyword, PageQuery pageQuery) {
        return namespaceRepository.search(optional(bizCode), optional(keyword), pageQuery);
    }

    public Optional<DdcNamespaceBO> find(String bizCode, String namespaceCode) {
        return namespaceRepository.findByBizCodeAndNamespaceCode(bizCode, namespaceCode);
    }

    @Transactional
    public DdcNamespaceBO save(DdcNamespaceBO namespace) {
        LocalDateTime now = LocalDateTime.now();
        namespace.setCreatedAt(now).setUpdatedAt(now);
        if (namespace.getEnabled() == null) namespace.setEnabled(true);
        if (!bizRepository.existsByBizCode(namespace.getBizCode())) throw new CommonException(DdcErrorStatus.BIZ_NOT_FOUND);
        if (namespaceRepository.existsByBizCodeAndNamespaceCode(namespace.getBizCode(), namespace.getNamespaceCode())
                || namespaceRepository.existsByBizCodeAndNamespace(namespace.getBizCode(), namespace.getNamespace()))
            throw new CommonException(DdcErrorStatus.NAMESPACE_CODE_EXISTS);
        return namespaceRepository.insert(namespace);
    }

    @Transactional
    public DdcNamespaceBO update(String publicId, DdcNamespaceBO request) {
        DdcNamespaceBO existing = require(publicId);
        if (namespaceRepository.existsByBizCodeAndNamespaceAndIdNot(
                existing.getBizCode(), request.getNamespace(), existing.getId()))
            throw new CommonException(DdcErrorStatus.NAMESPACE_CODE_EXISTS);
        existing.setNamespace(request.getNamespace()).setDescription(request.getDescription())
                .setUpdatedAt(LocalDateTime.now());
        return namespaceRepository.update(existing);
    }

    @Transactional
    public void delete(String publicId) {
        DdcNamespaceBO existing = require(publicId);
        if (bindingRepository.existsByNamespaceId(existing.getId()))
            throw new CommonException(DdcErrorStatus.NAMESPACE_IN_USE);
        namespaceRepository.delete(existing);
    }

    @Transactional
    public DdcNamespaceBO setEnabled(String publicId, boolean enabled) {
        DdcNamespaceBO existing = require(publicId);
        existing.setEnabled(enabled).setUpdatedAt(LocalDateTime.now());
        return namespaceRepository.update(existing);
    }

    private DdcNamespaceBO require(String publicId) {
        return namespaceRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CommonException(DdcErrorStatus.NAMESPACE_NOT_FOUND));
    }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String optional(String value) { return hasText(value) ? value.trim() : null; }
}
