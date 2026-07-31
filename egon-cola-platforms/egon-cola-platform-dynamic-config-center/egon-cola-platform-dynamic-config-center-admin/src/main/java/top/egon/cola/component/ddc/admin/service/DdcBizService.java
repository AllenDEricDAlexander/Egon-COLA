package top.egon.cola.component.ddc.admin.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.model.entity.DdcBizEntity;
import top.egon.cola.component.ddc.admin.repository.DdcAppRepository;
import top.egon.cola.component.ddc.admin.repository.DdcBizRepository;
import top.egon.cola.component.ddc.common.DdcErrorStatus;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DdcBizService {

    private final DdcScopeGate scopeGate;

    private final DdcBizRepository bizRepository;

    private final DdcAppRepository appRepository;

    public DdcBizService(DdcBizRepository bizRepository,
                         DdcAppRepository appRepository,
                         DdcScopeGate scopeGate) {
        this.bizRepository = bizRepository;
        this.appRepository = appRepository;
        this.scopeGate = scopeGate;
    }

    public List<DdcBizEntity> list(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return bizRepository.findAll(Sort.by(Sort.Direction.ASC, "bizCode"));
        }
        String trimmed = keyword.trim();
        return bizRepository.findByBizCodeContainingIgnoreCaseOrBizNameContainingIgnoreCase(
                trimmed, trimmed);
    }

    public DdcBizEntity findByBizCode(String bizCode) {
        return require(bizCode);
    }

    @Transactional
    public DdcBizEntity save(DdcBizEntity biz) {
        LocalDateTime now = LocalDateTime.now();
        if (biz.getId() == null) {
            biz.setId(UuidV7.simpleString());
            biz.setCreatedAt(now);
        }
        if (biz.getEnabled() == null) {
            biz.setEnabled(true);
        }
        if (bizRepository.existsByBizCode(biz.getBizCode())) {
            throw new CommonException(DdcErrorStatus.BIZ_CODE_EXISTS);
        }
        biz.setUpdatedAt(now);
        return bizRepository.save(biz);
    }

    @Transactional
    public DdcBizEntity update(String bizCode, DdcBizEntity request) {
        DdcBizEntity existing = require(bizCode);
        if (bizRepository.existsByBizCodeAndIdNot(request.getBizCode(), existing.getId())) {
            throw new CommonException(DdcErrorStatus.BIZ_CODE_EXISTS);
        }
        existing.setBizCode(request.getBizCode());
        existing.setBizName(request.getBizName());
        existing.setDescription(request.getDescription());
        existing.setUpdatedAt(LocalDateTime.now());
        return bizRepository.save(existing);
    }

    @Transactional
    public void delete(String bizCode) {
        DdcBizEntity existing = require(bizCode);
        if (appRepository.existsByBizCode(bizCode)) {
            throw new CommonException(DdcErrorStatus.BIZ_IN_USE);
        }
        bizRepository.delete(existing);
        scopeGate.invalidate("biz:" + bizCode);
    }

    @Transactional
    public DdcBizEntity setEnabled(String bizCode, boolean enabled) {
        DdcBizEntity existing = require(bizCode);
        existing.setEnabled(enabled);
        existing.setUpdatedAt(LocalDateTime.now());
        DdcBizEntity saved = bizRepository.save(existing);
        scopeGate.invalidate("biz:" + bizCode);
        return saved;
    }

    private DdcBizEntity require(String bizCode) {
        return bizRepository.findByBizCode(bizCode)
                .orElseThrow(() -> new CommonException(DdcErrorStatus.BIZ_NOT_FOUND));
    }
}
