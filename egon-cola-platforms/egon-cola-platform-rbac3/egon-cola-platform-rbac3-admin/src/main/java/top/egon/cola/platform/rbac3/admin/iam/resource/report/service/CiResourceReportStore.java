package top.egon.cola.platform.rbac3.admin.iam.resource.report.service;

import top.egon.cola.platform.rbac3.admin.iam.resource.report.domain.dto.CiResourceReportRequestDTO;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.domain.vo.CiResourceReportResultVO;

import java.util.Optional;

/** Persistence boundary for one global application CI report transaction. */
public interface CiResourceReportStore {

    Optional<ReportHead> findHead(String applicationCode);

    CiResourceReportResultVO replace(
            String applicationCode,
            CiResourceReportRequestDTO request,
            String checksum);

    record ReportHead(
            String buildId,
            String checksum,
            CiResourceReportResultVO result,
            long applicationVersion) {

        public ReportHead(
                String buildId,
                String checksum,
                CiResourceReportResultVO result) {
            this(buildId, checksum, result, 0L);
        }
    }
}
