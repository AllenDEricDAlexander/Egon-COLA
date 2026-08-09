package top.egon.cola.component.ddc.admin.service.metadata;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.model.entity.DdcAppEntity;
import top.egon.cola.component.ddc.admin.repository.DdcAppRepository;
import top.egon.cola.component.ddc.admin.repository.DdcBizRepository;
import top.egon.cola.component.ddc.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.ddc.error.DdcErrorStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DdcAppService {

    private final DdcScopeGate scopeGate;

    private final DdcAppRepository appRepository;

    private final DdcBizRepository bizRepository;

    private final DdcNamespaceEnvAppBindingRepository bindingRepository;

    private final DdcNamespaceEnvAppBindingService bindingService;

    public DdcAppService(DdcAppRepository appRepository,
                         DdcBizRepository bizRepository,
                         DdcNamespaceEnvAppBindingRepository bindingRepository,
                         DdcNamespaceEnvAppBindingService bindingService,
                         DdcScopeGate scopeGate) {
        this.appRepository = appRepository;
        this.bizRepository = bizRepository;
        this.bindingRepository = bindingRepository;
        this.bindingService = bindingService;
        this.scopeGate = scopeGate;
    }

    public List<DdcAppEntity> list(
            String bizCode,
            String namespaceCode,
            String env,
            String keyword) {
        if (hasText(namespaceCode) && hasText(env)) {
            if (!hasText(bizCode)) {
                return List.of();
            }
            return filterKeyword(bindingService.visibleApps(
                    bizCode.trim(), namespaceCode.trim(), env.trim()), keyword);
        }
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

    public Optional<DdcAppEntity> findById(String id) {
        return appRepository.findById(id);
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
        if (appRepository.existsByBizCodeAndAppCode(
                app.getBizCode(), app.getAppCode())) {
            throw new CommonException(DdcErrorStatus.APP_CODE_EXISTS);
        }
        app.setUpdatedAt(now);
        return appRepository.save(app);
    }

    @Transactional
    public DdcAppEntity update(String id, DdcAppEntity request) {
        DdcAppEntity existing = require(id);
        existing.setAppName(request.getAppName());
        existing.setOwner(request.getOwner());
        existing.setDescription(request.getDescription());
        existing.setUpdatedAt(LocalDateTime.now());
        return appRepository.save(existing);
    }

    @Transactional
    public void delete(String id) {
        DdcAppEntity existing = require(id);
        if (bindingRepository.existsByAppId(id)) {
            throw new CommonException(DdcErrorStatus.APP_IN_USE);
        }
        appRepository.delete(existing);
        scopeGate.invalidate(appCacheKey(existing));
    }

    @Transactional
    public DdcAppEntity setEnabled(String id, boolean enabled) {
        DdcAppEntity existing = require(id);
        existing.setEnabled(enabled);
        existing.setUpdatedAt(LocalDateTime.now());
        DdcAppEntity saved = appRepository.save(existing);
        scopeGate.invalidate(appCacheKey(existing));
        return saved;
    }

    private DdcAppEntity require(String id) {
        return appRepository.findById(id)
                .orElseThrow(() -> new CommonException(DdcErrorStatus.APP_NOT_FOUND));
    }

    private List<DdcAppEntity> filterKeyword(
            List<DdcAppEntity> apps,
            String keyword) {
        if (!hasText(keyword)) {
            return apps;
        }
        String value = keyword.trim().toLowerCase();
        return apps.stream()
                .filter(app -> app.getAppCode().toLowerCase().contains(value)
                        || app.getAppName().toLowerCase().contains(value))
                .toList();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String appCacheKey(DdcAppEntity app) {
        return "app:" + app.getBizCode() + ":" + app.getAppCode();
    }
}
