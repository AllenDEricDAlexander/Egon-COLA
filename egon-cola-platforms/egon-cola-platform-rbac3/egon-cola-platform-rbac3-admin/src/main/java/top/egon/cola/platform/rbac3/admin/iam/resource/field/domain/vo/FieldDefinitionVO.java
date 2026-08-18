package top.egon.cola.platform.rbac3.admin.iam.resource.field.domain.vo;

/** Global response field definition exposed by the IAM catalog. */
public record FieldDefinitionVO(
        String id,
        String applicationId,
        String resourceId,
        String fieldCode,
        String jsonPath,
        String dataType,
        String sensitivity,
        String defaultAccess,
        String maskingStrategy,
        boolean writable,
        boolean exportable,
        String status,
        long version) {
}
