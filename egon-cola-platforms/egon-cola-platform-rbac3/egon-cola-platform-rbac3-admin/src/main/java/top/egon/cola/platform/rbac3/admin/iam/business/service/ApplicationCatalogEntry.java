package top.egon.cola.platform.rbac3.admin.iam.business.service;

/** DDC-owned Application catalog data, including parent Business state. */
public record ApplicationCatalogEntry(
        String ddcApplicationId,
        String ddcBusinessId,
        String bizCode,
        String appCode,
        String appName,
        boolean applicationEnabled,
        boolean businessEnabled) {
}
