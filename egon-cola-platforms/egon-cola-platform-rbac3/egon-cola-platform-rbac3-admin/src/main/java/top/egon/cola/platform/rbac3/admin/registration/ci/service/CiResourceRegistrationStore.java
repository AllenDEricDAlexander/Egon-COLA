package top.egon.cola.platform.rbac3.admin.registration.ci.service;

import top.egon.cola.platform.rbac3.admin.registration.ci.domain.dto.CiResourceRegistrationRequestDTO;
import top.egon.cola.platform.rbac3.admin.registration.ci.domain.vo.CiResourceRegistrationResultVO;

import java.util.Optional;

/** Persistence boundary for one global application CI registration transaction. */
public interface CiResourceRegistrationStore {

    Optional<RegistrationHead> findHead(String applicationCode);

    CiResourceRegistrationResultVO replace(
            String applicationCode,
            CiResourceRegistrationRequestDTO request,
            String checksum);

    record RegistrationHead(
            String buildId,
            String checksum,
            CiResourceRegistrationResultVO result,
            long applicationVersion) {

        public RegistrationHead(
                String buildId,
                String checksum,
                CiResourceRegistrationResultVO result) {
            this(buildId, checksum, result, 0L);
        }
    }
}
