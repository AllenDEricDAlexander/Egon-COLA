package top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service;

/** Tianshu-owned Business catalog data projected into Tianquan-Jianshen. */
public record BusinessCatalogEntry(
        String ddcBusinessId,
        String bizCode,
        String bizName,
        boolean enabled) {
}
