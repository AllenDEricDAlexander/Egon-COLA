package top.egon.cola.component.ddc.admin.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.model.entity.DdcAppEntity;
import top.egon.cola.component.ddc.admin.repository.DdcAppRepository;
import top.egon.cola.component.ddc.admin.repository.DdcBizRepository;
import top.egon.cola.component.ddc.admin.repository.DdcNamespaceRepository;
import top.egon.cola.component.ddc.common.DdcErrorStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DdcAppService {

    private final DdcScopeGate scopeGate;

    private final DdcAppRepository appRepository;

    private final DdcBizRepository bizRepository;

    private final DdcNamespaceRepository namespaceRepository;

    public DdcAppService(DdcAppRepository appRepository,
                         DdcBizRepository bizRepository,
                         DdcNamespaceRepository namespaceRepository,
                         DdcScopeGate scopeGate) {
        this.appRepository = appRepository;
        this.bizRepository = bizRepository;
        this.namespaceRepository = namespaceRepository;
        this.scopeGate = scopeGate;
    }

    public List<DdcAppEntity> list(String bizCode, String keyword) {
        boolean hasBiz = bizCode != null && !bizCode.isBlank();
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (!hasBiz && !hasKeyword) {
            return appRepository.findAll(Sort.by(Sort.Direction.ASC, "appCode"));
        }
        String trimmedKeyword = hasKeyword ? keyword.trim() : null;
        if (hasBiz && hasKeyword) {
            return appRepository.findByBizCodeAndAppCodeContainingIgnoreCaseOrBizCodeAndAppNameContainingIgnoreCase(
                    bizCode.trim(), trimmedKeyword, bizCode.trim(), trimmedKeyword);
        }
        if (hasBiz) {
            return appRepository.findByBizCode(bizCode.trim());
        }
        return appRepository.findByAppCodeContainingIgnoreCaseOrAppNameContainingIgnoreCase(
                trimmedKeyword, trimmedKeyword);
    }

    public Optional<DdcAppEntity> findByAppCode(String appCode) {
        return appRepository.findByAppCode(appCode);
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
        if (!bizRepository.existsByBizCode(app.getBizCode())) {
            throw new CommonException(DdcErrorStatus.BIZ_NOT_FOUND);
        }
        if (appRepository.existsByAppCode(app.getAppCode())) {
            throw new CommonException(DdcErrorStatus.APP_CODE_EXISTS);
        }
        app.setUpdatedAt(now);
        return appRepository.save(app);
    }

    @Transactional
    public DdcAppEntity update(String appCode, DdcAppEntity request) {
        DdcAppEntity existing = require(appCode);
        if (!bizRepository.existsByBizCode(request.getBizCode())) {
            throw new CommonException(DdcErrorStatus.BIZ_NOT_FOUND);
        }
        existing.setBizCode(request.getBizCode());
        existing.setAppName(request.getAppName());
        existing.setOwner(request.getOwner());
        existing.setDescription(request.getDescription());
        existing.setUpdatedAt(LocalDateTime.now());
        return appRepository.save(existing);
    }

    @Transactional
    public void delete(String appCode) {
        DdcAppEntity existing = require(appCode);
        if (namespaceRepository.existsByAppCode(appCode)) {
            throw new CommonException(DdcErrorStatus.APP_IN_USE);
        }
        appRepository.delete(existing);
        scopeGate.invalidate("app:" + appCode);
        scopeGate.invalidate("app-biz:" + appCode);
    }

    @Transactional
    public DdcAppEntity setEnabled(String appCode, boolean enabled) {
        DdcAppEntity existing = require(appCode);
        existing.setEnabled(enabled);
        existing.setUpdatedAt(LocalDateTime.now());
        DdcAppEntity saved = appRepository.save(existing);
        scopeGate.invalidate("app:" + appCode);
        scopeGate.invalidate("app-biz:" + appCode);
        return saved;
    }

    private DdcAppEntity require(String appCode) {
        return appRepository.findByAppCode(appCode)
                .orElseThrow(() -> new CommonException(DdcErrorStatus.APP_NOT_FOUND));
    }
}
