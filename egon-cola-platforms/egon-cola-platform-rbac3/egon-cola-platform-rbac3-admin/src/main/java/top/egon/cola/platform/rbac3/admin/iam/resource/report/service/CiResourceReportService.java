package top.egon.cola.platform.rbac3.admin.iam.resource.report.service;

import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.rbac3.admin.iam.business.service.ApplicationCatalogEntry;
import top.egon.cola.platform.rbac3.admin.iam.business.service.DdcCatalogGateway;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.domain.dto.CiResourceReportRequestDTO;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.domain.vo.CiResourceReportResultVO;

import java.util.Objects;

/** Validates and atomically delegates a CI-only global resource replacement. */
public final class CiResourceReportService {

    public static final String REPORT_SCOPE = "rbac3:resource-catalog:report";

    private final DdcCatalogGateway catalog;
    private final CiResourceReportStore store;

    public CiResourceReportService(
            DdcCatalogGateway catalog,
            CiResourceReportStore store) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.store = Objects.requireNonNull(store, "store");
    }

    public CiResourceReportResultVO report(
            String businessCode,
            String applicationCode,
            ServiceIdentityPrincipal caller,
            CiResourceReportRequestDTO request) {
        if (caller == null || !caller.scopes().contains(REPORT_SCOPE)
                || !caller.sourceBizCode().equals(businessCode)
                || !caller.sourceAppCode().equals(applicationCode)) {
            throw new SecurityException("resource report source is not bound");
        }
        ApplicationCatalogEntry application = catalog.listApplications(businessCode, null)
                .stream()
                .filter(value -> value.appCode().equals(applicationCode))
                .findFirst()
                .orElseThrow(() -> new SecurityException("DDC application is not available"));
        if (!application.applicationEnabled() || !application.businessEnabled()) {
            throw new SecurityException("DDC application or business is disabled");
        }
        String checksum = CiResourceReportCanonicalizer.checksum(request);
        if (!checksum.equals(request.checksum())) {
            throw new IllegalArgumentException("resource report checksum mismatch");
        }
        CiResourceReportStore.ReportHead current = store.findHead(applicationCode)
                .orElse(null);
        if (current != null) {
            if (current.buildId().equals(request.buildId())
                    && current.checksum().equals(checksum)) {
                return current.result();
            }
            if (current.buildId().equals(request.buildId())) {
                throw new CiResourceReportConflictException(
                        "same build id has a different checksum");
            }
            if (request.expectedApplicationVersion() != current.applicationVersion()) {
                throw new CiResourceReportConflictException("application report version conflict");
            }
        }
        return store.replace(applicationCode, request, checksum);
    }

    public static final class CiResourceReportConflictException
            extends RuntimeException {
        public CiResourceReportConflictException(String message) {
            super(message);
        }
    }
}
