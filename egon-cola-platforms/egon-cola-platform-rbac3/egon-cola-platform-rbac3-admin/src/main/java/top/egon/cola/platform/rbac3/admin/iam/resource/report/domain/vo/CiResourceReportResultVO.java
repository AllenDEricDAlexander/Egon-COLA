package top.egon.cola.platform.rbac3.admin.iam.resource.report.domain.vo;

/** Diff summary returned after a global CI_REPORT replacement. */
public record CiResourceReportResultVO(
        int added,
        int updated,
        int stale,
        int unchanged,
        int pending,
        String checksum) {
}
