package top.egon.cola.archetype.source.lightopen.domain.user.vos;

public record RoleCode(String value) {
    public RoleCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("role code must not be blank");
        }
    }
}
