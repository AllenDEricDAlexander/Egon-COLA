package top.egon.cola.component.tianshu.admin.service.metadata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAppBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceEnvAppBindingBO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcNamespaceEnvAppBindingRequest;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcNamespaceEnvAppBindingResultConverter;
import top.egon.cola.component.tianshu.admin.model.vo.DdcNamespaceEnvAppBindingResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcNamespaceEnvAppBindingVO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcAppRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcEnvRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceRepository;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service("ddcNamespaceEnvAppBindingService")
@RequiredArgsConstructor
public class DdcNamespaceEnvAppBindingService {
    @Qualifier("ddcNamespaceEnvAppBindingRepository") private final DdcNamespaceEnvAppBindingRepository bindingRepository;
    @Qualifier("ddcNamespaceRepository") private final DdcNamespaceRepository namespaceRepository;
    @Qualifier("ddcAppRepository") private final DdcAppRepository appRepository;
    @Qualifier("ddcEnvRepository") private final DdcEnvRepository envRepository;
    @Qualifier("ddcNamespaceEnvAppBindingResultConverterImpl")
    private final DdcNamespaceEnvAppBindingResultConverter resultConverter;

    public List<DdcNamespaceEnvAppBindingVO> list(String bizCode, String namespaceCode, String env, String appCode) {
        return bindingRepository.findAll(optional(bizCode), optional(namespaceCode), optional(env), optional(appCode))
                .stream().map(this::toVO).toList();
    }

    public DdcPageResult<DdcNamespaceEnvAppBindingVO> page(String bizCode, String namespaceCode, String env,
            String appCode, PageQuery pageQuery) {
        DdcPageResult<DdcNamespaceEnvAppBindingBO> page = bindingRepository.search(optional(bizCode),
                optional(namespaceCode), optional(env), optional(appCode), pageQuery);
        return DdcPageResult.<DdcNamespaceEnvAppBindingVO>builder()
                .records(page.getRecords().stream().map(this::toVO).toList()).total(page.getTotal())
                .pageNo(page.getPageNo()).pageSize(page.getPageSize()).build();
    }

    @Transactional
    public DdcNamespaceEnvAppBindingVO create(DdcNamespaceEnvAppBindingRequest request) {
        DdcNamespaceBO namespace = requireNamespace(request);
        DdcAppBO app = requireApp(request);
        String envCode = requireEnv(request.env());
        if (bindingRepository.existsByNamespaceIdAndEnvCodeAndAppId(namespace.getId(), envCode, app.getId()))
            throw new CommonException(DdcErrorStatus.NAMESPACE_BINDING_EXISTS);
        LocalDateTime now = LocalDateTime.now();
        DdcNamespaceEnvAppBindingBO binding = DdcNamespaceEnvAppBindingBO.builder()
                .namespaceId(namespace.getId()).envCode(envCode).appId(app.getId())
                .enabled(request.enabled() == null || request.enabled()).createdAt(now).updatedAt(now).build();
        DdcNamespaceEnvAppBindingBO inserted = bindingRepository.insert(binding);
        enrich(inserted, namespace, app);
        return toVO(inserted);
    }

    @Transactional
    public DdcNamespaceEnvAppBindingVO update(String publicId, DdcNamespaceEnvAppBindingRequest request) {
        DdcNamespaceEnvAppBindingBO binding = require(publicId);
        DdcNamespaceBO namespace = requireNamespace(request);
        DdcAppBO app = requireApp(request);
        String envCode = requireEnv(request.env());
        if (bindingRepository.existsByNamespaceIdAndEnvCodeAndAppIdAndIdNot(
                namespace.getId(), envCode, app.getId(), binding.getId()))
            throw new CommonException(DdcErrorStatus.NAMESPACE_BINDING_EXISTS);
        binding.setNamespaceId(namespace.getId()).setEnvCode(envCode).setAppId(app.getId())
                .setUpdatedAt(LocalDateTime.now());
        if (request.enabled() != null) binding.setEnabled(request.enabled());
        DdcNamespaceEnvAppBindingBO updated = bindingRepository.update(binding);
        enrich(updated, namespace, app);
        return toVO(updated);
    }

    @Transactional
    public void delete(String publicId) { bindingRepository.delete(require(publicId)); }

    public List<DdcAppBO> visibleApps(String bizCode, String namespaceCode, String env) {
        DdcNamespaceBO namespace = namespaceRepository.findByBizCodeAndNamespaceCode(bizCode, namespaceCode)
                .orElse(null);
        if (namespace == null) return List.of();
        List<Long> appIds = bindingRepository.findByNamespaceIdAndEnvCodeAndEnabledTrue(namespace.getId(), env)
                .stream().map(DdcNamespaceEnvAppBindingBO::getAppId).distinct().toList();
        return appRepository.findAllByIdIn(appIds).stream().filter(app -> bizCode.equals(app.getBizCode()))
                .sorted(Comparator.comparing(DdcAppBO::getAppCode)).toList();
    }

    public List<String> visibleEnvCodes(String bizCode, String namespaceCode) {
        DdcNamespaceBO namespace = namespaceRepository.findByBizCodeAndNamespaceCode(bizCode, namespaceCode)
                .orElse(null);
        if (namespace == null) return List.of();
        return bindingRepository.findByNamespaceIdAndEnabledTrue(namespace.getId()).stream()
                .map(DdcNamespaceEnvAppBindingBO::getEnvCode).distinct().sorted().toList();
    }

    private DdcNamespaceBO requireNamespace(DdcNamespaceEnvAppBindingRequest request) {
        return namespaceRepository.findByBizCodeAndNamespaceCode(required(request.bizCode()), required(request.namespaceCode()))
                .orElseThrow(() -> new CommonException(DdcErrorStatus.NAMESPACE_NOT_FOUND));
    }
    private DdcAppBO requireApp(DdcNamespaceEnvAppBindingRequest request) {
        return appRepository.findByBizCodeAndAppCode(required(request.bizCode()), required(request.appCode()))
                .orElseThrow(() -> new CommonException(DdcErrorStatus.APP_NOT_FOUND));
    }
    private String requireEnv(String env) {
        String value = required(env);
        if (!envRepository.existsByEnvCode(value)) throw new CommonException(DdcErrorStatus.ENV_NOT_FOUND);
        return value;
    }
    private DdcNamespaceEnvAppBindingBO require(String publicId) {
        return bindingRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CommonException(DdcErrorStatus.NAMESPACE_BINDING_NOT_FOUND));
    }
    private DdcNamespaceEnvAppBindingVO toVO(DdcNamespaceEnvAppBindingBO binding) {
        DdcNamespaceEnvAppBindingResult result = resultConverter.toTarget(binding);
        return new DdcNamespaceEnvAppBindingVO(result.getId(), result.getBizCode(), result.getNamespaceId(),
                result.getNamespaceCode(), result.getEnv(), result.getAppId(), result.getAppCode(), result.getAppName(),
                result.isEnabled());
    }
    private void enrich(DdcNamespaceEnvAppBindingBO binding, DdcNamespaceBO namespace, DdcAppBO app) {
        binding.setBizCode(namespace.getBizCode()).setNamespaceCode(namespace.getNamespaceCode())
                .setNamespacePublicId(namespace.getPublicId()).setAppCode(app.getAppCode())
                .setAppName(app.getAppName()).setAppPublicId(app.getPublicId());
    }
    private String required(String value) {
        if (value == null || value.isBlank()) throw new CommonException(DdcErrorStatus.INVALID_REQUEST);
        return value.trim();
    }
    private String optional(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
