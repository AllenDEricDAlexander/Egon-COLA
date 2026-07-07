package top.egon.cola.component.ddc.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.util.IdUtils;
import top.egon.cola.component.ddc.admin.model.entity.DdcAppEntity;
import top.egon.cola.component.ddc.admin.repository.DdcAppRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DdcAppService {

    private final DdcAppRepository appRepository;

    public DdcAppService(DdcAppRepository appRepository) {
        this.appRepository = appRepository;
    }

    @Transactional
    public DdcAppEntity save(DdcAppEntity app) {
        LocalDateTime now = LocalDateTime.now();
        if (app.getId() == null) {
            app.setId(IdUtils.simpleUuid());
            app.setCreatedAt(now);
        }
        if (app.getEnabled() == null) {
            app.setEnabled(true);
        }
        app.setUpdatedAt(now);
        return appRepository.save(app);
    }

    public Optional<DdcAppEntity> findByAppCode(String appCode) {
        return appRepository.findByAppCode(appCode);
    }

    public List<DdcAppEntity> list() {
        return appRepository.findAll();
    }
}
