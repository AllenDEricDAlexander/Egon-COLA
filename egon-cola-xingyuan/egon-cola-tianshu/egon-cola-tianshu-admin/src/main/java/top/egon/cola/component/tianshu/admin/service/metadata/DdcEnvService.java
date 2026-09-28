package top.egon.cola.component.tianshu.admin.service.metadata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.model.bo.DdcEnvBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcEnvRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service("ddcEnvService")
@RequiredArgsConstructor
public class DdcEnvService {
    @Qualifier("ddcScopeGate") private final DdcScopeGate scopeGate;
    @Qualifier("ddcEnvRepository") private final DdcEnvRepository envRepository;
    @Qualifier("ddcConfigItemRepository") private final DdcConfigItemRepository configItemRepository;
    @Qualifier("ddcNamespaceEnvAppBindingRepository") private final DdcNamespaceEnvAppBindingRepository bindingRepository;
    @Qualifier("ddcNamespaceEnvAppBindingService") private final DdcNamespaceEnvAppBindingService bindingService;

    public List<DdcEnvBO> list(String bizCode, String namespaceCode, String keyword) {
        if (hasText(bizCode) && hasText(namespaceCode)) {
            List<String> visible = bindingService.visibleEnvCodes(bizCode.trim(), namespaceCode.trim());
            return filterKeyword(envRepository.findAllByOrderBySortOrderAsc().stream()
                    .filter(env -> visible.contains(env.getEnvCode())).toList(), keyword);
        }
        if (!hasText(keyword)) return envRepository.findAllByOrderBySortOrderAsc();
        String trimmed = keyword.trim();
        return envRepository.findByEnvCodeContainingIgnoreCaseOrDescriptionContainingIgnoreCase(trimmed, trimmed);
    }

    public DdcPageResult<DdcEnvBO> page(String bizCode, String namespaceCode, String keyword, PageQuery pageQuery) {
        return envRepository.search(optional(bizCode), optional(namespaceCode), optional(keyword), pageQuery);
    }

    public DdcEnvBO findByEnvCode(String envCode) { return require(envCode); }

    @Transactional
    public DdcEnvBO save(DdcEnvBO env) {
        LocalDateTime now = LocalDateTime.now();
        env.setCreatedAt(now).setUpdatedAt(now);
        if (env.getEnabled() == null) env.setEnabled(true);
        if (env.getSortOrder() == null) env.setSortOrder(0);
        if (envRepository.existsByEnvCode(env.getEnvCode())) throw new CommonException(DdcErrorStatus.ENV_CODE_EXISTS);
        return envRepository.insert(env);
    }

    @Transactional
    public DdcEnvBO update(String envCode, DdcEnvBO request) {
        DdcEnvBO existing = require(envCode);
        if (envRepository.existsByEnvCodeAndIdNot(request.getEnvCode(), existing.getId()))
            throw new CommonException(DdcErrorStatus.ENV_CODE_EXISTS);
        existing.setEnvCode(request.getEnvCode()).setDescription(request.getDescription())
                .setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder())
                .setUpdatedAt(LocalDateTime.now());
        return envRepository.update(existing);
    }

    @Transactional
    public void delete(String envCode) {
        DdcEnvBO existing = require(envCode);
        if (configItemRepository.existsByEnv(envCode) || bindingRepository.existsByEnvCode(envCode))
            throw new CommonException(DdcErrorStatus.ENV_IN_USE);
        envRepository.delete(existing);
        scopeGate.invalidate("env:" + envCode);
    }

    @Transactional
    public DdcEnvBO setEnabled(String envCode, boolean enabled) {
        DdcEnvBO existing = require(envCode);
        existing.setEnabled(enabled).setUpdatedAt(LocalDateTime.now());
        DdcEnvBO saved = envRepository.update(existing);
        scopeGate.invalidate("env:" + envCode);
        return saved;
    }

    private DdcEnvBO require(String envCode) {
        return envRepository.findByEnvCode(envCode)
                .orElseThrow(() -> new CommonException(DdcErrorStatus.ENV_NOT_FOUND));
    }
    private List<DdcEnvBO> filterKeyword(List<DdcEnvBO> envs, String keyword) {
        if (!hasText(keyword)) return envs;
        String value = keyword.trim().toLowerCase();
        return envs.stream().filter(env -> env.getEnvCode().toLowerCase().contains(value)
                || env.getDescription() != null && env.getDescription().toLowerCase().contains(value)).toList();
    }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String optional(String value) { return hasText(value) ? value.trim() : null; }
}
