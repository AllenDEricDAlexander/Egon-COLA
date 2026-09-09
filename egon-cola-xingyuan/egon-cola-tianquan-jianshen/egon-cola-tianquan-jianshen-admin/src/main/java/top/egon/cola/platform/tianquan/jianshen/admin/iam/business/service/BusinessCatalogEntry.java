package top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service;

/** DDC-owned Business catalog data projected into RBAC3. */
public record BusinessCatalogEntry(
        String ddcBusinessId,
        String bizCode,
        String bizName,
        boolean enabled) {
}
