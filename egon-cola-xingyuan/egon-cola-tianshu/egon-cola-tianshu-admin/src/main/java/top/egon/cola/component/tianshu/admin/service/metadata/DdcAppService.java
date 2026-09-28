package top.egon.cola.component.tianshu.admin.service.metadata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.BusinessException;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAppBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcAppRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcBizRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service("ddcAppService")
@RequiredArgsConstructor
public class DdcAppService {
    @Qualifier("ddcScopeGate") private final DdcScopeGate scopeGate;
    @Qualifier("ddcAppRepository") private final DdcAppRepository appRepository;
    @Qualifier("ddcBizRepository") private final DdcBizRepository bizRepository;
    @Qualifier("ddcNamespaceEnvAppBindingRepository") private final DdcNamespaceEnvAppBindingRepository bindingRepository;
    @Qualifier("ddcNamespaceEnvAppBindingService") private final DdcNamespaceEnvAppBindingService bindingService;

    public List<DdcAppBO> list(String bizCode, String namespaceCode, String env, String keyword) {
        if (hasText(namespaceCode) && hasText(env)) {
            if (!hasText(bizCode)) return List.of();
            return filterKeyword(bindingService.visibleApps(bizCode.trim(), namespaceCode.trim(), env.trim()), keyword);
        }
        boolean hasBiz = hasText(bizCode);
        boolean hasKeyword = hasText(keyword);
        if (!hasBiz && !hasKeyword) return appRepository.findByAppCodeContainingIgnoreCaseOrAppNameContainingIgnoreCase("", "");
        String trimmedKeyword = hasKeyword ? keyword.trim() : null;
        if (hasBiz && hasKeyword) return appRepository
                .findByBizCodeAndAppCodeContainingIgnoreCaseOrBizCodeAndAppNameContainingIgnoreCase(
                        bizCode.trim(), trimmedKeyword, bizCode.trim(), trimmedKeyword);
        if (hasBiz) return appRepository.findByBizCode(bizCode.trim());
        return appRepository.findByAppCodeContainingIgnoreCaseOrAppNameContainingIgnoreCase(
                trimmedKeyword, trimmedKeyword);
    }

    public List<DdcAppBO> list(String bizCode, String keyword, Boolean enabled) {
        return appRepository.list(optional(bizCode), optional(keyword), enabled);
    }

    public DdcPageResult<DdcAppBO> page(String bizCode, String namespaceCode, String env, String keyword,
            PageQuery pageQuery) {
        if (hasText(namespaceCode) && hasText(env) && !hasText(bizCode)) {
            PageQuery page = pageQuery == null ? PageQuery.defaultPage() : pageQuery;
            return DdcPageResult.<DdcAppBO>builder().records(List.of()).total(0)
                    .pageNo(page.pageNo()).pageSize(page.pageSize()).build();
        }
        return appRepository.search(optional(bizCode), optional(namespaceCode), optional(env), optional(keyword),
                null, pageQuery);
    }

    public Optional<DdcAppBO> findById(String publicId) {
        if (publicId == null || publicId.isBlank()) return Optional.empty();
        return appRepository.findByPublicId(publicId);
    }

    @Transactional
    public DdcAppBO save(DdcAppBO app) {
        LocalDateTime now = LocalDateTime.now();
        app.setCreatedAt(now).setUpdatedAt(now);
        if (app.getEnabled() == null) app.setEnabled(true);
        if (!bizRepository.existsByBizCode(app.getBizCode())) throw new CommonException(DdcErrorStatus.BIZ_NOT_FOUND);
        if (appRepository.existsByAppCode(app.getAppCode())) {
            throw new BusinessException(ResultCode.CONCURRENCY_ERROR.getCode(),
                    ResultCode.CONCURRENCY_ERROR.getStatus(), "app code already exists");
        }
        return appRepository.insert(app);
    }

    @Transactional
    public DdcAppBO update(String publicId, DdcAppBO request) {
        DdcAppBO existing = require(publicId);
        existing.setAppName(request.getAppName()).setOwner(request.getOwner())
                .setDescription(request.getDescription()).setUpdatedAt(LocalDateTime.now());
        return appRepository.update(existing);
    }

    @Transactional
    public void delete(String publicId) {
        DdcAppBO existing = require(publicId);
        if (bindingRepository.existsByAppId(existing.getId())) throw new CommonException(DdcErrorStatus.APP_IN_USE);
        appRepository.delete(existing);
        scopeGate.invalidate(appCacheKey(existing));
    }

    @Transactional
    public DdcAppBO setEnabled(String publicId, boolean enabled) {
        DdcAppBO existing = require(publicId);
        existing.setEnabled(enabled).setUpdatedAt(LocalDateTime.now());
        DdcAppBO saved = appRepository.update(existing);
        scopeGate.invalidate(appCacheKey(existing));
        return saved;
    }

    private DdcAppBO require(String publicId) {
        return appRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CommonException(DdcErrorStatus.APP_NOT_FOUND));
    }
    private List<DdcAppBO> filterKeyword(List<DdcAppBO> apps, String keyword) {
        if (!hasText(keyword)) return apps;
        String value = keyword.trim().toLowerCase();
        return apps.stream().filter(app -> app.getAppCode().toLowerCase().contains(value)
                || app.getAppName().toLowerCase().contains(value)).toList();
    }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String optional(String value) { return hasText(value) ? value.trim() : null; }
    private String appCacheKey(DdcAppBO app) { return "app:" + app.getBizCode() + ":" + app.getAppCode(); }
}
