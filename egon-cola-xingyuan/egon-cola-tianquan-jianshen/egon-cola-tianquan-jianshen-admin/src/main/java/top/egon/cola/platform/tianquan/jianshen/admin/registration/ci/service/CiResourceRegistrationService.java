package top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.service;

import top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.ApplicationCatalogEntry;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.DdcCatalogGateway;
import top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.domain.dto.CiResourceRegistrationRequestDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.domain.vo.CiResourceRegistrationResultVO;

import java.util.Objects;

/** Validates and atomically delegates a CI-only global resource replacement. */
public final class CiResourceRegistrationService {

    public static final String REGISTRATION_SCOPE = "tianquan-jianshen:resource-catalog:report";

    private final DdcCatalogGateway catalog;
    private final CiResourceRegistrationStore store;

    public CiResourceRegistrationService(
            DdcCatalogGateway catalog,
            CiResourceRegistrationStore store) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.store = Objects.requireNonNull(store, "store");
    }

    public CiResourceRegistrationResultVO register(
            String businessCode,
            String applicationCode,
            ServiceIdentityPrincipal caller,
            CiResourceRegistrationRequestDTO request) {
        if (caller == null || !caller.scopes().contains(REGISTRATION_SCOPE)
                || !caller.sourceBizCode().equals(businessCode)
                || !caller.sourceAppCode().equals(applicationCode)) {
            throw new SecurityException("resource registration source is not bound");
        }
        ApplicationCatalogEntry application = catalog.listApplications(businessCode, null)
                .stream()
                .filter(value -> value.appCode().equals(applicationCode))
                .findFirst()
                .orElseThrow(() -> new SecurityException("Tianshu application is not available"));
        if (!application.applicationEnabled() || !application.businessEnabled()) {
            throw new SecurityException("Tianshu application or business is disabled");
        }
        String checksum = CiResourceRegistrationCanonicalizer.checksum(request);
        if (!checksum.equals(request.checksum())) {
            throw new IllegalArgumentException("resource registration checksum mismatch");
        }
        CiResourceRegistrationStore.RegistrationHead current = store.findHead(applicationCode)
                .orElse(null);
        if (current != null) {
            if (current.buildId().equals(request.buildId())
                    && current.checksum().equals(checksum)) {
                return current.result();
            }
            if (current.buildId().equals(request.buildId())) {
                throw new CiResourceRegistrationConflictException(
                        "same build id has a different checksum");
            }
            if (request.expectedApplicationVersion() != current.applicationVersion()) {
                throw new CiResourceRegistrationConflictException("application registration version conflict");
            }
        }
        return store.replace(applicationCode, request, checksum);
    }

    public static final class CiResourceRegistrationConflictException
            extends RuntimeException {
        public CiResourceRegistrationConflictException(String message) {
            super(message);
        }
    }
}
