package top.egon.cola.component.tianshu.admin.service.metadata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.model.bo.DdcBizBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcAppRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcBizRepository;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service("ddcBizService")
@RequiredArgsConstructor
public class DdcBizService {

    @Qualifier("ddcScopeGate")
    private final DdcScopeGate scopeGate;
    @Qualifier("ddcBizRepository")
    private final DdcBizRepository bizRepository;
    @Qualifier("ddcAppRepository")
    private final DdcAppRepository appRepository;

    public List<DdcBizBO> list(String keyword) {
        return bizRepository.list(optional(keyword), null);
    }

    public List<DdcBizBO> list(String keyword, Boolean enabled) {
        return bizRepository.list(optional(keyword), enabled);
    }

    public DdcPageResult<DdcBizBO> page(String keyword, PageQuery pageQuery) {
        return bizRepository.page(optional(keyword), null, pageQuery);
    }

    public DdcBizBO findByBizCode(String bizCode) {
        return require(bizCode);
    }

    public Optional<DdcBizBO> findById(String publicId) {
        if (publicId == null || publicId.isBlank()) return Optional.empty();
        return bizRepository.findByPublicId(publicId);
    }

    @Transactional
    public DdcBizBO save(DdcBizBO biz) {
        LocalDateTime now = LocalDateTime.now();
        biz.setCreatedAt(now);
        biz.setUpdatedAt(now);
        if (biz.getEnabled() == null) biz.setEnabled(true);
        if (bizRepository.existsByBizCode(biz.getBizCode()))
            throw new CommonException(DdcErrorStatus.BIZ_CODE_EXISTS);
        return bizRepository.insert(biz);
    }

    @Transactional
    public DdcBizBO update(String bizCode, DdcBizBO request) {
        DdcBizBO existing = require(bizCode);
        if (bizRepository.existsByBizCodeAndIdNot(request.getBizCode(), existing.getId()))
            throw new CommonException(DdcErrorStatus.BIZ_CODE_EXISTS);
        existing.setBizCode(request.getBizCode());
        existing.setBizName(request.getBizName());
        existing.setDescription(request.getDescription());
        existing.setUpdatedAt(LocalDateTime.now());
        return bizRepository.update(existing);
    }

    @Transactional
    public void delete(String bizCode) {
        DdcBizBO existing = require(bizCode);
        if (appRepository.existsByBizCode(bizCode)) throw new CommonException(DdcErrorStatus.BIZ_IN_USE);
        bizRepository.delete(existing);
        scopeGate.invalidate("biz:" + bizCode);
    }

    @Transactional
    public DdcBizBO setEnabled(String bizCode, boolean enabled) {
        DdcBizBO existing = require(bizCode);
        existing.setEnabled(enabled);
        existing.setUpdatedAt(LocalDateTime.now());
        DdcBizBO saved = bizRepository.update(existing);
        scopeGate.invalidate("biz:" + bizCode);
        return saved;
    }

    private DdcBizBO require(String bizCode) {
        return bizRepository.findByBizCode(bizCode)
                .orElseThrow(() -> new CommonException(DdcErrorStatus.BIZ_NOT_FOUND));
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
