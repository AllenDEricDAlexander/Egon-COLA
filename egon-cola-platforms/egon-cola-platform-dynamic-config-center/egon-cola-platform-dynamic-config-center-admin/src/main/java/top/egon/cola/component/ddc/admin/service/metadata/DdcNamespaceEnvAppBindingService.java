package top.egon.cola.component.ddc.admin.service.metadata;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.model.dto.DdcNamespaceEnvAppBindingRequest;
import top.egon.cola.component.ddc.admin.model.entity.DdcAppEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcNamespaceEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcNamespaceEnvAppBindingEntity;
import top.egon.cola.component.ddc.admin.model.vo.DdcNamespaceEnvAppBindingVO;
import top.egon.cola.component.ddc.admin.repository.DdcAppRepository;
import top.egon.cola.component.ddc.admin.repository.DdcEnvRepository;
import top.egon.cola.component.ddc.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.ddc.admin.repository.DdcNamespaceRepository;
import top.egon.cola.component.ddc.error.DdcErrorStatus;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class DdcNamespaceEnvAppBindingService {

    private final DdcNamespaceEnvAppBindingRepository bindingRepository;

    private final DdcNamespaceRepository namespaceRepository;

    private final DdcAppRepository appRepository;

    private final DdcEnvRepository envRepository;

    public DdcNamespaceEnvAppBindingService(
            DdcNamespaceEnvAppBindingRepository bindingRepository,
            DdcNamespaceRepository namespaceRepository,
            DdcAppRepository appRepository,
            DdcEnvRepository envRepository) {
        this.bindingRepository = bindingRepository;
        this.namespaceRepository = namespaceRepository;
        this.appRepository = appRepository;
        this.envRepository = envRepository;
    }

    public List<DdcNamespaceEnvAppBindingVO> list(
            String bizCode,
            String namespaceCode,
            String env,
            String appCode) {
        return bindingRepository.findAll().stream()
                .map(this::toVO)
                .filter(value -> matches(bizCode, value.bizCode()))
                .filter(value -> matches(namespaceCode, value.namespaceCode()))
                .filter(value -> matches(env, value.env()))
                .filter(value -> matches(appCode, value.appCode()))
                .sorted(Comparator
                        .comparing(DdcNamespaceEnvAppBindingVO::bizCode)
                        .thenComparing(DdcNamespaceEnvAppBindingVO::namespaceCode)
                        .thenComparing(DdcNamespaceEnvAppBindingVO::env)
                        .thenComparing(DdcNamespaceEnvAppBindingVO::appCode))
                .toList();
    }

    @Transactional
    public DdcNamespaceEnvAppBindingVO create(
            DdcNamespaceEnvAppBindingRequest request) {
        DdcNamespaceEntity namespace = requireNamespace(request);
        DdcAppEntity app = requireApp(request);
        String env = requireEnv(request.env());
        if (bindingRepository.existsByNamespaceIdAndEnvCodeAndAppId(
                namespace.getId(), env, app.getId())) {
            throw new CommonException(DdcErrorStatus.NAMESPACE_BINDING_EXISTS);
        }
        LocalDateTime now = LocalDateTime.now();
        DdcNamespaceEnvAppBindingEntity binding =
                new DdcNamespaceEnvAppBindingEntity();
        binding.setId(UuidV7.simpleString());
        binding.setNamespaceId(namespace.getId());
        binding.setEnvCode(env);
        binding.setAppId(app.getId());
        binding.setEnabled(request.enabled() == null || request.enabled());
        binding.setCreatedAt(now);
        binding.setUpdatedAt(now);
        return toVO(bindingRepository.save(binding), namespace, app);
    }

    @Transactional
    public DdcNamespaceEnvAppBindingVO update(
            String id,
            DdcNamespaceEnvAppBindingRequest request) {
        DdcNamespaceEnvAppBindingEntity binding = require(id);
        DdcNamespaceEntity namespace = requireNamespace(request);
        DdcAppEntity app = requireApp(request);
        String env = requireEnv(request.env());
        if (bindingRepository.existsByNamespaceIdAndEnvCodeAndAppIdAndIdNot(
                namespace.getId(), env, app.getId(), id)) {
            throw new CommonException(DdcErrorStatus.NAMESPACE_BINDING_EXISTS);
        }
        binding.setNamespaceId(namespace.getId());
        binding.setEnvCode(env);
        binding.setAppId(app.getId());
        if (request.enabled() != null) {
            binding.setEnabled(request.enabled());
        }
        binding.setUpdatedAt(LocalDateTime.now());
        return toVO(bindingRepository.save(binding), namespace, app);
    }

    @Transactional
    public void delete(String id) {
        bindingRepository.delete(require(id));
    }

    public List<DdcAppEntity> visibleApps(
            String bizCode,
            String namespaceCode,
            String env) {
        DdcNamespaceEntity namespace = namespaceRepository
                .findByBizCodeAndNamespaceCode(bizCode, namespaceCode)
                .orElse(null);
        if (namespace == null) {
            return List.of();
        }
        List<String> appIds = bindingRepository
                .findByNamespaceIdAndEnvCodeAndEnabledTrue(
                        namespace.getId(), env)
                .stream()
                .map(DdcNamespaceEnvAppBindingEntity::getAppId)
                .distinct()
                .toList();
        return appRepository.findAllByIdIn(appIds).stream()
                .filter(app -> bizCode.equals(app.getBizCode()))
                .sorted(Comparator.comparing(DdcAppEntity::getAppCode))
                .toList();
    }

    public List<String> visibleEnvCodes(
            String bizCode,
            String namespaceCode) {
        DdcNamespaceEntity namespace = namespaceRepository
                .findByBizCodeAndNamespaceCode(bizCode, namespaceCode)
                .orElse(null);
        if (namespace == null) {
            return List.of();
        }
        return bindingRepository.findByNamespaceIdAndEnabledTrue(namespace.getId())
                .stream()
                .map(DdcNamespaceEnvAppBindingEntity::getEnvCode)
                .distinct()
                .sorted()
                .toList();
    }

    private DdcNamespaceEntity requireNamespace(
            DdcNamespaceEnvAppBindingRequest request) {
        String bizCode = required(request.bizCode());
        String namespaceCode = required(request.namespaceCode());
        return namespaceRepository.findByBizCodeAndNamespaceCode(
                        bizCode, namespaceCode)
                .orElseThrow(() -> new CommonException(
                        DdcErrorStatus.NAMESPACE_NOT_FOUND));
    }

    private DdcAppEntity requireApp(DdcNamespaceEnvAppBindingRequest request) {
        return appRepository.findByBizCodeAndAppCode(
                        required(request.bizCode()), required(request.appCode()))
                .orElseThrow(() -> new CommonException(DdcErrorStatus.APP_NOT_FOUND));
    }

    private String requireEnv(String env) {
        String value = required(env);
        if (!envRepository.existsByEnvCode(value)) {
            throw new CommonException(DdcErrorStatus.ENV_NOT_FOUND);
        }
        return value;
    }

    private DdcNamespaceEnvAppBindingEntity require(String id) {
        return bindingRepository.findById(id)
                .orElseThrow(() -> new CommonException(
                        DdcErrorStatus.NAMESPACE_BINDING_NOT_FOUND));
    }

    private DdcNamespaceEnvAppBindingVO toVO(
            DdcNamespaceEnvAppBindingEntity binding) {
        DdcNamespaceEntity namespace = namespaceRepository
                .findById(binding.getNamespaceId())
                .orElseThrow(() -> new CommonException(
                        DdcErrorStatus.NAMESPACE_NOT_FOUND));
        DdcAppEntity app = appRepository.findById(binding.getAppId())
                .orElseThrow(() -> new CommonException(DdcErrorStatus.APP_NOT_FOUND));
        return toVO(binding, namespace, app);
    }

    private DdcNamespaceEnvAppBindingVO toVO(
            DdcNamespaceEnvAppBindingEntity binding,
            DdcNamespaceEntity namespace,
            DdcAppEntity app) {
        return new DdcNamespaceEnvAppBindingVO(
                binding.getId(),
                namespace.getBizCode(),
                namespace.getId(),
                namespace.getNamespaceCode(),
                binding.getEnvCode(),
                app.getId(),
                app.getAppCode(),
                app.getAppName(),
                Boolean.TRUE.equals(binding.getEnabled())
        );
    }

    private boolean matches(String expected, String actual) {
        return expected == null
                || expected.isBlank()
                || expected.trim().equals(actual);
    }

    private String required(String value) {
        if (value == null || value.isBlank()) {
            throw new CommonException(DdcErrorStatus.INVALID_REQUEST);
        }
        return value.trim();
    }
}
