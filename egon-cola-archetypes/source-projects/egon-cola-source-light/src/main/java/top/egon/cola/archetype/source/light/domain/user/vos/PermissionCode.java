package top.egon.cola.archetype.source.light.domain.user.vos;

public record PermissionCode(String value) {
    public PermissionCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("permission code must not be blank");
        }
    }
}
