package top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.domain.vo;

/** Diff summary returned after a global CI registration replacement. */
public record CiResourceRegistrationResultVO(
        int added,
        int updated,
        int stale,
        int unchanged,
        int pendingMapping,
        int apiBindingsAdded,
        int apiBindingsRemoved,
        String checksum,
        long applicationVersion) {
}
